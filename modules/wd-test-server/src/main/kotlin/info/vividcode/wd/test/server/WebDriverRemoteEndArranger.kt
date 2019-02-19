package info.vividcode.wd.test.server

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import java.util.*
import java.util.concurrent.TimeUnit

class WebDriverRemoteEndArranger(private val server: MockWebServer) {

    fun expectNewSessionCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("POST"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session"))

        server.enqueue(createResponse(200, createNewSessionCommandResponseJson(testSessionId)))
    }

    fun expectDeleteSessionCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("DELETE"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session/$testSessionId"))

        server.enqueue(createResponse(200, createDeleteSessionCommandResponseJson()))
    }

    fun expectSetTimeoutsCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(100, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("POST"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session/$testSessionId/timeouts"))

        server.enqueue(createResponse(200, createSetTimeoutsCommandResponseJson()))
    }

    /**
     * [Create a response to send](https://www.w3.org/TR/webdriver/#dfn-send-a-response).
     */
    private fun createResponse(status: Int, data: JsonValue?) = createResponseInternal(status, data)

    /**
     * [Create a response to send](https://www.w3.org/TR/webdriver/#dfn-send-a-response).
     */
    private fun createResponse(status: Int, data: JsonObject) = createResponseInternal(status, data)

    /**
     * [Create a response to send](https://www.w3.org/TR/webdriver/#dfn-send-a-response).
     */
    private fun createResponseInternal(status: Int, data: Any?) =
            MockResponse()
                    .setResponseCode(status)
                    .setHeaders(Headers.of(mapOf(
                            "Content-Type" to "application/json; charset=utf-8",
                            "Cache-Control" to "no-cache"
                    )))
                    .setBody(JsonObject(mapOf("value" to data)).toJsonString())

    /**
     * [New Session command](https://www.w3.org/TR/webdriver/#new-session).
     */
    private fun createNewSessionCommandResponseJson(sessionId: UUID): JsonObject =
            JsonObject(mapOf("sessionId" to sessionId.toString(), "capabilities" to JsonObject()))

    /**
     * [Delete Session command](https://www.w3.org/TR/webdriver/#dfn-delete-session).
     */
    private fun createDeleteSessionCommandResponseJson(): JsonValue? = null

    /**
     * [New Session command](https://www.w3.org/TR/webdriver/#new-session).
     */
    private fun createSetTimeoutsCommandResponseJson(): JsonValue? = null

}
