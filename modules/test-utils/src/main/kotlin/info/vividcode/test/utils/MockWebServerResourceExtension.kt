package info.vividcode.test.utils

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MockWebServerResourceExtension : BeforeEachCallback, AfterEachCallback {

    val mockWebServer = MockWebServer()
    val mockWebServerUrl: String
        get() = mockWebServer.url("").toString().let {
            if (it.last() == '/') it.dropLast(1)
            else it
        }

    override fun beforeEach(context: ExtensionContext) {
        mockWebServer.start()
    }

    override fun afterEach(context: ExtensionContext) {
        mockWebServer.shutdown()
    }

}
