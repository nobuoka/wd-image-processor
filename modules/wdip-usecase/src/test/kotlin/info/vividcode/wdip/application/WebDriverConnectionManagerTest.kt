package info.vividcode.wdip.application

import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import info.vividcode.wdip.test.WebDriverRemoteEndArranger
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
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

        runBlocking {
            val arrange = async {
                val testSessionId = UUID.fromString("552c06c2-bfc7-43c3-b7dd-9eda5c05a771")
                webDriverRemoteEndArranger.expectNewSessionCommand(testSessionId)
                webDriverRemoteEndArranger.expectSetTimeoutsCommand(testSessionId)
                webDriverRemoteEndArranger.expectDeleteSessionCommand(testSessionId)
            }

            val act = async {
                webDriverConnectionManager.withSession {
                    // Do nothing
                }
            }

            arrange.await()
            act.await()
        }
    }

}
