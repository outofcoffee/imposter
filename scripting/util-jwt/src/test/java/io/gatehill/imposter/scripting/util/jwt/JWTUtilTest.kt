package io.gatehill.imposter.scripting.util.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class JWTUtilTest {
    @Test
    fun `decode valid token`() {
        val token = createTokenBuilder()
            .sign(Algorithm.HMAC256("secret"))

        val decoded = JWTUtil.decode(token)
        assertThat(decoded.issuer, equalTo("issuer"))
        assertThat(decoded.audience, hasItem(equalTo("audience")))
    }

    @Test
    fun `verify valid HS256 token`() {
        val token = createTokenBuilder()
            .sign(Algorithm.HMAC256("secret"))

        val verified = JWTUtil.verifyHmac256(token, "issuer", "secret")
        assertThat(verified.issuer, equalTo("issuer"))
        assertThat(verified.audience, hasItem(equalTo("audience")))
    }

    @Test
    fun `verify valid RS256 token`() {
        val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)

        val keyPair: KeyPair = keyGen.generateKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey

        val token = createTokenBuilder()
            .sign(Algorithm.RSA256(publicKey, privateKey))

        val verified = JWTUtil.verifyRsa256(token, "issuer", publicKey, privateKey)
        assertThat(verified.issuer, equalTo("issuer"))
        assertThat(verified.audience, hasItem(equalTo("audience")))
    }

    private fun createTokenBuilder() = JWT.create()
        .withAudience("audience")
        .withIssuer("issuer")
        .withIssuedAt(Date())
        .withNotBefore(Date())
        .withExpiresAt(Date.from(LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC)))
}
