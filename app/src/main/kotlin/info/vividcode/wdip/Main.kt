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
    val getenv: (String) -> String? = System::getenv
    val env = ApplicationEnvironmentVariables.load(getenv)

    val wdSessionManager = createWebDriverSessionManager(env.webDriverBaseUrls, env.webDriverSessionCapacity)

    val processorsConfigJsonPath = env.processorsConfigPath
    val config = parseProcessorsConfigJson(Paths.get(processorsConfigJsonPath))

    startServer(WebDriverImageProcessorModule(config, wdSessionManager))
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
