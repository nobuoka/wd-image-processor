package info.vividcode.wdip

import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import info.vividcode.wd.pool.WebDriverConnectionManager
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.withTestApplication
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class WebDriverImageProcessorModuleTest {

    private val testTarget = run {
        val config = WdipSetting(emptySet(), listOf(
                ProcessorSetting("test", "<!DOCTYPE html><html lang=\"en\"><title>test</title>", "return null;", null)
        ))
        val wdSessionManager = WebDriverConnectionManager(
                OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient()), listOf("http://test.com/wd"),
                1, Timeouts(1, 1, 1)
        )

        WebDriverImageProcessorModule(config, wdSessionManager)
    }

    @Test
    internal fun healthCheck() = withTestApplication(testTarget) {
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

}
