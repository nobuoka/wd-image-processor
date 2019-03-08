package info.vividcode.wdip.web

import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.PipelineInterceptor

class WdImageProcessingEndpoint(
    val path: String,
    val interceptors: List<PipelineInterceptor<Unit, ApplicationCall>>
)
