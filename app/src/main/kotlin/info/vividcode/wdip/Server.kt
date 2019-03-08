package info.vividcode.wdip

import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import info.vividcode.wd.pool.WebDriverConnectionManager
import info.vividcode.wdip.application.WdImageProcessingExecutor
import info.vividcode.wdip.ktor.SignatureVerifyingInterceptor
import info.vividcode.wdip.ktor.WdImageProcessingInterceptor
import info.vividcode.wdip.ktor.features.OriginAccessControl
import info.vividcode.wdip.ktor.getAndHead
import info.vividcode.wdip.ktor.routeGetAndHead
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.util.pipeline.PipelineInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun createWebDriverSessionManager(webDriverBaseUrls: List<String>, webDriverSessionCapacity: Int): WebDriverConnectionManager {
    val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            // Avoid retrying on 408 error.
            .retryOnConnectionFailure(false)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    return WebDriverConnectionManager(
            OkHttpWebDriverCommandHttpRequestDispatcher.Factory(okHttpClient), webDriverBaseUrls,
            webDriverSessionCapacity = webDriverSessionCapacity,
            webDriverTimeouts = Timeouts(18_000, 18_000, 0))
}

fun startServer(processorsConfigJsonPath: String, wdSessionManager: WebDriverConnectionManager) {
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
        setup(config, wdSessionManager, wdImageProcessingEndpoints)
    }

    serverReference.set(server)

    Runtime.getRuntime().addShutdownHook(shutdownHookThread)
    server.start(wait = true)
}

internal fun Application.setup(
        config: WdipSetting,
        wdSessionManager: WebDriverConnectionManager,
        wdImageProcessingEndpoints: List<WdImageProcessingEndpoint>
) {
    intercept(ApplicationCallPipeline.Call) {
        try {
            proceed()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    intercept(ApplicationCallPipeline.Call) {
        call.response.header("X-Content-Type-Options", "nosniff")
    }

    install(OriginAccessControl) {
        accessControlAllowOrigins.addAll(config.accessControlAllowOrigins)
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

        val gitRevision = Resources.readAsUtf8Text("/wdip-git-revision")
        getAndHead("/-/system-info") {
            if (gitRevision != null) {
                call.response.header("X-Rev", gitRevision)
            }
            call.respond("")
        }

        wdImageProcessingEndpoints.forEach { endpoint ->
            routeGetAndHead(endpoint.path) {
                endpoint.interceptors.forEach(::handle)
            }
        }
    }
}

internal object Resources {
    internal fun readAsUtf8Text(resourcePath: String): String? =
            Resources::class.java.getResourceAsStream(resourcePath)?.use { inputStream ->
                String(inputStream.readBytes(), Charsets.UTF_8)
            }
}

internal class WdImageProcessingEndpoint(
    val path: String,
    val interceptors: List<PipelineInterceptor<Unit, ApplicationCall>>
)
