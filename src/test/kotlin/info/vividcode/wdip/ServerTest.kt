package info.vividcode.wdip

import info.vividcode.wd.Timeouts
import info.vividcode.wdip.application.WebDriverConnectionManager
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ServerTest {

    private fun <T> withWdipApplicationModule(test: TestApplicationEngine.() -> T) = withTestApplication {
        val config = WdipSetting(emptySet(), emptyList())
        val wdSessionManager = WebDriverConnectionManager(OkHttpClient(), listOf("http://test.com/wd"), 1, Timeouts(1, 1, 1))
        application.setup(config, wdSessionManager, emptyList())

        test()
    }

    @Test
    internal fun healthCheck() = withWdipApplicationModule {
        handleRequest {
            uri = "/-/health/app-only"
        }.let { call ->
            Assertions.assertTrue(call.requestHandled)
            Assertions.assertEquals(HttpStatusCode.OK, call.response.status())
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

    @Test
    internal fun notFound() = withWdipApplicationModule {
        handleRequest {
            uri = "/not-found"
        }.let { call ->
            Assertions.assertFalse(call.requestHandled)
            Assertions.assertNull(call.response.status())
            Assertions.assertEquals(
                    Headers.build {
                        set("Vary", "Origin")
                        set("X-Content-Type-Options", "nosniff")
                    },
                    call.response.headers.allValues()
            )
        }
    }

}
