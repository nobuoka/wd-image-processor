package info.vividcode.wd.test.server

import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import java.util.*
import java.util.concurrent.TimeUnit
import javax.json.Json
import javax.json.JsonValue

class WebDriverRemoteEndArranger(private val server: MockWebServer) {

    fun expectNewSessionCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(1000, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("POST"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session"))

        server.enqueue(createResponse(200, createNewSessionCommandResponseJson(testSessionId)))
    }

    fun expectDeleteSessionCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(1000, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("DELETE"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session/$testSessionId"))

        server.enqueue(createResponse(200, createDeleteSessionCommandResponseJson()))
    }

    fun expectSetTimeoutsCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(1000, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("POST"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session/$testSessionId/timeouts"))

        server.enqueue(createResponse(200, createSetTimeoutsCommandResponseJson()))
    }

    fun expectGoCommand(testSessionId: UUID) {
        val request = requireNotNull(server.takeRequest(1000, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("POST"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session/$testSessionId/url"))

        server.enqueue(createResponse(200, createDeleteSessionCommandResponseJson()))
    }

    fun expectExecuteAsyncScriptCommand(testSessionId: UUID, scriptResultValue: JsonValue) {
        val request = requireNotNull(server.takeRequest(1000, TimeUnit.MILLISECONDS))
        MatcherAssert.assertThat(request.method, CoreMatchers.equalTo("POST"))
        MatcherAssert.assertThat(request.path, CoreMatchers.equalTo("/session/$testSessionId/execute/async"))

        server.enqueue(createResponse(200, Json.createArrayBuilder().add(true).add(scriptResultValue).build()))
    }

    /**
     * [Create a response to send](https://www.w3.org/TR/webdriver/#dfn-send-a-response).
     */
    private fun createResponse(status: Int, data: JsonValue) =
            createResponseInternal(status, Json.createObjectBuilder().add("value", data).build().toString())

    /**
     * [Create a response to send](https://www.w3.org/TR/webdriver/#dfn-send-a-response).
     */
    private fun createResponseInternal(status: Int, jsonString: String) =
            MockResponse()
                    .setResponseCode(status)
                    .setHeaders(Headers.of(mapOf(
                            "Content-Type" to "application/json; charset=utf-8",
                            "Cache-Control" to "no-cache"
                    )))
                    .setBody(jsonString)

    /**
     * [New Session command](https://www.w3.org/TR/webdriver/#new-session).
     */
    private fun createNewSessionCommandResponseJson(sessionId: UUID): JsonValue =
            Json.createObjectBuilder()
                    .add("sessionId", sessionId.toString())
                    .add("capabilities", JsonValue.EMPTY_JSON_OBJECT)
                    .build()

    /**
     * [Delete Session command](https://www.w3.org/TR/webdriver/#dfn-delete-session).
     */
    private fun createDeleteSessionCommandResponseJson(): JsonValue = JsonValue.NULL

    /**
     * [New Session command](https://www.w3.org/TR/webdriver/#new-session).
     */
    private fun createSetTimeoutsCommandResponseJson(): JsonValue = JsonValue.NULL

}
