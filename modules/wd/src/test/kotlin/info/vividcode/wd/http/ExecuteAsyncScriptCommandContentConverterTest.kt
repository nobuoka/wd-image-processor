package info.vividcode.wd.http

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import info.vividcode.wd.Script
import info.vividcode.wd.ScriptResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class ExecuteAsyncScriptCommandContentConverterTest {

    @Nested
    internal inner class CreateRequestJsonTest {
        @Test
        fun createRequestJson_noArg() {
            val testScript = Script("return \"Test\";")
            val requestJson = ExecuteAsyncScriptCommandContentConverter.createRequestJson(testScript)

            Assertions.assertEquals(JsonObject(mapOf(
                    "script" to script,
                    "args" to JsonArray("return \"Test\";", emptyList<Any>())
            )), requestJson)
        }

        @Test
        fun createRequestJson_withArgs() {
            val testScript = Script("return args[0];", listOf("Hello world!"))
            val requestJson = ExecuteAsyncScriptCommandContentConverter.createRequestJson(testScript)

            Assertions.assertEquals(JsonObject(mapOf(
                    "script" to script,
                    "args" to JsonArray("return args[0];", listOf<Any>("Hello world!"))
            )), requestJson)
        }
    }

    @Nested
    internal inner class ParseTest {
        @Test
        fun parseResponseJson_success_boolean() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, true)))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Boolean(true)), result)
        }

        @Test
        fun parseResponseJson_success_string() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, "Test")))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.String("Test")), result)
        }

        @Test
        fun parseResponseJson_success_int() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, 1)))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("1"))), result)
        }

        @Test
        fun parseResponseJson_success_long() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, 0x100000000L)))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("4294967296"))), result)
        }

        @Test
        fun parseResponseJson_success_bigDecimal() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, BigInteger("100000000000000000000"))))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("100000000000000000000"))), result)
        }

        @Test
        fun parseResponseJson_success_double() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, 0.5)))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("0.5"))), result)
        }

        @Test
        fun parseResponseJson_success_array() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, JsonArray(1, 2))))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Array(JsonArray(1, 2))), result)
        }

        @Test
        fun parseResponseJson_success_object() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, JsonObject(mapOf("key1" to 1, "key2" to 2)))))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Object(JsonObject(mapOf("key1" to 1, "key2" to 2)))), result)
        }

        @Test
        fun parseResponseJson_success_null() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, null)))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Null), result)
        }

        @Test
        fun parseResponseJson_error() {
            val testValue = JsonObject(mapOf("value" to JsonArray(false, "Test error message")))
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(ExecuteAsyncScriptCommandContentConverter.JavaScriptResult.Error("Test error message"), result)
        }

        @Test
        fun parseResponseJson_invalidInput_noValueProperty() {
            val testValue = JsonObject(mapOf("key" to "value"))
            try {
                ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
                Assertions.fail<Any>("Exception must be thrown.")
            } catch (e: ExecuteAsyncScriptCommandContentConverter.UnexpectedResponseContentException) {
                Assertions.assertEquals(
                        "The `value` field of response of Execute Async Script command is not array. " +
                                "(response : {\"key\":\"value\"})",
                        e.message
                )
            }
        }

        @Test
        fun parseResponseJson_invalidInput_notArrayValue() {
            val testValue = JsonObject(mapOf("value" to "value"))
            try {
                ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
                Assertions.fail<Any>("Exception must be thrown.")
            } catch (e: ExecuteAsyncScriptCommandContentConverter.UnexpectedResponseContentException) {
                Assertions.assertEquals(
                        "The `value` field of response of Execute Async Script command is not array. " +
                                "(response : {\"value\":\"value\"})",
                        e.message
                )
            }
        }

        @Test
        fun parseResponseJson_invalidInput_firstItemNotBoolean() {
            val testValue = JsonObject(mapOf("value" to JsonArray(null, "")))
            try {
                ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
                Assertions.fail<Any>("Exception must be thrown.")
            } catch (e: ExecuteAsyncScriptCommandContentConverter.UnexpectedResponseContentException) {
                Assertions.assertEquals(
                        "Unexpected result value from Execute Async Script command (value : [null,\"\"])",
                        e.message
                )
            }
        }

        @Test
        fun parseResponseJson_invalidInput_noSecondItem() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true)))
            try {
                ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
                Assertions.fail<Any>("Exception must be thrown.")
            } catch (e: ExecuteAsyncScriptCommandContentConverter.UnexpectedResponseContentException) {
                Assertions.assertEquals(
                        "The size of `value` array of response of Execute Async Script command must contains 2 items. (value : [true])",
                        e.message
                )
            }
        }

        @Test
        fun parseResponseJson_invalidInput_unknownType() {
            val testValue = JsonObject(mapOf("value" to JsonArray(true, TestObject("Test"))))
            try {
                ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
                Assertions.fail<Any>("Exception must be thrown.")
            } catch (e: ExecuteAsyncScriptCommandContentConverter.UnexpectedResponseContentException) {
                Assertions.assertEquals(
                        "Execute Async Script command returns success, but data type is unexpected " +
                                "(data : TestObject(message=Test), type of data : " +
                                "info.vividcode.wd.http.ExecuteAsyncScriptCommandContentConverterTest.TestObject)",
                        e.message
                )
            }
        }
    }

    private data class TestObject(val message: String)

    companion object {
        fun successResult(value: ScriptResult) = ExecuteAsyncScriptCommandContentConverter.JavaScriptResult.Success(value)

        private const val script = """
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
                    """
    }

}