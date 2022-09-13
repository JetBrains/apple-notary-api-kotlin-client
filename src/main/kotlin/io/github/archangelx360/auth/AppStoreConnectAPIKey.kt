package io.github.archangelx360.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class AppStoreConnectAPIKey(
    val issuerId: String,
    val keyId: String,
    val privateKey: String,
)

fun HttpRequestBuilder.withAppleAuthentication(credentials: AppStoreConnectAPIKey, expiration: Duration = 10.minutes) {
    val privateKey = generatePrivateKey(credentials.privateKey)
    val algorithm = Algorithm.ECDSA256(null, privateKey as ECPrivateKey)
    val token = JWT.create()
        .withKeyId(credentials.keyId)
        .withAudience("appstoreconnect-v1")
        .withIssuer(credentials.issuerId)
        .withExpiresAt(Instant.now().plusSeconds(expiration.inWholeSeconds))
        .sign(algorithm)
    bearerAuth(token)
}

private fun generatePrivateKey(privateKey: String): PrivateKey {
    val sanitizedKey = privateKey
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\n", "")

    val factory = KeyFactory.getInstance("EC")
    val decodedPrivateKey = Base64.getDecoder().decode(sanitizedKey.toByteArray())
    val keySpec = PKCS8EncodedKeySpec(decodedPrivateKey)
    return factory.generatePrivate(keySpec)
}
