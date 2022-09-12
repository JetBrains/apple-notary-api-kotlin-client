package io.github.archangelx360.auth

import com.philjay.jwt.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class AppStoreConnectAPIKey(
    val issuerId: String,
    val keyId: String,
    val privateKey: String,
)

fun HttpRequestBuilder.withAppleAuthentication(credentials: AppStoreConnectAPIKey) {
    val token = JWT.tokenApple(
        credentials.issuerId,
        credentials.keyId,
        credentials.privateKey,
        jsonEncoder,
        encoder,
        decoder
    )
    bearerAuth(token)
}

private val jsonEncoder = object : JsonEncoder<AppleJWTAuthHeader, JWTAuthPayload> {
    override fun toJson(header: AppleJWTAuthHeader): String {
        return Json.encodeToString(header)
    }

    override fun toJson(payload: JWTAuthPayload): String {
        return Json.encodeToString(payload)
    }
}

private val encoder = object : Base64Encoder {
    override fun encodeURLSafe(bytes: ByteArray): String {
        return Base64.getUrlEncoder().encodeToString(bytes)
    }

    override fun encode(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }
}

private val decoder = object : Base64Decoder {
    override fun decode(bytes: ByteArray): ByteArray {
        return Base64.getDecoder().decode(bytes)
    }

    override fun decode(string: String): ByteArray {
        return Base64.getDecoder().decode(string)
    }
}
