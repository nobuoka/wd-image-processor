package info.vividcode.wdip

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ApplicationEnvironmentVariablesTest {

    @Nested
    internal inner class LoadTest {
        @Test
        internal fun normal() {
            val env = mapOf(
                    "WD_BASE_URLS" to "http://example.com/wd",
                    "WD_SESSION_CAPACITY" to "5",
                    "PROCESSORS_CONFIG_PATH" to "./example.json"
            )

            val vars = ApplicationEnvironmentVariables.load(env::get)

            Assertions.assertEquals(listOf("http://example.com/wd"), vars.webDriverBaseUrls)
            Assertions.assertEquals(5, vars.webDriverSessionCapacity)
            Assertions.assertEquals("./example.json", vars.processorsConfigPath)
        }

        @Test
        internal fun default() {
            val env = emptyMap<String, String>()

            val vars = ApplicationEnvironmentVariables.load(env::get)

            Assertions.assertEquals(listOf("http://localhost:10001"), vars.webDriverBaseUrls)
            Assertions.assertEquals(10, vars.webDriverSessionCapacity)
            Assertions.assertEquals("./sampleProcessors/config.json", vars.processorsConfigPath)
        }
    }

}
