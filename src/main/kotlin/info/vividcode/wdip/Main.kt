@file:JvmName("Main")

package info.vividcode.wdip

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import info.vividcode.wd.*
import java.nio.charset.StandardCharsets

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    startServer()
}

data class WdipSetting(
    val accessControlAllowOrigins: List<String>,
    val processors: List<ProcessorSetting>
)
data class ProcessorSetting(val path: String, val html: String, val js: String, val key: String?)

fun parseProcessorsConfigJson(jsonFile: Path): WdipSetting {
    fun Path.readContent(): String = Files.readAllBytes(jsonFile.parent.resolve(this)).toString(StandardCharsets.UTF_8)

    val configObject = jsonFile.toFile().reader().use { Klaxon().parseJsonObject(it) }
    return WdipSetting(
        accessControlAllowOrigins =
        (configObject["accessControlAllowOrigins"] as JsonArray<*>?)?.map { it as String } ?: emptyList(),
        processors =
        (configObject["processors"] as JsonObject?)?.let {
            it.entries.map { (path, config) ->
                val htmlString = (config as? JsonObject)?.string("html")?.let { Paths.get(it).readContent() }
                val jsString = (config as? JsonObject)?.string("js")?.let { Paths.get(it).readContent() }
                val key = (config as? JsonObject)?.string("key")
                ProcessorSetting(path, htmlString ?: "", jsString ?: "", key)
            }
        } ?: emptyList())
}

class WdImageProcessingExecutor(
    private val webDriverConnectionManager: WebDriverConnectionManager
) {
    suspend fun execute(htmlString: String, jsString: String, jsArg: String): WdImageProcessingResult =
        webDriverConnectionManager.withSession { session ->
            executeImageProcessorWithElementScreenshot(session, htmlString, jsString, jsArg)
        }
}

data class ScreenshotRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class ImageProcessorScriptResponse(
    val targetElement: WebElement?,
    val statusCode: Int,
    val httpCache: HttpCache?
)

data class HttpCache(
    val maxAge: Int?
)
