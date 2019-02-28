package info.vividcode.wdip

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class SignaturesTest {

    @Test
    internal fun normal() {
        val signature = Signatures.makeSignatureWithHmacSha1("0Ad9", "Hello, 世界 %23")

        Assertions.assertEquals("I0u5Blzge+znXKUVCzJrmiuewpY=", signature)
    }

    @Test
    internal fun normal_emptyString() {
        val signature = Signatures.makeSignatureWithHmacSha1("0", "")

        Assertions.assertEquals("AyGblCXM9L6n85BKZ29NM9kWsWE=", signature)
    }

    @Test
    internal fun error_emptyKey() {
        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            Signatures.makeSignatureWithHmacSha1("", "")
        }

        Assertions.assertEquals("Empty key", exception.message)
    }

}
