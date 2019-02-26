package info.vividcode.wd.http

import info.vividcode.wd.Script
import info.vividcode.wd.ScriptResult
import javax.json.*

internal object ExecuteAsyncScriptCommandContentConverter {

    fun createRequestJson(script: Script): JsonObject = Json.createObjectBuilder(
            mapOf(
                    // From https://github.com/manakai/perl-web-driver-client/blob/master/lib/Web/Driver/Client/Session.pm#L103
                    "script" to """
                    var code = new Function(arguments[0]);
                    var args = arguments[1];
                    var callback = arguments[2];
                    Promise.resolve().then(function () {
                      return code.apply(null, args);
                    }).then(function (r) {
                      callback([true, r]);
                    }, function (e) {
                      callback([false, e + ""]);
                    });
                    """,
                    "args" to listOf(
                            script.script,
                            script.args ?: emptyList<Any?>()
                    )
            )
    ).build()

    fun parseResponseJson(responseJson: JsonObject): JavaScriptResult =
            responseJson["value"].let { value ->
                if (value is JsonArray) {
                    value
                } else {
                    throw UnexpectedResponseContentException(
                            "The `value` field of response of Execute Async Script command is not array. " +
                                    "(response : $responseJson)"
                    )
                }
            }.let { resultArray ->
                if (resultArray.size != 2) {
                    throw UnexpectedResponseContentException(
                            "The size of `value` array of response of Execute Async Script command must contains 2 items. " +
                                    "(value : $resultArray)"
                    )
                }
                when (resultArray[0]) {
                    JsonValue.TRUE ->
                        when (val successResultValue = resultArray[1]) {
                            is JsonNumber -> ScriptResult.Number(successResultValue.bigDecimalValue())
                            is JsonString -> ScriptResult.String(successResultValue.string)
                            is JsonObject -> ScriptResult.Object(successResultValue)
                            is JsonArray -> ScriptResult.Array(successResultValue)
                            JsonValue.TRUE -> ScriptResult.Boolean(true)
                            JsonValue.FALSE -> ScriptResult.Boolean(false)
                            JsonValue.NULL -> ScriptResult.Null
                            else ->
                                throw UnexpectedResponseContentException(
                                        "Execute Async Script command returns success, but data type is unexpected " +
                                                "(data : $successResultValue," +
                                                " type of data : ${successResultValue::class.qualifiedName})"
                                )
                        }.let(JavaScriptResult::Success)
                    JsonValue.FALSE -> {
                        when (val errorMessageValue = resultArray[1]) {
                            is JsonString -> JavaScriptResult.Error(errorMessageValue.string)
                            else ->
                                throw UnexpectedResponseContentException(
                                        "Execute Async Script command returns error, but error message type is unexpected " +
                                                "(error message : $errorMessageValue," +
                                                " type of error message : ${errorMessageValue::class.qualifiedName})"
                                )
                        }
                    }
                    else ->
                        throw UnexpectedResponseContentException(
                                "Unexpected result value from Execute Async Script command (value : $resultArray)"
                        )
                }
            }

    class UnexpectedResponseContentException(message: String) : RuntimeException(message)

    sealed class JavaScriptResult {
        data class Success(val value: ScriptResult) : JavaScriptResult()
        data class Error(val message: String) : JavaScriptResult()
    }

}
