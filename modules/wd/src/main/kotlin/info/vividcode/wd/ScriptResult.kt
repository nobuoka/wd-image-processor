package info.vividcode.wd

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import java.math.BigDecimal

sealed class ScriptResult {
    data class Object(val value: JsonObject) : ScriptResult()
    data class Array(val value: JsonArray<*>) : ScriptResult()
    data class String(val value: kotlin.String) : ScriptResult()
    data class Number(val value: BigDecimal) : ScriptResult()
    data class Boolean(val value: kotlin.Boolean) : ScriptResult()
    object Null : ScriptResult()
}