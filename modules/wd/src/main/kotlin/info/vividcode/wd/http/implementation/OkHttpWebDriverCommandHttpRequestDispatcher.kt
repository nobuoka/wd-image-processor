package info.vividcode.wd.http.implementation

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import info.vividcode.wd.WebDriverError
import info.vividcode.wd.http.WebDriverCommandHttpRequest
import info.vividcode.wd.http.WebDriverCommandHttpRequestDispatcher
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class OkHttpWebDriverCommandHttpRequestDispatcher(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) : WebDriverCommandHttpRequestDispatcher {

    override fun <T> dispatch(commandHttpRequest: WebDriverCommandHttpRequest, responseHandler: (JsonObject) -> T): T =
        responseHandler(dispatch(commandHttpRequest))

    private fun dispatch(commandHttpRequest: WebDriverCommandHttpRequest): JsonObject {
        val okHttpRequest = Request.Builder()
            .url(baseUrl + commandHttpRequest.path)
            .method(commandHttpRequest.method, commandHttpRequest.requestContent?.let { RequestBody.create(MediaType.parse("application/json"), it) })
            .build()
        val response = okHttpClient.newCall(okHttpRequest).execute()
        if (response.isSuccessful) {
            return response.body()?.charStream()?.let { Parser().parse(it) } as? JsonObject
                    ?: throw RuntimeException("Unexpected response: $response")
        } else {
            val jsonErrorCode = response.body()?.let { body ->
                if (MediaType.parse("application/json; charset=utf-8") == body.contentType()) {
                    (body.charStream().let { Parser().parse(it) } as? JsonObject)?.obj("value")?.string("error")
                } else {
                    null
                }
            }
            if (response.code() == 408 && jsonErrorCode == "script timeout") {
                throw WebDriverError.ScriptTimeout()
            } else {
                throw RuntimeException("Http request error: $response")
            }
        }
    }

}
