package info.vividcode.wd.http.implementation

import info.vividcode.wd.WebDriverError
import info.vividcode.wd.http.WebDriverCommandHttpRequest
import info.vividcode.wd.http.WebDriverCommandHttpRequestDispatcher
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.StringReader
import javax.json.Json
import javax.json.JsonObject

class OkHttpWebDriverCommandHttpRequestDispatcher(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) : WebDriverCommandHttpRequestDispatcher {

    class Factory(private val okHttpClient: OkHttpClient) : WebDriverCommandHttpRequestDispatcher.Factory {
        override fun create(baseUrl: String): WebDriverCommandHttpRequestDispatcher =
                OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, baseUrl)
    }

    override fun <T> dispatch(commandHttpRequest: WebDriverCommandHttpRequest, responseHandler: (JsonObject) -> T): T =
        responseHandler(dispatch(commandHttpRequest))

    private fun dispatch(commandHttpRequest: WebDriverCommandHttpRequest): JsonObject {
        val okHttpRequest = Request.Builder()
            .url(baseUrl + commandHttpRequest.path)
            .method(commandHttpRequest.method, commandHttpRequest.requestContent?.let {
                RequestBody.create(MediaType.parse("application/json"), it)
            })
            .build()
        val response = okHttpClient.newCall(okHttpRequest).execute()
        if (response.isSuccessful) {
            return response.body()?.string()?.let { responseBody ->
                try {
                    Json.createReader(StringReader(responseBody)).readValue()
                } catch (e: RuntimeException) {
                    throw RuntimeException("Parsing JSON failed (response body : $responseBody)", e)
                }
            } as? JsonObject ?: throw RuntimeException("Unexpected response: $response")
        } else {
            val jsonErrorCode = response.body()?.let { body ->
                if (MediaType.parse("application/json; charset=utf-8") == body.contentType()) {
                    (body.charStream().let { Json.createReader(it).readValue() } as? JsonObject)
                            ?.getJsonObject("value")?.getString("error")
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
