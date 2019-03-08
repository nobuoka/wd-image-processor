@file:JvmName("Main")

package info.vividcode.wdip

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonString

fun main(args: Array<String>) {
    val wdSessionManager = createWebDriverSessionManager(
            webDriverBaseUrls = (System.getenv("WD_BASE_URLS") ?: "http://localhost:10001").split(Regex("\\s")),
            // Session of WebDriver will be recreated after requests are received `WD_SESSION_CAPACITY` times
            webDriverSessionCapacity = System.getenv("WD_SESSION_CAPACITY")?.toIntOrNull() ?: 10
    )

    val processorsConfigJsonPath = System.getenv("PROCESSORS_CONFIG_PATH") ?: "./sampleProcessors/config.json"

    startServer(processorsConfigJsonPath, wdSessionManager)
}

data class WdipSetting(
    val accessControlAllowOrigins: Set<String>,
    val processors: List<ProcessorSetting>
)
data class ProcessorSetting(val path: String, val html: String, val js: String, val key: String?)

fun parseProcessorsConfigJson(jsonFile: Path): WdipSetting {
    fun Path.readContent(): String = Files.readAllBytes(jsonFile.parent.resolve(this)).toString(StandardCharsets.UTF_8)

    val configObject = jsonFile.toFile().reader().use { Json.createReader(it).readObject() }
    return WdipSetting(
        accessControlAllowOrigins =
        (configObject["accessControlAllowOrigins"] as JsonArray?)?.map { (it as JsonString).string }?.toSet() ?: emptySet(),
        processors =
        (configObject["processors"] as JsonObject?)?.let {
            it.entries.map { (path, config) ->
                val htmlString = (config as? JsonObject)?.getJsonString("html")?.let { Paths.get(it.string).readContent() }
                val jsString = (config as? JsonObject)?.getJsonString("js")?.let { Paths.get(it.string).readContent() }
                val key = (config as? JsonObject)?.getJsonString("key")?.string
                ProcessorSetting(path, htmlString ?: "", jsString ?: "", key)
            }
        } ?: emptyList())
}
