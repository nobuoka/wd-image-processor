package info.vividcode.wdip.ktor

import info.vividcode.wdip.Signatures
import info.vividcode.wdip.WdImageProcessingExecutor
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.pipeline.PipelineInterceptor
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
        call.respond(
            ByteArrayContent(
                ContentType.Image.PNG,
                wdImageProcessingExecutor.execute(html, js, arg)
            )
        )
    }
}