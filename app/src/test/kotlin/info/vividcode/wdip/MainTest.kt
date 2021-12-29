package info.vividcode.wdip

import info.vividcode.wdip.web.ProcessorSetting
import info.vividcode.wdip.web.WdipSetting
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

internal class MainTest {

    @Nested
    internal inner class CreateWebDriverImageProcessorModuleTest {
        @Test
        internal fun normal() {
            val env = mapOf("PROCESSORS_CONFIG_PATH" to "./test.json")
            val files = mapOf(Paths.get("./test.json") to "{}".toByteArray())

            createWebDriverImageProcessorModule(env::get, files::getValue)
        }
    }

    @Nested
    internal inner class ParseProcessorsConfigJsonTest {
        @Test
        internal fun normal() {
            @Language("JSON") val configJson = """
                {
                  "accessControlAllowOrigins": ["http://example.com"],
                  "processors": {
                    "test": {
                      "html": "./test.html",
                      "js": "./test.js"
                    }
                  }
                }
            """.trimIndent()
            val configPath = Paths.get("./test.json")
            val files = mapOf(
                    configPath to configJson,
                    Paths.get("./test.html") to "<!DOCTYPE html><title>test</title>",
                    Paths.get("./test.js") to "return 100;"
            ).mapKeys { (path, _) -> path.normalize() }.mapValues { (_, content) -> content.toByteArray() }
            val fileContentLoader = { target: Path -> files.getValue(target.normalize()) }

            val setting = parseProcessorsConfigJson(configPath, fileContentLoader)

            Assertions.assertEquals(
                    WdipSetting(
                            accessControlAllowOrigins = setOf("http://example.com"),
                            processors = listOf(
                                    ProcessorSetting(
                                            path = "test",
                                            html = "<!DOCTYPE html><title>test</title>",
                                            js = "return 100;",
                                            key = null
                                    )
                            )
                    ),
                    setting
            )
        }
    }

}
