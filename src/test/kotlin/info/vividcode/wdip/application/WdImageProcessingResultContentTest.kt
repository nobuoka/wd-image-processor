package info.vividcode.wdip.application

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class WdImageProcessingResultContentTest {

    @Test
    internal fun equals_sameValue() {
        val content = WdImageProcessingResultContent.createText("Hello, world!")

        Assertions.assertTrue(content == content)
        Assertions.assertTrue(content == WdImageProcessingResultContent.createText("Hello, world!"))
    }

    @Test
    internal fun equals_differentValue() {
        val content = WdImageProcessingResultContent.createText("Hello, world!")

        Assertions.assertFalse(content == WdImageProcessingResultContent.createText("Hello, world?"))
    }

    @Test
    internal fun hashCode_sameValue() {
        val content1 = WdImageProcessingResultContent.createText("Hello, world!")
        val content2 = WdImageProcessingResultContent.createText("Hello, world!")

        Assertions.assertTrue(content1.hashCode() == content2.hashCode())
    }

    @Test
    internal fun hashCode_differentValue() {
        val content1 = WdImageProcessingResultContent.createText("Hello, world!")
        val content2 = WdImageProcessingResultContent.createText("Hello, world?")

        Assertions.assertFalse(content1.hashCode() == content2.hashCode())
    }

}
