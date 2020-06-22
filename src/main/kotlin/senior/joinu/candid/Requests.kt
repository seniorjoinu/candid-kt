package senior.joinu.candid

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.builder.MapBuilder
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import org.whispersystems.curve25519.SecureRandomProvider
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*


class InsecureRandomProvider : SecureRandomProvider {
    private val r = Random()

    override fun nextBytes(output: ByteArray?) {
        r.nextBytes(output)
    }

    override fun nextInt(maxValue: Int): Int {
        return r.nextInt(maxValue)
    }
}

fun main() {
}

fun getCipher() = Curve25519.getInstance(Curve25519.BEST, InsecureRandomProvider())

fun hash(value: String): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")

    return digest.digest(value.toByteArray(StandardCharsets.UTF_8))
}

fun hash(value: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")

    return digest.digest(value)
}

data class AuthenticatedICRequest(
    val content: ICRequest,
    val senderPubKey: ByteArray,
    val senderSig: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthenticatedICRequest

        if (content != other.content) return false
        if (!senderPubKey.contentEquals(other.senderPubKey)) return false
        if (!senderSig.contentEquals(other.senderSig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + senderPubKey.contentHashCode()
        result = 31 * result + senderSig.contentHashCode()
        return result
    }

    fun cbor(): ByteArray {
        val os = ByteBufferBackedOutputStream()
        val encoder = CborEncoder(os).nonCanonical()

        val builder = CborBuilder().addMap()
        content.cbor("content", builder)
        val items = builder
            .put("sender_pub_key", senderPubKey)
            .put("sender_sig", senderSig)
            .end()
            .build()

        encoder.encode(items)
        os.flush()

        return os.out!!
    }
}

data class ICRequest(
    val requestType: IDLFuncRequestType,
    val canisterId: ByteArray,
    val methodName: String,
    val arg: ByteArray,
    val sender: SimpleIDLPrincipal
) {
    val id: ByteArray by lazy {
        val traversed = listOf(
            Pair(hash("request_type"), hash(requestType.value)),
            Pair(hash("canister_id"), hash(canisterId)),
            Pair(hash("method_name"), hash(methodName)),
            Pair(hash("arg"), hash(arg)),
            Pair(hash("sender"), hash(sender.id!!))
        )

        val sorted = traversed.sortedWith(kotlin.Comparator { o1, o2 ->
            var result = 0
            for (i in 0..o1.first.size) {
                result = o1.first[i].compareTo(o2.first[i])
                if (result != 0) break
            }

            result
        })
        val concatenatedSize = sorted.map { it.first.size + it.second.size }.sum()
        val concatenatedBuf = ByteBuffer.allocate(concatenatedSize)
        concatenatedBuf.order(ByteOrder.LITTLE_ENDIAN)
        sorted.forEach {
            concatenatedBuf.put(it.first)
            concatenatedBuf.put(it.second)
        }
        concatenatedBuf.rewind()
        val concatenated = ByteArray(concatenatedSize)
        concatenatedBuf.get(concatenated)

        hash(concatenated)
    }

    fun authenticate(keyPair: Curve25519KeyPair): AuthenticatedICRequest {
        val cipher = getCipher()
        val sign = cipher.calculateSignature(keyPair.privateKey, id)

        return AuthenticatedICRequest(this, keyPair.publicKey, sign)
    }

    fun cbor(name: String, builder: MapBuilder<CborBuilder>) {
        builder.putMap(name)
            .put("request_type", requestType.value)
            .put("canister_id", canisterId)
            .put("method_name", methodName)
            .put("arg", arg)
            .put("sender", sender.id!!)
            .end()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ICRequest

        if (requestType != other.requestType) return false
        if (canisterId != other.canisterId) return false
        if (methodName != other.methodName) return false
        if (!arg.contentEquals(other.arg)) return false
        if (sender != other.sender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = requestType.hashCode()
        result = 31 * result + canisterId.hashCode()
        result = 31 * result + methodName.hashCode()
        result = 31 * result + arg.contentHashCode()
        result = 31 * result + sender.hashCode()
        return result
    }
}

class ByteBufferBackedOutputStream : OutputStream() {
    private val buffer = mutableListOf<Int>()
    var out: ByteArray? = null

    override fun flush() {
        val buf = ByteBuffer.allocate(buffer.size)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        buffer.forEach { buf.put(it.toByte()) }
        buffer.clear()
        buf.rewind()

        out = ByteArray(buffer.size)
        buf.get(out)
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        buffer.add(b)
    }
}