package info.vividcode.wdip.application

import info.vividcode.wd.*
import javax.json.JsonObject

fun WebDriverCommandExecutor.executeImageProcessorWithElementScreenshot(
        session: WebDriverSession, htmlString: String, jsString: String, jsArg: String
): WdImageProcessingResult {
    WebDriverCommand.Go(session, createHtmlDataUrl(htmlString)).execute()
    val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session,
            Script(jsString, listOf(jsArg))
    ).execute()

    val executeResult = ImageProcessorScriptResponse.parseScriptResponse((rawExecuteResult as? JsonObject))

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
