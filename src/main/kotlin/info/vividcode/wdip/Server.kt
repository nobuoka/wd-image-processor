package info.vividcode.wdip

import info.vividcode.wdip.ktor.SignatureVerifyingInterceptor
import info.vividcode.wdip.ktor.WdImageProcessingInterceptor
import info.vividcode.wdip.ktor.getAndHead
import info.vividcode.wdip.ktor.routeGetAndHead
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun startServer() {
    val spaceSeparatedWebDriverBaseUrls = System.getenv("WD_BASE_URLS") ?: "http://localhost:10001"
    val webDriverBaseUrls = spaceSeparatedWebDriverBaseUrls.split(Regex("\\s"))
    val processorsConfigJsonPath = System.getenv("PROCESSORS_CONFIG_PATH") ?: "./sampleProcessors/config.json"

    val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    val wdSessionManager = WebDriverConnectionManager(okHttpClient, webDriverBaseUrls)

    val config = parseProcessorsConfigJson(Paths.get(processorsConfigJsonPath))
    val wdImageProcessingEndpoints = config.processors.map {
        WdImageProcessingEndpoint(it.path, listOfNotNull(
            it.key?.let(::SignatureVerifyingInterceptor),
            WdImageProcessingInterceptor(
                WdImageProcessingExecutor(wdSessionManager),
                it.html,
                it.js
            )
        ).map { it.toPipelineInterceptor() })
    }

    val serverReference = AtomicReference<NettyApplicationEngine?>(null)
    val shutdownHookThread = Thread(Runnable {
        serverReference.get()?.stop(2000, 15000, TimeUnit.MILLISECONDS)
    })

    val server = embeddedServer(Netty, 8080) {
        intercept(ApplicationCallPipeline.Call) {
            try {
                proceed()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        intercept(ApplicationCallPipeline.Call) {
            config.accessControlAllowOrigins.let {
                if (it.isNotEmpty()) {
                    call.response.header("Access-Control-Allow-Origin", it.joinToString(", "))
                }
            }
        }

        routing {
            getAndHead("/-/health/app-only") {
                call.respond("OK")
            }
            getAndHead("/-/health/all") {
                val result = wdSessionManager.checkAllWebDriverRemoteEndsAvailable()
                val healthCount = result.count { it }
                val countString = "(WebDriver remote ends: $healthCount / ${result.size})"
                if (healthCount == result.size) {
                    call.respond("OK $countString")
                } else {
                    call.respond(HttpStatusCode.ServiceUnavailable, "NG $countString")
                }
            }
            getAndHead("/-/health/wd") {
                val argUrl = call.request.queryParameters["url"]
                val result = argUrl?.let { wdSessionManager.checkWebDriverRemoteEndAvailable(it) }
                val message = "(WebDriver remote end: $argUrl)"
                if (result == true) {
                    call.respond("OK $message")
                } else {
                    call.respond(HttpStatusCode.ServiceUnavailable, "NG $message")
                }
            }

            wdImageProcessingEndpoints.forEach { endpoint ->
                routeGetAndHead(endpoint.path) {
                    endpoint.interceptors.forEach(::handle)
                }
            }
        }
    }
    serverReference.set(server)

    Runtime.getRuntime().addShutdownHook(shutdownHookThread)
    server.start(wait = true)
}

private class WdImageProcessingEndpoint(
    val path: String,
    val interceptors: List<PipelineInterceptor<Unit, ApplicationCall>>
)
