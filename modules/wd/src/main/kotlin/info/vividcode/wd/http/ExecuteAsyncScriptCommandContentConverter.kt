package info.vividcode.wd.http

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import info.vividcode.wd.Script
import info.vividcode.wd.ScriptResult
import java.math.BigDecimal
import java.math.BigInteger

internal object ExecuteAsyncScriptCommandContentConverter {

    fun createRequestJson(script: Script): JsonObject = JsonObject(
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
                    "args" to JsonArray(
                            script.script,
                            JsonArray(script.args ?: emptyList())
                    )
            )
    )

    fun parseResponseJson(responseJson: JsonObject): JavaScriptResult =
            responseJson["value"].let { value ->
                if (value is JsonArray<*>) {
                    value
                } else {
                    throw UnexpectedResponseContentException(
                            "The `value` field of response of Execute Async Script command is not array. " +
                                    "(response : ${responseJson.toJsonString()})"
                    )
                }
            }.let { resultArray ->
                if (resultArray.size != 2) {
                    throw UnexpectedResponseContentException(
                            "The size of `value` array of response of Execute Async Script command must contains 2 items. " +
                                    "(value : ${resultArray.toJsonString()})"
                    )
                }
                when (resultArray[0]) {
                    true ->
                        when (val successResultValue = resultArray[1]) {
                            // Possible types : https://github.com/cbeust/klaxon#low-level-api
                            is Int -> ScriptResult.Number(BigDecimal(successResultValue))
                            is Long -> ScriptResult.Number(BigDecimal(successResultValue))
                            is BigInteger -> ScriptResult.Number(BigDecimal(successResultValue))
                            is Double -> ScriptResult.Number(BigDecimal(successResultValue))
                            is String -> ScriptResult.String(successResultValue)
                            is Boolean -> ScriptResult.Boolean(successResultValue)
                            is JsonObject -> ScriptResult.Object(successResultValue)
                            is JsonArray<*> -> ScriptResult.Array(successResultValue)
                            null -> ScriptResult.Null
                            else ->
                                throw UnexpectedResponseContentException(
                                        "Execute Async Script command returns success, but data type is unexpected " +
                                                "(data : $successResultValue, type of data : ${successResultValue::class.qualifiedName})"
                                )
                        }.let(JavaScriptResult::Success)
                    false -> JavaScriptResult.Error("${resultArray[1]}")
                    else ->
                        throw UnexpectedResponseContentException(
                                "Unexpected result value from Execute Async Script command (value : ${resultArray.toJsonString()})"
                        )
                }
            }

    class UnexpectedResponseContentException(message: String) : RuntimeException(message)

    sealed class JavaScriptResult {
        data class Success(val value: ScriptResult) : JavaScriptResult()
        data class Error(val message: String) : JavaScriptResult()
    }

}
