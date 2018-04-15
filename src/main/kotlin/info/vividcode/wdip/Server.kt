package info.vividcode.wdip

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.head
import io.ktor.routing.routing
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
    val processorsConfigJsonPath = System.getenv("PROCESSORS_CONFIG_PATH") ?: "./sampleProcessors/processors.json"

    val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
    val wdSessionManager = WebDriverSessionManager(okHttpClient, webDriverBaseUrls)

    val settings = parseProcessorsConfigJson(Paths.get(processorsConfigJsonPath))
    val imageProcessorPathAndInterceptorPairs = settings.map {
        it.path to createWdImageProcessingPipelineInterceptor(WdImageProcessingExecutor(it.html, it.js, wdSessionManager), it.key)
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
        routing {
            getAndHead("/-/health") {
                call.respond("OK")
            }

            imageProcessorPathAndInterceptorPairs.map { pathAndInterceptorPair ->
                getAndHead(pathAndInterceptorPair.first, pathAndInterceptorPair.second)
            }
        }
    }
    serverReference.set(server)

    Runtime.getRuntime().addShutdownHook(shutdownHookThread)
    server.start(wait = true)
}

private class ByteArrayContent(
    override val contentType: ContentType,
    private val bytes: ByteArray
) : OutgoingContent.ByteArrayContent() {
    override fun bytes(): ByteArray = bytes
}

private fun Route.getAndHead(path: String, body: PipelineInterceptor<Unit, ApplicationCall>) {
    get(path, body)
    head(path, body)
}

private fun createWdImageProcessingPipelineInterceptor(
    wdImageProcessingExecutor: WdImageProcessingExecutor,
    key: String?
): PipelineInterceptor<Unit, ApplicationCall> = lambda@{
    val argParameter = call.request.queryParameters["arg"]
    val signatureParameter = call.request.queryParameters["signature"]
    if (key != null) {
        val expectedSignature = Signatures.makeSignatureWithHmacSha1(key, argParameter ?: "")
        if (expectedSignature != signatureParameter) {
            call.respond(HttpStatusCode.BadRequest, "Bad Signature")
            return@lambda
        }
    }
    val arg = argParameter ?: "null"
    call.respond(ByteArrayContent(ContentType.Image.PNG, wdImageProcessingExecutor.execute(arg)))
}