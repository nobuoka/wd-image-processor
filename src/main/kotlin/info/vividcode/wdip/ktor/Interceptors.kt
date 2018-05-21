package info.vividcode.wdip.ktor

import info.vividcode.wdip.Signatures
import info.vividcode.wdip.WdImageProcessingExecutor
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.header
import io.ktor.response.respond

interface PipelineInterceptorBase {
    suspend fun PipelineContext<Unit, ApplicationCall>.intercept()
    fun toPipelineInterceptor(): PipelineInterceptor<Unit, ApplicationCall> = { intercept() }
}

class SignatureVerifyingInterceptor(private val key: String) : PipelineInterceptorBase {
    override suspend fun PipelineContext<Unit, ApplicationCall>.intercept() {
        val argParameter = call.request.queryParameters["arg"]
        val signatureParameter = call.request.queryParameters["signature"]

        val expectedSignature = Signatures.makeSignatureWithHmacSha1(key, argParameter ?: "")
        if (expectedSignature != signatureParameter) {
            call.respond(HttpStatusCode.BadRequest, "Bad Signature")
            finish()
        } else {
            proceed()
        }
    }
}

class WdImageProcessingInterceptor(
    private val wdImageProcessingExecutor: WdImageProcessingExecutor,
    private val html: String,
    private val js: String
) : PipelineInterceptorBase {
    override suspend fun PipelineContext<Unit, ApplicationCall>.intercept() {
        val argParameter = call.request.queryParameters["arg"]
        val arg = argParameter ?: "null"

        val result = wdImageProcessingExecutor.execute(html, js, arg)

        result.httpCache?.let { httpCache ->
            httpCache.maxAge?.let { call.response.header("Cache-Control", "public, max-age=$it") }
        }

        call.respond(HttpStatusCode.fromValue(result.statusCode),
            result.content ?: ByteArrayContent(ContentType.Application.OctetStream, ByteArray(0)))
    }
}
