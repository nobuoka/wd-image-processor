package info.vividcode.wd.pool.internal

import info.vividcode.wd.*

internal object WebDriverHealthChecker {

    fun checkAvailability(webDriverCommandExecutor: WebDriverCommandExecutor, session: WebDriverSession): Boolean =
        with (webDriverCommandExecutor) {
            WebDriverCommand.Go(session, htmlDataUrl).execute()
            val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session, Script(js, listOf())).execute()
            (rawExecuteResult as? ScriptResult.String)?.value == "Health check"
        }

    private val htmlDataUrl = createHtmlDataUrl(
            "<!DOCTYP html><html><head><title>Health check</title></head><body></body></html>")
    private val js = """
        return document.title;
    """.trimIndent()

}
