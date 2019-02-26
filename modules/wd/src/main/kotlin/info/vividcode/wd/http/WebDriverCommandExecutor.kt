package info.vividcode.wd.http

import info.vividcode.wd.*
import info.vividcode.wd.WebDriverCommandExecutor
import java.util.*
import javax.json.Json
import javax.json.JsonString
import javax.json.JsonValue

class WebDriverCommandExecutor(
        private val dispatcher: WebDriverCommandHttpRequestDispatcher
) : WebDriverCommandExecutor {

    class OkHttpWebDriverSession(override val id: String) : WebDriverSession

    // TODO : Escaping
    private val WebDriverCommand.SessionCommand.sessionPathSegment get() = session.id

    override fun WebDriverCommand.NewSession.execute(): WebDriverSession =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session", Json.createObjectBuilder(
                    mapOf("desiredCapabilities" to mapOf<String, Any?>())
                ).build().toString()
            )
        ) { jsonObject ->
            val sessionIdJsonString =
                jsonObject["value"]?.asJsonObjectOrNull()?.let { it["sessionId"] as? JsonString } ?:
                jsonObject["sessionId"] as? JsonString ?:
                throw RuntimeException("Unexpected response content (sessionId not found): $jsonObject")
            OkHttpWebDriverSession(sessionIdJsonString.string)
        }

    private fun JsonValue.asJsonObjectOrNull() = if (this.valueType == JsonValue.ValueType.OBJECT) this.asJsonObject() else null

    override fun WebDriverCommand.DeleteSession.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("DELETE", "/session/$sessionPathSegment", null)
        ) {}

    override fun WebDriverCommand.SetWindowRect.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session/$sessionPathSegment/window/rect", Json.createObjectBuilder(
                    mapOf(
                        "height" to rect.height + 105,
                        "width" to rect.width
                    )
                ).build().toString()
            )
        ) {}

    override fun WebDriverCommand.SetTimeouts.execute() =
            dispatcher.dispatch(
                    WebDriverCommandHttpRequest(
                            "POST", "/session/$sessionPathSegment/timeouts",
                            Json.createObjectBuilder(mapOf(
                                    "script" to timeouts.scriptTimeoutInMs,
                                    "pageLoad" to timeouts.pageLoadTimeoutInMs,
                                    "implicit" to timeouts.implicitTimeoutInMs
                            )).build().toString()
                    )
            ) {}

    override fun WebDriverCommand.Go.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST",
                "/session/$sessionPathSegment/url",
                Json.createObjectBuilder(mapOf("url" to url)).build().toString()
            )
        ) {}

    override fun WebDriverCommand.ExecuteAsyncScript.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session/$sessionPathSegment/execute/async",
                    ExecuteAsyncScriptCommandContentConverter.createRequestJson(script).toString()
            )
        ) { jsonObject ->
            return@dispatch ExecuteAsyncScriptCommandContentConverter.parseResponseJson(jsonObject).let { jsResult ->
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
            val screenshotBase64 = it.getString("value")
            Base64.getDecoder().decode(screenshotBase64)
        }

    override fun WebDriverCommand.TakeScreenshot.execute(): ByteArray =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("GET", "/session/$sessionPathSegment/screenshot", null)
        ) {
            val screenshotBase64 = it.getString("value")
            Base64.getDecoder().decode(screenshotBase64)
        }

    override fun WebDriverCommand.FindElement.execute(): WebElement =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest(
                "POST", "/session/$sessionPathSegment/element", Json.createObjectBuilder(
                    mapOf(
                        "using" to when (selector.strategy) {
                        //ElementSelector.Strategy.CSS -> "css" // "css selector"
                            ElementSelector.Strategy.XPATH -> "xpath"
                        },
                        "value" to selector.value
                    )
                ).build().toString()
            )
        ) {
            (it.getJsonObject("value") ?: throw RuntimeException("$it")).let(WebElement.Companion::from)
        }

}
