package info.vividcode.wd.http.implementation

import info.vividcode.test.utils.MockWebServerResourceExtension
import info.vividcode.wd.http.WebDriverCommandHttpRequest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import javax.json.Json

internal class OkHttpWebDriverCommandHttpRequestDispatcherTest {

    @JvmField
    @RegisterExtension
    internal val mockWebServerResourceExtension = MockWebServerResourceExtension()

    private val factory = OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient.Builder().build())

    @Test
    internal fun dispatch_successfulResponse_responseContentJson_object() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val testTarget = factory.create(url)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{ \"test\": 100 }")
        )

        // Act
        val result = testTarget.dispatch(WebDriverCommandHttpRequest("GET", "/test", null)) { it }

        // Assert
        Assertions.assertEquals(Json.createObjectBuilder(mapOf("test" to 100)).build(), result)
    }

    @Test
    internal fun dispatch_successfulResponse_responseContentJson_notObject() {
        val baseUrl = mockWebServerResourceExtension.mockWebServerUrl
        val testTarget = factory.create(baseUrl)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("\"test\"")
        )

        // Act
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            testTarget.dispatch(WebDriverCommandHttpRequest("GET", "/test", null)) { it }
        }

        // Assert
        Assertions.assertEquals(
                "Unexpected response: Response{protocol=http/1.1, code=200, message=OK, url=$baseUrl/test}",
                exception.message
        )
    }

    @Test
    internal fun dispatch_successfulResponse_responseContentNotJson() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val testTarget = factory.create(url)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "text/plain; charset=utf-8")
                        .setBody("Test response content.")
        )

        // Act
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            testTarget.dispatch(WebDriverCommandHttpRequest("GET", "/test", null)) { it }
        }

        // Assert
        Assertions.assertEquals("Parsing JSON failed (response body : Test response content.)", exception.message)
    }

    @Test
    internal fun dispatch_errorResponse() {
        val url = mockWebServerResourceExtension.mockWebServerUrl
        val testTarget = factory.create(url)

        mockWebServerResourceExtension.mockWebServer.enqueue(
                MockResponse()
                        .setResponseCode(500)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{ \"message\": \"Test error\" }")
        )

        // Act
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            testTarget.dispatch(WebDriverCommandHttpRequest("GET", "/test", null)) { it }
        }

        // Assert
        Assertions.assertEquals("Http request error: Response{protocol=http/1.1, code=500, message=Server Error, url=$url/test}", exception.message)
    }

}
