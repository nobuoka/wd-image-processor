package info.vividcode.wd.http

import info.vividcode.wd.Script
import info.vividcode.wd.ScriptResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import javax.json.Json

internal class ExecuteAsyncScriptCommandContentConverterTest {

    @Nested
    internal inner class CreateRequestJsonTest {
        @Test
        fun createRequestJson_noArg() {
            val testScript = Script("return \"Test\";")
            val requestJson = ExecuteAsyncScriptCommandContentConverter.createRequestJson(testScript)

            Assertions.assertEquals(Json.createObjectBuilder(mapOf(
                    "script" to script,
                    "args" to listOf("return \"Test\";", emptyList<Any>())
            )).build(), requestJson)
        }

        @Test
        fun createRequestJson_withArgs() {
            val testScript = Script("return args[0];", listOf("Hello world!"))
            val requestJson = ExecuteAsyncScriptCommandContentConverter.createRequestJson(testScript)

            Assertions.assertEquals(Json.createObjectBuilder(mapOf(
                    "script" to script,
                    "args" to listOf("return args[0];", listOf<Any>("Hello world!"))
            )).build(), requestJson)
        }
    }

    @Nested
    internal inner class ParseTest {
        @Test
        fun parseResponseJson_success_boolean() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, true))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Boolean(true)), result)
        }

        @Test
        fun parseResponseJson_success_string() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, "Test"))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.String("Test")), result)
        }

        @Test
        fun parseResponseJson_success_int() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, 1))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("1"))), result)
        }

        @Test
        fun parseResponseJson_success_long() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, 0x100000000L))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("4294967296"))), result)
        }

        @Test
        fun parseResponseJson_success_bigDecimal() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, BigInteger("100000000000000000000")))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("100000000000000000000"))), result)
        }

        @Test
        fun parseResponseJson_success_double() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, 0.5))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Number(BigDecimal("0.5"))), result)
        }

        @Test
        fun parseResponseJson_success_array() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, listOf(1, 2)))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            val expected = successResult(ScriptResult.Array(Json.createArrayBuilder(listOf(1, 2)).build()))
            Assertions.assertEquals(expected, result)
        }

        @Test
        fun parseResponseJson_success_object() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, mapOf("key1" to 1, "key2" to 2)))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            val expected = successResult(ScriptResult.Object(Json.createObjectBuilder(mapOf("key1" to 1, "key2" to 2)).build()))
            Assertions.assertEquals(expected, result)
        }

        @Test
        fun parseResponseJson_success_null() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true, null))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(successResult(ScriptResult.Null), result)
        }

        @Test
        fun parseResponseJson_error() {
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(false, "Test error message"))).build()
            val result = ExecuteAsyncScriptCommandContentConverter.parseResponseJson(testValue)
            Assertions.assertEquals(ExecuteAsyncScriptCommandContentConverter.JavaScriptResult.Error("Test error message"), result)
        }

        @Test
        fun parseResponseJson_invalidInput_noValueProperty() {
            val testValue = Json.createObjectBuilder(mapOf("key" to "value")).build()
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
            val testValue = Json.createObjectBuilder(mapOf("value" to "value")).build()
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
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(null, ""))).build()
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
            val testValue = Json.createObjectBuilder(mapOf("value" to listOf(true))).build()
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
    }

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