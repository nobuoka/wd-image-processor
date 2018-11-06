package info.vividcode.wdip.application

import java.util.*

data class WdImageProcessingResultContent(
        val bytes: ByteArray,
        val type: Type
) {

    companion object {
        fun createJpeg(jpegBytes: ByteArray) = WdImageProcessingResultContent(jpegBytes, Type.IMAGE_JPEG)
        fun createPng(pngBytes: ByteArray) = WdImageProcessingResultContent(pngBytes, Type.IMAGE_PNG)
        fun createText(text: String) = WdImageProcessingResultContent(text.toByteArray(Charsets.UTF_8), Type.TEXT)
    }

    enum class Type {
        IMAGE_JPEG, IMAGE_PNG, TEXT
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WdImageProcessingResultContent

        if (!Arrays.equals(bytes, other.bytes)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(bytes)
        result = 31 * result + type.hashCode()
        return result
    }

}
