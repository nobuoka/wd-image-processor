package info.vividcode.wdip

import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Signatures {

    private val US_ASCII = StandardCharsets.US_ASCII

    fun makeSignatureWithHmacSha1(key: String, text: String): String {
        // Every implementation of the Java platform is required to support HmacSHA1.
        val algorithmName = "HmacSHA1"
        val mac: Mac = try {
            Mac.getInstance(algorithmName)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

        val k = SecretKeySpec(key.toByteArray(US_ASCII), algorithmName)
        try {
            mac.init(k)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        }

        val digest = mac.doFinal(text.toByteArray(US_ASCII))
        return Base64.getEncoder().encodeToString(digest)
    }

}
