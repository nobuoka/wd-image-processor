package info.vividcode.wdip

import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import info.vividcode.wd.pool.WebDriverConnectionManager
import info.vividcode.wdip.web.WdipSetting
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

internal class ServerTest {

    private fun <T> withWdipApplicationModule(test: TestApplicationEngine.() -> T) = withTestApplication {
        val config = WdipSetting(emptySet(), emptyList())
        val wdSessionManager = WebDriverConnectionManager(
                OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient()), listOf("http://test.com/wd"),
                1, Timeouts(1, 1, 1)
        )
        application.setup(config, wdSessionManager, emptyList(), "test-revision")

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
    internal fun systemInfo() = withWdipApplicationModule {
        handleRequest {
            uri = "/-/system-info"
        }.let { call ->
            Assertions.assertTrue(call.requestHandled)
            Assertions.assertEquals(HttpStatusCode.OK, call.response.status())
            val rev = call.response.headers["X-Rev"]
            Assertions.assertTrue(rev?.let { Regex("[0-9a-z]{40}").matchEntire(it) != null } == true) {
                "Value of `X-Rev` header field is not expected : $rev"
            }
            Assertions.assertEquals(
                    Headers.build {
                        set("Vary", "Origin")
                        set("X-Content-Type-Options", "nosniff")
                        set("X-Rev", assertNotNull(rev))
                        set("Content-Length", "0")
                        set("Content-Type", "text/plain; charset=UTF-8")
                    },
                    call.response.headers.allValues()
            )
            Assertions.assertEquals("", call.response.content)
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

    @Nested
    internal inner class CreateWebDriverSessionManager {
        @Test
        internal fun normal() {
            createWebDriverSessionManager(listOf("http://example.com/wd"), 1)
        }
    }

    @Nested
    internal inner class ResourcesTest {
        @Test
        internal fun readAsUtf8Text_normal() {
            val read = Resources.readAsUtf8Text("/info/vividcode/wdip/utf8-text.txt")
            Assertions.assertEquals(
                    "This is UTF-8 text file.\nこれは UTF-8 のテキストファイルです。 (Japanese)\n",
                    read
            )
        }

        @Test
        internal fun readAsUtf8Text_notExist() {
            val read = Resources.readAsUtf8Text("/not-found")
            Assertions.assertNull(read)
        }
    }

}
