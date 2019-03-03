package info.vividcode.wd.pool.internal

import info.vividcode.wd.*
import javax.json.JsonString

internal object WebDriverHealthChecker {

    fun checkAvailability(webDriverCommandExecutor: WebDriverCommandExecutor, session: WebDriverSession): Boolean =
        with (webDriverCommandExecutor) {
            WebDriverCommand.Go(session, htmlDataUrl).execute()
            val rawExecuteResult = WebDriverCommand.ExecuteAsyncScript(session, Script(js, listOf())).execute()
            (rawExecuteResult as? JsonString)?.string == "Health check"
        }

    private val htmlDataUrl = createHtmlDataUrl(
            "<!DOCTYP html><html><head><title>Health check</title></head><body></body></html>")
    private val js = """
        return document.title;
    """.trimIndent()

}
