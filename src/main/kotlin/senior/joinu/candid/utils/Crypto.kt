package senior.joinu.candid.utils

import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Signature


val EDDSA_SPEC: EdDSAParameterSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

data class EdDSAKeyPair(val pub: EdDSAPublicKey, val priv: EdDSAPrivateKey) {
    companion object {
        fun generateInsecure(): EdDSAKeyPair {
            val seed = randomBytes(32)
            return fromSeed(seed)
        }

        fun fromSeed(seed: ByteArray): EdDSAKeyPair {
            val privSpec = EdDSAPrivateKeySpec(seed, EDDSA_SPEC)
            val priv = EdDSAPrivateKey(privSpec)
            val pubSpec = EdDSAPublicKeySpec(priv.a, EDDSA_SPEC)
            val pub = EdDSAPublicKey(pubSpec)

            return EdDSAKeyPair(pub, priv)
        }
    }
}

fun signInsecure(privKey: EdDSAPrivateKey, message: ByteArray): ByteArray {
    val sgr: Signature = EdDSAEngine(MessageDigest.getInstance(EDDSA_SPEC.hashAlgorithm))
    sgr.initSign(privKey)
    sgr.update(message)

    return sgr.sign()
}

fun verify(pubKey: EdDSAPublicKey, message: ByteArray, sig: ByteArray): Boolean {
    val sgr: Signature = EdDSAEngine(MessageDigest.getInstance(EDDSA_SPEC.hashAlgorithm))
    sgr.initVerify(pubKey)
    sgr.update(message)

    return sgr.verify(sig)
}

enum class HashAlgorithm(val text: String) {
    SHA256("SHA-256"),
    SHA224("SHA-224")
}

fun hash(value: String, algorithm: HashAlgorithm = HashAlgorithm.SHA256) =
    hash(value.toByteArray(StandardCharsets.UTF_8), algorithm)

fun hash(value: ByteArray, algorithm: HashAlgorithm = HashAlgorithm.SHA256): ByteArray {
    val digest = MessageDigest.getInstance(algorithm.text)

    return digest.digest(value)
}
