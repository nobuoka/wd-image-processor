package info.vividcode.wd.http

import com.beust.klaxon.JsonObject
import info.vividcode.wd.*
import info.vividcode.wd.WebDriverCommandExecutor
import java.util.*

class WebDriverCommandExecutor(
        private val dispatcher: WebDriverCommandHttpRequestDispatcher
) : WebDriverCommandExecutor {

    class OkHttpWebDriverSession(override val id: String) : WebDriverSession

    // TODO : Escaping
    private val WebDriverCommand.SessionCommand.sessionPathSegment get() = session.id

    override fun WebDriverCommand.NewSession.execute(): WebDriverSession =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session", JsonObject(
                    mapOf("desiredCapabilities" to JsonObject(mapOf()))
                ).toJsonString()
            )
        ) {
            val sessionId =
                it.obj("value")?.string("sessionId") ?:
                it.string("sessionId") ?:
                throw RuntimeException("Unexpected response content (sessionId not found): $it")
            OkHttpWebDriverSession(sessionId)
        }

    override fun WebDriverCommand.DeleteSession.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("DELETE", "/session/$sessionPathSegment", null)
        ) {}

    override fun WebDriverCommand.SetWindowRect.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session/$sessionPathSegment/window/rect", JsonObject(
                    mapOf(
                        "height" to rect.height + 105,
                        "width" to rect.width
                    )
                ).toJsonString()
            )
        ) {}

    override fun WebDriverCommand.SetTimeouts.execute() =
            dispatcher.dispatch(
                    WebDriverCommandHttpRequest(
                            "POST", "/session/$sessionPathSegment/timeouts",
                            JsonObject(mapOf(
                                    "script" to timeouts.scriptTimeoutInMs,
                                    "pageLoad" to timeouts.pageLoadTimeoutInMs,
                                    "implicit" to timeouts.implicitTimeoutInMs
                            )).toJsonString()
                    )
            ) {}

    override fun WebDriverCommand.Go.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST",
                "/session/$sessionPathSegment/url",
                JsonObject(mapOf("url" to url)).toJsonString()
            )
        ) {}

    override fun WebDriverCommand.ExecuteAsyncScript.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session/$sessionPathSegment/execute/async",
                    ExecuteAsyncScriptCommandContentConverter.createRequestJson(script).toJsonString()
            )
        ) {
            return@dispatch ExecuteAsyncScriptCommandContentConverter.parseResponseJson(it).let { jsResult ->
                when (jsResult) {
                    is ExecuteAsyncScriptCommandContentConverter.JavaScriptResult.Success -> jsResult.value
                    is ExecuteAsyncScriptCommandContentConverter.JavaScriptResult.Error ->
                        throw RuntimeException("Script error on Execute Async Script command (error : ${jsResult.message})")
                }
            }
        }

    override fun WebDriverCommand.TakeElementScreenshot.execute(): ByteArray =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "GET",
                "/session/$sessionPathSegment/element/${this.targetElement.reference}/screenshot",
                null
            )
        ) {
            val screenshotBase64 = it.string("value")
            Base64.getDecoder().decode(screenshotBase64)
        }

    override fun WebDriverCommand.TakeScreenshot.execute(): ByteArray =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("GET", "/session/$sessionPathSegment/screenshot", null)
        ) {
            val screenshotBase64 = it.string("value")
            Base64.getDecoder().decode(screenshotBase64)
        }

    override fun WebDriverCommand.FindElement.execute(): WebElement =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session/$sessionPathSegment/element", JsonObject(
                    mapOf(
                        "using" to when (selector.strategy) {
                        //ElementSelector.Strategy.CSS -> "css" // "css selector"
                            ElementSelector.Strategy.XPATH -> "xpath"
                        },
                        "value" to selector.value
                    )
                ).toJsonString()
            )
        ) {
            (it.obj("value") ?: throw RuntimeException("$it")).let(WebElement.Companion::from)
        }

}
