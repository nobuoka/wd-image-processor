package info.vividcode.wdip.web

import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.PipelineInterceptor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class WdImageProcessingEndpointTest {

    @Test
    internal fun instantiation() {
        val endpoint = WdImageProcessingEndpoint("test", listOf<PipelineInterceptor<Unit, ApplicationCall>>({}, {}))

        Assertions.assertEquals("test", endpoint.path)
        Assertions.assertEquals(2, endpoint.interceptors.size)
    }

}
