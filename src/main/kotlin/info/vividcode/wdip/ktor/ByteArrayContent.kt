package info.vividcode.wdip.ktor

import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType

class ByteArrayContent(
    override val contentType: ContentType,
    private val bytes: ByteArray
) : OutgoingContent.ByteArrayContent() {
    override fun bytes(): ByteArray = bytes
}
