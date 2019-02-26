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
                    JsonValue.TRUE -> resultArray[1].let { successResultValue ->
                        when (successResultValue.valueType!!) {
                            JsonValue.ValueType.NUMBER -> ScriptResult.Number((successResultValue as JsonNumber).bigDecimalValue())
                            JsonValue.ValueType.STRING -> ScriptResult.String((successResultValue as JsonString).string)
                            JsonValue.ValueType.OBJECT -> ScriptResult.Object(successResultValue as JsonObject)
                            JsonValue.ValueType.ARRAY -> ScriptResult.Array(successResultValue as JsonArray)
                            JsonValue.ValueType.TRUE -> ScriptResult.Boolean(true)
                            JsonValue.ValueType.FALSE -> ScriptResult.Boolean(false)
                            JsonValue.ValueType.NULL -> ScriptResult.Null
                        }.let(JavaScriptResult::Success)
                    }
                    JsonValue.FALSE -> {
                        when (val errorMessageValue = resultArray[1]) {
                            is JsonString -> JavaScriptResult.Error(errorMessageValue.string)
                            else ->
                                throw UnexpectedResponseContentException(
                                        "Execute Async Script command returns error, but error message type is unexpected " +
                                                "(error message : $errorMessageValue," +
                                                " type of error message : ${errorMessageValue.valueType})"
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
