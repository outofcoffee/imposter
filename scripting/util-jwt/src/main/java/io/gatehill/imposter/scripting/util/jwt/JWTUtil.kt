package io.gatehill.imposter.scripting.util.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

object JWTUtil {
    @JvmStatic
    fun decode(token: String): DecodedJWT {
        return JWT().decodeJwt(token)
    }

    @JvmStatic
    fun verifyHmac256(token: String, issuer: String, secret: String): DecodedJWT {
        val algorithm: Algorithm = Algorithm.HMAC256(secret)
        val verifier = JWT.require(algorithm).withIssuer(issuer).build()

        return verifier.verify(token)
    }

    @JvmStatic
    fun verifyRsa256(token: String, issuer: String, publicKey: RSAPublicKey, privateKey: RSAPrivateKey): DecodedJWT {
        val algorithm: Algorithm = Algorithm.RSA256(publicKey, privateKey)
        val verifier = JWT.require(algorithm).withIssuer(issuer).build()

        return verifier.verify(token)
    }
}
