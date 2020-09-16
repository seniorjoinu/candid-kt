package senior.joinu.candid.serialize

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.builder.MapBuilder
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.Map
import co.nstant.`in`.cbor.model.Number
import co.nstant.`in`.cbor.model.UnicodeString
import senior.joinu.candid.transpile.IDLFuncRequestType
import senior.joinu.candid.transpile.SimpleIDLPrincipal
import senior.joinu.candid.utils.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets


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

        val builder = CborBuilder()
            .addMap()

        content.toCBOR("content", builder)
        val items = builder
            .put("sender_pubkey", senderPubKey)
            .put("sender_sig", senderSig)
            .end()
            .build()

        encoder.encode(items)
        os.flush()

        return os.out!!
    }
}

data class RequestId(private val data: ByteArray) {
    companion object {
        fun fromHashedFields(hashedFields: List<Pair<ByteArray, ByteArray>>): RequestId {
            val sorted = hashedFields.sortedWith { (k1, v1), (k2, v2) ->
                var result = 0
                for (i in 0..k1.size) {
                    result = k1[i].toUByte().compareTo(k2[i].toUByte())
                    if (result != 0) break
                }

                result
            }
            val concatenatedSize = sorted.map { it.first.size + it.second.size }.sum()
            val concatenatedBuf = ByteBuffer.allocate(concatenatedSize)

            sorted.forEach {
                concatenatedBuf.put(it.first)
                concatenatedBuf.put(it.second)
            }
            concatenatedBuf.rewind()
            val concatenated = ByteArray(concatenatedSize)
            concatenatedBuf.get(concatenated)

            return RequestId(hash(concatenated))
        }
    }

    val prefixed: ByteArray
        get() = "\nic-request".toByteArray(StandardCharsets.UTF_8) + data

    val plain: ByteArray
        get() = data

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RequestId

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

data class ICReply(val arg: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ICReply

        if (!arg.contentEquals(other.arg)) return false

        return true
    }

    override fun hashCode(): Int {
        return arg.contentHashCode()
    }

    companion object {
        fun fromCBORMap(replyMap: Map): ICReply {
            val arg = replyMap[UnicodeString("arg")] as ByteString
            return ICReply(arg.bytes)
        }
    }
}

sealed class ICRequest {
    abstract fun toCBOR(name: String, builder: MapBuilder<CborBuilder>)
    abstract fun authenticate(keyPair: EdDSAKeyPair): AuthenticatedICRequest

    data class ICCommonRequest(
        val requestType: IDLFuncRequestType,
        val canisterId: SimpleIDLPrincipal,
        val methodName: String,
        val arg: ByteArray,
        val sender: SimpleIDLPrincipal,
        val nonce: ByteArray = randomBytes(13)
    ) : ICRequest() {
        val requestId: RequestId by lazy {
            val traversed = listOf(
                Pair(hash("arg"), hash(arg)),
                Pair(hash("canister_id"), hash(canisterId.id!!)),
                Pair(hash("method_name"), hash(methodName)),
                Pair(hash("nonce"), hash(nonce)),
                Pair(hash("request_type"), hash(requestType.value)),
                Pair(hash("sender"), hash(sender.id!!))
            )

            RequestId.fromHashedFields(traversed)
        }

        override fun authenticate(keyPair: EdDSAKeyPair): AuthenticatedICRequest {
            val sign = signInsecure(keyPair.priv, requestId.prefixed)

            return AuthenticatedICRequest(this, keyPair.pub.abyte, sign)
        }

        override fun toCBOR(name: String, builder: MapBuilder<CborBuilder>) {
            builder.putMap(name)
                .put("arg", arg)
                .put("canister_id", canisterId.id!!)
                .put("method_name", methodName)
                .put("nonce", nonce)
                .put("request_type", requestType.value)
                .put("sender", sender.id!!)
                .end()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ICCommonRequest

            if (requestType != other.requestType) return false
            if (canisterId != other.canisterId) return false
            if (methodName != other.methodName) return false
            if (!arg.contentEquals(other.arg)) return false
            if (sender != other.sender) return false
            if (!nonce.contentEquals(other.nonce)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = requestType.hashCode()
            result = 31 * result + canisterId.hashCode()
            result = 31 * result + methodName.hashCode()
            result = 31 * result + arg.contentHashCode()
            result = 31 * result + sender.hashCode()
            result = 31 * result + nonce.contentHashCode()
            return result
        }
    }

    data class ICStatusRequest(
        val requestId: RequestId,
        val requestType: IDLFuncRequestType = IDLFuncRequestType.RequestStatus
    ) : ICRequest() {
        override fun authenticate(keyPair: EdDSAKeyPair): AuthenticatedICRequest {
            val sign = signInsecure(keyPair.priv, requestId.prefixed)

            return AuthenticatedICRequest(this, keyPair.pub.abyte, sign)
        }

        override fun toCBOR(name: String, builder: MapBuilder<CborBuilder>) {
            builder.putMap(name)
                .put("request_id", requestId.plain)
                .put("request_type", requestType.value)
                .end()
        }
    }
}


sealed class ICStatusResponse {
    object Unknown : ICStatusResponse()
    data class Replied(val reply: ICReply) : ICStatusResponse()
    data class Rejected(val rejectCode: Int, val rejectMessage: String) : ICStatusResponse()

    companion object {
        fun fromCBORBytes(encodedBytes: ByteArray): ICStatusResponse {
            val bais = ByteArrayInputStream(encodedBytes.sliceArray(3 until encodedBytes.size))
            val dataItems = CborDecoder(bais).decode()

            check(dataItems[0].majorType.name == "MAP") { "ICReply should contain map" }
            val responseMap = dataItems[0] as Map
            val status = responseMap[UnicodeString("status")] as UnicodeString

            return when (status.toString()) {
                "unknown" -> Unknown
                "replied" -> {
                    val reply = responseMap[UnicodeString("reply")] as Map
                    Replied(
                        ICReply.fromCBORMap(reply)
                    )
                }
                "rejected" -> {
                    val rejectCode = responseMap[UnicodeString("reject_code")] as Number
                    val rejectMessage = responseMap[UnicodeString("reject_message")] as UnicodeString

                    Rejected(rejectCode.value.toInt(), rejectMessage.string)
                }
                else -> throw RuntimeException("Unknown ICReply type")
            }
        }
    }
}
