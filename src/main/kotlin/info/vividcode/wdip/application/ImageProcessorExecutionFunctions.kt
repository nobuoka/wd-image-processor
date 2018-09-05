package info.vividcode.wdip.application

import com.beust.klaxon.JsonObject
import info.vividcode.wd.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun WebDriverCommandExecutor.executeImageProcessorWithElementScreenshot(
        session: WebDriverSession, htmlString: String, jsString: String, jsArg: String
): WdImageProcessingResult {
    WebDriverCommand.Go(session, createHtmlDataUrl(htmlString)).execute()
    val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session,
            Script(jsString, listOf(jsArg))
    ).execute()

    val executeResult = ImageProcessorScriptResponse.parseScriptResponse((rawExecuteResult as? ScriptResult.Object)?.value)

    val content: WdImageProcessingResultContent? = when (executeResult.content) {
        is Content.Screenshot -> {
            val element =
                executeResult.content.targetElement ?:
                WebDriverCommand.FindElement(session, ElementSelector(ElementSelector.Strategy.XPATH, "//body")).execute()
            val screenshot = WebDriverCommand.TakeElementScreenshot(session, element).execute()
            when (executeResult.content.imageType) {
                Content.Screenshot.ImageType.JPEG ->
                    WdImageProcessingResultContent.createJpeg(convertImageToJpeg(screenshot))
                Content.Screenshot.ImageType.PNG ->
                    WdImageProcessingResultContent.createPng(screenshot)
            }
        }
        is Content.Text -> {
            executeResult.content.value?.let { WdImageProcessingResultContent.createText(it) }
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

    val executeResult = ImageProcessorScriptResponse.parseScriptResponse((rawExecuteResult as? ScriptResult.Object)?.value)

    // TODO : 要素のサイズ取得やスクロール処理をする。
    val screenshotRect = parseScreenshotRect(
            (executeResult as? ScriptResult.Object)?.value?.get("screenshotRect") as? JsonObject,
            screenshotRectDefaultValue
    )
    val screenshotImage = WebDriverCommand.TakeScreenshot(session).execute()
    return cropImage(ByteArrayInputStream(screenshotImage), screenshotRect, ByteArrayOutputStream()).toByteArray()
}

private val screenshotRectDefaultValue = ScreenshotRect(0, 0, 360, 360)

private fun parseScreenshotRect(obj: JsonObject?, defaultValue: ScreenshotRect) =
        ScreenshotRect(
                x = (obj?.get("x") as? Int ?: defaultValue.x),
                y = (obj?.get("y") as? Int ?: defaultValue.y),
                width = (obj?.get("width") as? Int ?: defaultValue.width),
                height = (obj?.get("height") as? Int ?: defaultValue.height)
        )
