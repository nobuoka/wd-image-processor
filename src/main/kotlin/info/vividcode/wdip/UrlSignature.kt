package info.vividcode.wdip

import io.ktor.http.Parameters
import io.ktor.util.flattenEntries
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SignatureBase(
    val path: String,
    val parametersWithoutSignature: List<Pair<String, String>>
)
sealed class UrlSignatureInfoResponse {
    data class UrlSignatureInfo(
        val signatureBase: SignatureBase,
        val signature: String
    ) : UrlSignatureInfoResponse()
    object InvalidUrl : UrlSignatureInfoResponse()
}

const val SignatureParameterName = "s"

/**
 * [RFC 5849 section 3.4.1.3](https://tools.ietf.org/html/rfc5849#section-3.4.1.3)
 *
 * @param url Request URL, query parameters of which are used.
 * @param requestBody Request body or null. If it is specified, it must be an "application/x-www-form-urlencoded" string.
 * @return Percent-encoded parameters.
 */
fun getUrlSignatureInfoFromUrl(url: URL): UrlSignatureInfoResponse {
    // o  The query component of the HTTP request URI as defined by
    //    [RFC3986], Section 3.4.  The query component is parsed into a list
    //    of name/value pairs by treating it as an
    //    "application/x-www-form-urlencoded" string, separating the names
    //    and values and decoding them as defined by
    //    [W3C.REC-html40-19980424], Section 17.13.4.

    val parameters = url.query?.let {
        getParametersFromWwwFormUrlEncodedString(it)
    } ?: listOf()
    val signatureParameters = parameters.filter { it.first == SignatureParameterName }

    return UrlSignatureInfoResponse.UrlSignatureInfo(SignatureBase(url.path, parameters), signatureParameters.first().second)
}

fun getUrlSignatureInfoFromUrl(path: String, queryParameters: Parameters): UrlSignatureInfoResponse {
    val parameters = queryParameters.flattenEntries()
    val signatureParameterFilter: (Pair<String, String>) -> Boolean = { it.first == SignatureParameterName }
    val signatureParameters = parameters.filter(signatureParameterFilter)
    val parametersWithoutSignature = parameters.filterNot(signatureParameterFilter)
    if (signatureParameters.size == 1) return UrlSignatureInfoResponse.InvalidUrl

    return UrlSignatureInfoResponse.UrlSignatureInfo(SignatureBase(path, parametersWithoutSignature), signatureParameters.first().second)
}

fun makeSignature(signatureBase: SignatureBase, key: String): String {
    val percentEncodedParameters =
        signatureBase.parametersWithoutSignature
            .map { Pair(PercentEncode.encode(it.first), PercentEncode.encode(it.second)) }
            .toMutableList()
    Collections.sort(percentEncodedParameters, ParamComparator)
    val canonicalParametersString = percentEncodedParameters.joinToString("&") { "${it.first}=${it.second}" }
    val signatureBaseString = signatureBase.path + "?" + canonicalParametersString

    return Signatures.makeSignatureWithHmacSha1(key, signatureBaseString)
}

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


fun getParametersFromWwwFormUrlEncodedString(s: String): List<Pair<String, String>> =
    s.split('&').dropLastWhile { it.isEmpty() }.map { pair ->
        val encKeyValue = pair.split('=', limit = 2).toTypedArray()
        val key = URLDecoder.decode(encKeyValue[0], "UTF-8")
        val value = URLDecoder.decode(if (encKeyValue.size == 1) "" else encKeyValue[1], "UTF-8")
        Pair(key, value)
    }

/**
 * パラメータ同士の比較を行うためのクラス.
 * OAuth 認証では, パラメータを並べ替える必要があり,
 * その際にこのクラスのインスタンスを使用する
 */
object ParamComparator : Comparator<Pair<String, String>> {
    override fun compare(o1: Pair<String, String>, o2: Pair<String, String>): Int =
        o1.first.compareTo(o2.first).let {
            if (it == 0) {
                o1.second.compareTo(o2.second)
            } else {
                it
            }
        }
}

/**
 * The OAuth 1.0 Protocol の仕様に合う形で文字列をパーセントエンコードする機能を提供するクラス。
 */
object PercentEncode {

    /** "0123456789ABCDEF" の ASCII バイト列  */
    private val BS = byteArrayOf(48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70)

    /** 指定のバイトをパーセントエンコードする必要があるかどうかの真理値を格納した配列
     * (インデックスがバイト値に対応. ただし最上位ビットが 1 のものは含まない)  */
    private val NEED_ENCODE = BooleanArray(0x7F + 1).also {
        for (i in it.indices) {
            // a(97)-z(122), A(65)-Z(90), 0(48)-9(57), -(45), .(46), _(95), ~(126)
            it[i] = !(i in 65..90 || i in 97..122 || i in 48..57 || i == 45 || i == 46 || i == 95 || i == 126)
        }
    }

    /**
     * The OAuth 1.0 Protocol の仕様に合う形で文字列をパーセントエンコードする.
     * パーセントエンコードの対象になるのは 'A'-'Z', 'a'-'z', '0'-'9', '-', '.', '_', '~' を除く全ての文字である.
     *
     * @param str パーセントエンコードの対象文字列
     * @return str をパーセントエンコードした文字列
     */
    @JvmStatic
    fun encode(str: String): String = encode(str.toByteArray(StandardCharsets.UTF_8))

    @JvmStatic
    fun encode(bytes: ByteArray): String =
        ByteArrayOutputStream().use { os ->
            bytes.forEach {
                val b = it.toInt()
                if (it < 0 || NEED_ENCODE[b]) {
                    // "%"
                    os.write(37)
                    // 上の 4 ビット
                    os.write(BS[b shr 4 and 0x0F].toInt())
                    // 下の 4 ビット
                    os.write(BS[b and 0x0F].toInt())
                } else {
                    os.write(b)
                }
            }
            os.toString()
        }

}
