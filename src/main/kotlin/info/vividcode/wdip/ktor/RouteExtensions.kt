package info.vividcode.wdip.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpMethod
import io.ktor.pipeline.ContextDsl
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.head
import io.ktor.routing.route

@ContextDsl
fun Route.getAndHead(path: String, body: PipelineInterceptor<Unit, ApplicationCall>) {
    get(path, body)
    head(path, body)
}

@ContextDsl
fun Route.routeGetAndHead(path: String, build: Route.() -> Unit) {
    route(path, HttpMethod.Get, build)
    route(path, HttpMethod.Head, build)
}
