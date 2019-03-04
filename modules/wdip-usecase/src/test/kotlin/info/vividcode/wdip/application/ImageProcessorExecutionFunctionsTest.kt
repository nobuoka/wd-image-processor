package info.vividcode.wdip.application

import info.vividcode.test.utils.MockWebServerResourceExtension
import info.vividcode.wd.Timeouts
import info.vividcode.wd.http.implementation.OkHttpWebDriverCommandHttpRequestDispatcher
import info.vividcode.wd.pool.WebDriverConnectionManager
import info.vividcode.wd.test.server.WebDriverRemoteEndArranger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.*
import javax.json.JsonValue

internal class ImageProcessorExecutionFunctionsTest {

    @JvmField
    @RegisterExtension
    internal val mockWebServerResourceExtension = MockWebServerResourceExtension()

    private val webDriverRemoteEndArranger = WebDriverRemoteEndArranger(mockWebServerResourceExtension.mockWebServer)

    @Test
    internal fun executeImageProcessorWithElementScreenshot() {
        val baseUrl = mockWebServerResourceExtension.mockWebServerUrl
        val timeouts = Timeouts(2000, 2000, 2000)
        val webDriverConnectionManager = WebDriverConnectionManager(
                OkHttpWebDriverCommandHttpRequestDispatcher.Factory(OkHttpClient()),
                setOf(baseUrl), 1, timeouts
        )

        val testScreenshotByteArray = javaClass.getResourceAsStream(testPngImageResourcePath).use { it.readBytes() }

        val arrange = GlobalScope.async {
            val testSessionId = UUID.fromString("552c06c2-bfc7-43c3-b7dd-9eda5c05a771")
            val testWebElementReference = UUID.fromString("98c76e56-6077-47ae-ba9a-effd392ce780")
            webDriverRemoteEndArranger.expectNewSessionCommand(testSessionId)
            webDriverRemoteEndArranger.expectSetTimeoutsCommand(testSessionId)
            webDriverRemoteEndArranger.expectGoCommand(testSessionId)
            webDriverRemoteEndArranger.expectExecuteAsyncScriptCommand(testSessionId, JsonValue.EMPTY_JSON_OBJECT)
            webDriverRemoteEndArranger.expectFindElement(testSessionId, testWebElementReference)
            webDriverRemoteEndArranger.expectTakeElementScreenshot(
                    testSessionId, testWebElementReference, testScreenshotByteArray
            )
            webDriverRemoteEndArranger.expectDeleteSessionCommand(testSessionId)
        }

        val act = GlobalScope.async {
            webDriverConnectionManager.withSession { session ->
                executeImageProcessorWithElementScreenshot(
                        session,
                        "<!DOCTYPE html><html><head><title>test</title></head><body></body></html>",
                        "return {};",
                        ""
                )
            }
        }

        val result = runBlocking {
            arrange.await()
            act.await()
        }

        Assertions.assertEquals(200, result.statusCode)
        Assertions.assertNull(result.httpCache)
        Assertions.assertEquals(WdImageProcessingResultContent.createPng(testScreenshotByteArray), result.content)
    }

    companion object {
        private const val testPngImageResourcePath = "/info/vividcode/wdip/application/test-image.png"
    }

}
