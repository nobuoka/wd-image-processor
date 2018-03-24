package info.vividcode.wdip

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import info.vividcode.wd.*
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandExecutor
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.charset.StandardCharsets
import java.util.*

import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.respond
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import java.io.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.coroutines.experimental.CoroutineContext

class ByteArrayContent(override val contentType: ContentType, private val bytes: ByteArray) : OutgoingContent.ByteArrayContent() {
    override fun bytes(): ByteArray = bytes
}

fun verifySignature(path: String, queryParameters: Parameters, key: String): Boolean {
    val signatureInfo = getUrlSignatureInfoFromUrl(key, queryParameters)
    return when (signatureInfo) {
        is UrlSignatureInfoResponse.UrlSignatureInfo ->
            makeSignature(signatureInfo.signatureBase, key) == signatureInfo.signature
        UrlSignatureInfoResponse.InvalidUrl -> false
    }
}

fun main(args: Array<String>) {
    val spaceSeparatedWebDriverBaseUrls = System.getenv("WD_BASE_URLS") ?: "http://localhost:10001"
    val webDriverBaseUrls = spaceSeparatedWebDriverBaseUrls.split(Regex("\\s"))
    val processorsConfigJsonPath = System.getenv("PROCESSORS_CONFIG_PATH") ?: "./sampleProcessors/processors.json"

    val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
    val wdSessionManager = WebDriverSessionManager(okHttpClient, webDriverBaseUrls)

    val settings = parseProcessorsConfigJson(Paths.get(processorsConfigJsonPath))
    val functionMap = settings.map {
        it.path to createWdImageProcessingPipelineInterceptor(WdImageProcessingExecutor(it.html, it.js, wdSessionManager), it.key)
    }.toMap()

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
            functionMap.map { function ->
                get(function.key, function.value)
            }
        }
    }
    server.start(wait = true)
}

class WebDriverSessionManager(okHttpClient: OkHttpClient, webDriverBaseUrls: List<String>) {
    private val wdCommandExecutorQueue: BlockingQueue<Pair<WebDriverCommandExecutor, WebDriverSession?>> =
        ArrayBlockingQueue(webDriverBaseUrls.size)

    private val context: CoroutineContext = newFixedThreadPoolContext(webDriverBaseUrls.size, "WebDriverContext")

    init {
        wdCommandExecutorQueue.addAll(
            webDriverBaseUrls
                .map { OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, it) }
                .map { Pair(OkHttpWebDriverCommandExecutor(it), null) }
        )
    }

    suspend fun <T> executeInSession(proc: WebDriverCommandExecutor.(session: WebDriverSession) -> T): T =
            async(context) {
                val (wd, continuedSession) = wdCommandExecutorQueue.take()
                var sessionToNext: WebDriverSession? = null
                try {
                    return@async with(wd) {
                        val session = if (continuedSession != null) continuedSession else WebDriverCommand.NewSession().execute()
                        val result: T
                        try {
                            result = proc(session)
                            sessionToNext = session
                        } catch (e: Exception) {
                            WebDriverCommand.DeleteSession(session).execute()
                            throw e
                        }
                        return@with result
                    }
                } finally {
                    wdCommandExecutorQueue.put(Pair(wd, sessionToNext))
                }
            }.await()
}

data class ProcessorSetting(val path: String, val html: String, val js: String, val key: String?)

private val screenshotRectDefaultValue = ScreenshotRect(0, 0, 360, 360)

fun parseProcessorsConfigJson(jsonFile: Path): List<ProcessorSetting> {
    fun Path.readContent(): String = Files.readAllBytes(jsonFile.parent.resolve(this)).toString(StandardCharsets.UTF_8)

    val configObject = jsonFile.toFile().reader().use { Klaxon().parseJsonObject(it) }
    return configObject.entries.map { (path, config) ->
        val htmlString = (config as? JsonObject)?.string("html")?.let { Paths.get(it).readContent() }
        val jsString = (config as? JsonObject)?.string("js")?.let { Paths.get(it).readContent() }
        val key = (config as? JsonObject)?.string("key")
        ProcessorSetting(path, htmlString ?: "", jsString ?: "", key)
    }
}

fun createWdImageProcessingPipelineInterceptor(
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

class WdImageProcessingExecutor(private val htmlString: String, private val jsString: String, private val webDriverSessionManager: WebDriverSessionManager) {
    suspend fun execute(jsArg: String): ByteArray {
        return webDriverSessionManager.executeInSession { session ->
            WebDriverCommand.Go(session, createHtmlDataUrl(htmlString)).execute()
            val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session,
                Script(jsString, listOf(jsArg))
            ).execute()
            println("Execute result: $rawExecuteResult")
//            WebDriverCommand.SetWindowRect(session, Rect(10, 10)).execute()

            val executeResult = parseScriptResponse((rawExecuteResult as? ScriptResult.Object)?.value)

            val element =
                    executeResult.targetElement ?:
                    WebDriverCommand.FindElement(
                            session,
                        ElementSelector(ElementSelector.Strategy.XPATH, "//body")
                    ).execute()
            WebDriverCommand.TakeElementScreenshot(session, element).execute()

//            val screenshotRect = parseScreenshotRect(
//                (executeResult as? ScriptResult.Object)?.value?.get("screenshotRect") as? JsonObject,
//                screenshotRectDefaultValue
//            )
//            WebDriverCommand.TakeScreenshot(session).execute()
//            cropImage(ByteArrayInputStream(screenshotImage), screenshotRect, ByteArrayOutputStream()).toByteArray()
        }
    }
}

data class ScreenshotRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class ImageProcessorScriptResponse(
    val targetElement: WebElement?
)

fun parseScriptResponse(obj: JsonObject?) =
        ImageProcessorScriptResponse(
            targetElement = (obj?.get("targetElement") as? JsonObject)?.let(WebElement.Companion::from)
        )

fun parseScreenshotRect(obj: JsonObject?, defaultValue: ScreenshotRect) =
    ScreenshotRect(
        x = (obj?.get("x") as? Int ?: defaultValue.x),
        y = (obj?.get("y") as? Int ?: defaultValue.y),
        width = (obj?.get("width") as? Int ?: defaultValue.width),
        height = (obj?.get("height") as? Int ?: defaultValue.height)
    )

fun <T : OutputStream> cropImage(imageInputStream: InputStream, screenshotRect: ScreenshotRect, imageOutputStream: T): T =
    imageOutputStream.also {
        val originalImage = ImageIO.read(imageInputStream)
        val croppedImage = originalImage.getSubimage(screenshotRect.x, screenshotRect.y, screenshotRect.width, screenshotRect.height)
        val ok = ImageIO.write(croppedImage, "png", it)
        if (!ok) throw RuntimeException("No appropriate writer is found for ImageIO")
    }

fun createHtmlDataUrl(html: String) =
        "data:text/html;charset=utf-8;base64,${Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))}"
