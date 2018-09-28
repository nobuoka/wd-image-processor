package info.vividcode.wdip.ktor.features

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.features.CORS
import io.ktor.request.header
import io.ktor.response.header
import io.ktor.util.AttributeKey

/**
 * This feature provides a mechanism that checks `Origin` header and respond necessary headers.
 *
 * Headers added to response are following:
 *
 * * `Access-Control-Allow-Origin` header : added if the `Origin` header value of request is contained in
 *   [Configuration.accessControlAllowOrigins], or not added otherwise.
 * * `Vary: Origin` header
 *
 * This feature may be able to be replaced with [CORS] feature.
 */
class OriginAccessControl(private val accessControlAllowOrigins: Set<String>) {

    /**
     * Configuration of [OriginAccessControl] feature.
     */
    class Configuration {
        val accessControlAllowOrigins = mutableSetOf<String>()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, OriginAccessControl> {
        override val key = AttributeKey<OriginAccessControl>("OriginAccessControl")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): OriginAccessControl {
            val configuration = Configuration().apply(configure)
            val originHeaders = OriginAccessControl(configuration.accessControlAllowOrigins)

            pipeline.intercept(ApplicationCallPipeline.Call) {
                call.response.header("Vary", "Origin")
                call.request.header("Origin")?.let { origin ->
                    if (originHeaders.accessControlAllowOrigins.contains(origin)) {
                        call.response.header("Access-Control-Allow-Origin", origin)
                    }
                }
            }

            return originHeaders
        }
    }

}
