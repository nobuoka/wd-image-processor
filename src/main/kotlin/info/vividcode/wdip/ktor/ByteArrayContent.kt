package info.vividcode.wdip.ktor

import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType

class ByteArrayContent(
    override val contentType: ContentType,
    private val bytes: ByteArray
) : OutgoingContent.ByteArrayContent() {
    override val contentLength: Long get() = bytes.size.toLong()
    override fun bytes(): ByteArray = bytes
}
