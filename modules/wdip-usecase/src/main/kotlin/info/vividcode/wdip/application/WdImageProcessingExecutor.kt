package info.vividcode.wdip.application

class WdImageProcessingExecutor(
    private val webDriverConnectionManager: WebDriverConnectionManager
) {
    suspend fun execute(htmlString: String, jsString: String, jsArg: String): WdImageProcessingResult =
        webDriverConnectionManager.withSession { session ->
            executeImageProcessorWithElementScreenshot(session, htmlString, jsString, jsArg)
        }
}
