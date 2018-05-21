package info.vividcode.wdip

import com.beust.klaxon.JsonObject
import info.vividcode.wd.*
import info.vividcode.wdip.ktor.ByteArrayContent
import io.ktor.content.OutgoingContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import javax.imageio.ImageIO

class WdImageProcessingResult(val statusCode: Int, val content: OutgoingContent?, val httpCache: HttpCache?)

fun WebDriverCommandExecutor.executeImageProcessorWithElementScreenshot(
    session: WebDriverSession, htmlString: String, jsString: String, jsArg: String
): WdImageProcessingResult {
    WebDriverCommand.Go(session, createHtmlDataUrl(htmlString)).execute()
    val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session,
        Script(jsString, listOf(jsArg))
    ).execute()

    val executeResult = parseScriptResponse((rawExecuteResult as? ScriptResult.Object)?.value)

    val content: OutgoingContent? = when (executeResult.content) {
        is Content.Screenshot -> {
            val element =
                executeResult.content.targetElement ?:
                WebDriverCommand.FindElement(session, ElementSelector(ElementSelector.Strategy.XPATH, "//body")).execute()
            val screenshot = WebDriverCommand.TakeElementScreenshot(session, element).execute()
            ByteArrayContent(ContentType.Image.PNG, screenshot)
        }
        is Content.Text -> {
            executeResult.content.value?.let { TextContent(it, ContentType.Text.Plain.withCharset(Charsets.UTF_8)) }
        }
    }

    return WdImageProcessingResult(executeResult.statusCode, content, executeResult.httpCache)
}

fun WebDriverCommandExecutor.executeImageProcessorWithCroppedScreenshot(
    session: WebDriverSession, htmlString: String, jsString: String, jsArg: String
): ByteArray {
    WebDriverCommand.Go(session, createHtmlDataUrl(htmlString)).execute()
    val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session,
        Script(jsString, listOf(jsArg))
    ).execute()
    println("Execute result: $rawExecuteResult")
//            WebDriverCommand.SetWindowRect(session, Rect(10, 10)).execute()

    val executeResult = parseScriptResponse((rawExecuteResult as? ScriptResult.Object)?.value)

    // TODO : 要素のサイズ取得やスクロール処理をする。
    val screenshotRect = parseScreenshotRect(
        (executeResult as? ScriptResult.Object)?.value?.get("screenshotRect") as? JsonObject,
        screenshotRectDefaultValue
    )
    val screenshotImage = WebDriverCommand.TakeScreenshot(session).execute()
    return cropImage(ByteArrayInputStream(screenshotImage), screenshotRect, ByteArrayOutputStream()).toByteArray()
}

fun parseScriptResponse(obj: JsonObject?) =
    ImageProcessorScriptResponse(
        content = run {
            (obj?.get("content") as? JsonObject)?.let { c ->
                when (c.get("type") as? String) {
                    "text" -> Content.Text(c["value"] as? String)
                    else -> Content.Screenshot((c["targetElement"] as? JsonObject)?.let(WebElement.Companion::from))
                }
            } ?: Content.Screenshot((obj?.get("targetElement") as? JsonObject)?.let(WebElement.Companion::from))
        },
        statusCode = (obj?.get("statusCode") as? Int) ?: 200,
        httpCache = (obj?.get("httpCache") as? JsonObject)?.let {
            HttpCache(it["maxAge"] as? Int)
        }
    )

private fun <T : OutputStream> cropImage(imageInputStream: InputStream, screenshotRect: ScreenshotRect, imageOutputStream: T): T =
    imageOutputStream.also {
        val originalImage = ImageIO.read(imageInputStream)
        val croppedImage = originalImage.getSubimage(screenshotRect.x, screenshotRect.y, screenshotRect.width, screenshotRect.height)
        val ok = ImageIO.write(croppedImage, "png", it)
        if (!ok) throw RuntimeException("No appropriate writer is found for ImageIO")
    }

fun createHtmlDataUrl(html: String) =
    "data:text/html;charset=utf-8;base64,${Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))}"

private val screenshotRectDefaultValue = ScreenshotRect(0, 0, 360, 360)

private fun parseScreenshotRect(obj: JsonObject?, defaultValue: ScreenshotRect) =
    ScreenshotRect(
        x = (obj?.get("x") as? Int ?: defaultValue.x),
        y = (obj?.get("y") as? Int ?: defaultValue.y),
        width = (obj?.get("width") as? Int ?: defaultValue.width),
        height = (obj?.get("height") as? Int ?: defaultValue.height)
    )
