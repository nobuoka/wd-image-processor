package info.vividcode.wd.http

import info.vividcode.test.utils.MockWebServerResourceExtension
import info.vividcode.wd.*
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class OkHttpWebDriverCommandExecutorTest {

    @JvmField
    @RegisterExtension
    internal val mockWebServerResourceExtension = MockWebServerResourceExtension()

    private val testSessionId = "f9e62afa-a24b-4529-97c0-f91a566f2401"

    /**
     * See : [WebDriver - 8.1 New Session](https://www.w3.org/TR/webdriver/#new-session)
     */
    @Test
    internal fun newSessionCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString(
                        """{ "sessionId": "$testSessionId", "capabilities": [] }"""
                )
        )

        val webDriverSession = with (testTarget) {
            WebDriverCommand.NewSession().execute()
        }

        Assertions.assertEquals(webDriverSession.id, testSessionId)
        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("POST", request.method)
        Assertions.assertEquals("/session", request.path)
        Assertions.assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
        Assertions.assertEquals("""{"desiredCapabilities":{}}""", request.body.readString(Charsets.UTF_8))
    }

    /**
     * See : [WebDriver - 8.2 Delete Session](https://www.w3.org/TR/webdriver/#delete-session)
     */
    @Test
    internal fun deleteSessionCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("null")
        )

        with (testTarget) {
            WebDriverCommand.DeleteSession(createWebDriverSession(testSessionId)).execute()
        }

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("DELETE", request.method)
        Assertions.assertEquals("/session/$testSessionId", request.path)
        Assertions.assertEquals(0, request.bodySize)
    }

    /**
     * See : [WebDriver - 10.7.2 Set Window Rect](https://www.w3.org/TR/webdriver/#dfn-set-window-rect)
     */
    @Test
    internal fun setWindowRectCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("null")
        )

        val testSession = createWebDriverSession(testSessionId)
        with (testTarget) {
            WebDriverCommand.SetWindowRect(testSession, Rect(width = 16, height = 18)).execute()
        }

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("POST", request.method)
        Assertions.assertEquals("/session/$testSessionId/window/rect", request.path)
        Assertions.assertEquals("""{"height":123,"width":16}""", request.body.readString(Charsets.UTF_8))
    }

    /**
     * See : [WebDriver - 8.5 Set Timeouts](https://www.w3.org/TR/webdriver/#dfn-timeouts)
     */
    @Test
    internal fun setTimeoutsCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("null")
        )

        val testSession = createWebDriverSession(testSessionId)
        with (testTarget) {
            val timeouts = Timeouts(scriptTimeoutInMs = 15_000, pageLoadTimeoutInMs = 20_000, implicitTimeoutInMs = 1_000)
            WebDriverCommand.SetTimeouts(testSession, timeouts).execute()
        }

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("POST", request.method)
        Assertions.assertEquals("/session/$testSessionId/timeouts", request.path)
        Assertions.assertEquals(
                """{"script":15000,"pageLoad":20000,"implicit":1000}""",
                request.body.readString(Charsets.UTF_8)
        )
    }

    /**
     * See : [WebDriver - 9.1 Navigate To](https://www.w3.org/TR/webdriver/#navigate-to)
     */
    @Test
    internal fun navigateToCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("null")
        )

        val testSession = createWebDriverSession(testSessionId)
        with (testTarget) {
            WebDriverCommand.Go(testSession, "http://example.com/test").execute()
        }

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("POST", request.method)
        Assertions.assertEquals("/session/$testSessionId/url", request.path)
        Assertions.assertEquals(
                """{"url":"http://example.com/test"}""",
                request.body.readString(Charsets.UTF_8)
        )
    }

    /**
     * See : [WebDriver - 15.2.2 Execute Async Script](https://www.w3.org/TR/webdriver/#dfn-execute-async-script)
     */
    @Test
    internal fun executeAsyncScriptCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("[true, [1]]")
        )

        val testSession = createWebDriverSession(testSessionId)
        with (testTarget) {
            WebDriverCommand.ExecuteAsyncScript(testSession, Script("return [arguments[0]];", listOf(1))).execute()
        }

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("POST", request.method)
        Assertions.assertEquals("/session/$testSessionId/execute/async", request.path)
        Assertions.assertEquals(
                "{\"script\":\"\\n" +
                        "                    var code = new Function(arguments[0]);\\n" +
                        "                    var args = arguments[1];\\n" +
                        "                    var callback = arguments[2];\\n" +
                        "                    Promise.resolve().then(function () {\\n" +
                        "                      return code.apply(null, args);\\n" +
                        "                    }).then(function (r) {\\n" +
                        "                      callback([true, r]);\\n" +
                        "                    }, function (e) {\\n" +
                        "                      callback([false, e + \\\"\\\"]);\\n" +
                        "                    });\\n" +
                        "                    \",\"args\":[\"return [arguments[0]];\",[1]]}",
                request.body.readString(Charsets.UTF_8)
        )
    }

    /**
     * See : [WebDriver - 19.2 Take Element Screenshot](https://www.w3.org/TR/webdriver/#dfn-take-element-screenshot)
     *
     * Test screenshot is generated by [http://png-pixel.com/](http://png-pixel.com/).
     */
    @Test
    internal fun takeElementScreenshotCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        val testScreenshotEncoded = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("\"$testScreenshotEncoded\"")
        )

        val testSession = createWebDriverSession(testSessionId)
        val testElementReference = "04fcbabf-f354-414c-90b0-f61a25817b2f"
        val screenshot = with (testTarget) {
            WebDriverCommand.TakeElementScreenshot(testSession, WebElement(testElementReference)).execute()
        }

        val expectedScreenshot = javaClass.getResourceAsStream("/info/vividcode/wd/test/test-screenshot-small.png").readBytes()
        Assertions.assertArrayEquals(expectedScreenshot, screenshot)

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("GET", request.method)
        Assertions.assertEquals("/session/$testSessionId/element/$testElementReference/screenshot", request.path)
    }

    /**
     * See : [WebDriver - 19.1 Take Screenshot](https://www.w3.org/TR/webdriver/#dfn-take-screenshot)
     *
     * Test screenshot is generated by [http://png-pixel.com/](http://png-pixel.com/).
     */
    @Test
    internal fun takeScreenshotCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        val testScreenshotEncoded = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString("\"$testScreenshotEncoded\"")
        )

        val testSession = createWebDriverSession(testSessionId)
        val screenshot = with (testTarget) {
            WebDriverCommand.TakeScreenshot(testSession).execute()
        }

        val expectedScreenshot = javaClass.getResourceAsStream("/info/vividcode/wd/test/test-screenshot-small.png").readBytes()
        Assertions.assertArrayEquals(expectedScreenshot, screenshot)

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("GET", request.method)
        Assertions.assertEquals("/session/$testSessionId/screenshot", request.path)
    }

    /**
     * See : [WebDriver - 12.2 Find Element](https://www.w3.org/TR/webdriver/#find-element)
     */
    @Test
    internal fun findElementCommand() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val dispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(OkHttpClient.Builder().build(), url)
        val testTarget = OkHttpWebDriverCommandExecutor(dispatcher)

        val testElementReference = "04fcbabf-f354-414c-90b0-f61a25817b2f"
        mockWebServerResourceExtension.mockWebServer.enqueue(
                createSuccessTestResponseWithValueJsonString(
                        "{ \"element-6066-11e4-a52e-4f735466cecf\": \"$testElementReference\" }"
                )
        )

        val testSession = createWebDriverSession(testSessionId)
        val element = with (testTarget) {
            WebDriverCommand.FindElement(testSession, ElementSelector(ElementSelector.Strategy.XPATH, "//div")).execute()
        }

        Assertions.assertEquals(WebElement(testElementReference), element)

        Assertions.assertEquals(mockWebServerResourceExtension.mockWebServer.requestCount, 1)
        val request = mockWebServerResourceExtension.mockWebServer.takeRequest()
        Assertions.assertEquals("POST", request.method)
        Assertions.assertEquals("/session/$testSessionId/element", request.path)
        Assertions.assertEquals(
                """{"using":"xpath","value":"//div"}""",
                request.body.readString(Charsets.UTF_8)
        )
    }

    private fun createWebDriverSession(sessionId: String) = object : WebDriverSession { override val id = sessionId }

    /**
     * See : [WebDriver - *send a response*](https://www.w3.org/TR/webdriver/#dfn-send-a-response)
     */
    private fun createSuccessTestResponseWithValueJsonString(valueJsonString: String) =
            MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json; charset=utf-8")
                    .setHeader("Cache-Control", "no-cache")
                    .setBody("""{ "value": $valueJsonString }""")

}
