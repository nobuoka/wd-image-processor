package info.vividcode.wdip

import info.vividcode.wd.Timeouts
import info.vividcode.wdip.application.WebDriverConnectionManager
import io.ktor.http.Headers
import io.ktor.server.testing.withTestApplication
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ServerTest {

    @Test
    internal fun healthCheck() = withTestApplication {
        val config = WdipSetting(emptySet(), emptyList())
        val wdSessionManager = WebDriverConnectionManager(OkHttpClient(), listOf("http://test.com/wd"), 1, Timeouts(1, 1, 1))
        application.setup(config, wdSessionManager, emptyList())

        handleRequest {
            uri = "/-/health/app-only"
        }.let { call ->
            Assertions.assertEquals(
                    Headers.build {
                        set("Vary", "Origin")
                        set("X-Content-Type-Options", "nosniff")
                        set("Content-Length", "2")
                        set("Content-Type", "text/plain; charset=UTF-8")
                    },
                    call.response.headers.allValues()
            )
            Assertions.assertEquals("OK", call.response.content)
        }
    }

}
