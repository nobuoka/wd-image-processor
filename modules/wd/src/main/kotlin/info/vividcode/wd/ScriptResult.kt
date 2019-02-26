package info.vividcode.wd

import java.math.BigDecimal
import javax.json.JsonArray
import javax.json.JsonObject

sealed class ScriptResult {
    data class Object(val value: JsonObject) : ScriptResult()
    data class Array(val value: JsonArray) : ScriptResult()
    data class String(val value: kotlin.String) : ScriptResult()
    data class Number(val value: BigDecimal) : ScriptResult()
    data class Boolean(val value: kotlin.Boolean) : ScriptResult()
    object Null : ScriptResult()
}
