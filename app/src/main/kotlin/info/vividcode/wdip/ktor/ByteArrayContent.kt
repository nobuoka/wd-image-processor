package info.vividcode.wdip.ktor

import info.vividcode.wdip.application.WdImageProcessingResultContent
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset

class ByteArrayContent(
    override val contentType: ContentType,
    private val bytes: ByteArray
) : OutgoingContent.ByteArrayContent() {

    override val contentLength: Long get() = bytes.size.toLong()
    override fun bytes(): ByteArray = bytes

    companion object {
        fun from(content: WdImageProcessingResultContent?): ByteArrayContent = run {
            if (content == null) {
                ByteArrayContent(ContentType.Application.OctetStream, ByteArray(0))
            } else {
                val type = when (content.type) {
                    WdImageProcessingResultContent.Type.IMAGE_JPEG -> ContentType.Image.JPEG
                    WdImageProcessingResultContent.Type.IMAGE_PNG -> ContentType.Image.PNG
                    WdImageProcessingResultContent.Type.TEXT -> ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                }
                ByteArrayContent(type, content.bytes)
            }
        }
    }

}
