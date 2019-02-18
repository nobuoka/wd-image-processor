package info.vividcode.wdip.application

import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import info.vividcode.wd.pool.WebDriverConnectionManager
import info.vividcode.wdip.test.WebDriverRemoteEndArranger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.jupiter.api.Test
import java.util.*

internal class WebDriverConnectionManagerTest {

    @Rule
    @JvmField
    internal val server = MockWebServer()

    private val webDriverRemoteEndArranger = WebDriverRemoteEndArranger(server)

    @Test
    internal fun instantiation() {
        val baseUrl = server.url("").toString().dropLast(1)
        val timeouts = Timeouts(2000, 2000, 2000)
        WebDriverConnectionManager(
                OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient()),
                setOf(baseUrl), 1, timeouts
        )
    }

    @Test
    internal fun withSession() {
        val baseUrl = server.url("").toString().dropLast(1)
        val timeouts = Timeouts(2000, 2000, 2000)
        val webDriverConnectionManager = WebDriverConnectionManager(
                OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient()),
                setOf(baseUrl), 1, timeouts
        )

        val arrange = GlobalScope.async {
            val testSessionId = UUID.fromString("552c06c2-bfc7-43c3-b7dd-9eda5c05a771")
            webDriverRemoteEndArranger.expectNewSessionCommand(testSessionId)
            webDriverRemoteEndArranger.expectSetTimeoutsCommand(testSessionId)
            webDriverRemoteEndArranger.expectDeleteSessionCommand(testSessionId)
        }

        val act = GlobalScope.async {
            webDriverConnectionManager.withSession {
                // Do nothing
            }
        }

        runBlocking {
            arrange.await()
            act.await()
        }
    }

}
