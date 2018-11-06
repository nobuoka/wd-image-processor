package info.vividcode.wdip.application

import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

internal class WebDriverConnectionManagerTest {

    @Test
    internal fun instantiation() {
        val urlNotInService = "http://localhost:10000"
        val timeouts = Timeouts(2000, 2000, 2000)
        WebDriverConnectionManager(
                OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient()),
                setOf(urlNotInService), 1, timeouts
        )
    }

}
