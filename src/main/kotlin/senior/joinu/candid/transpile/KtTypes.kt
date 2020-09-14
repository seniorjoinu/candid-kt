package senior.joinu.candid.transpile

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import kotlinx.coroutines.delay
import org.apache.commons.codec.binary.Base32
import senior.joinu.candid.serialize.ICRequest
import senior.joinu.candid.serialize.ICStatusResponse
import senior.joinu.candid.serialize.RequestId
import senior.joinu.candid.utils.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

open class SimpleIDLService(
    var host: String?,
    val canisterId: SimpleIDLPrincipal?,
    var keyPair: EdDSAKeyPair?,
    var apiVersion: String = "v1",
    var pollingInterval: Long = 100
) {
    suspend fun call(funcName: String, arg: ByteArray): ByteArray {
        val req = ICRequest.ICCommonRequest(
            IDLFuncRequestType.Call,
            canisterId!!,
            funcName,
            arg,
            SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte)
        )
        val requestId = submit(req)

        var status: ICStatusResponse
        loop@ while (true) {
            status = requestStatus(requestId)
            when (status) {
                is ICStatusResponse.Replied -> break@loop
                is ICStatusResponse.Rejected -> throw RuntimeException("Request was rejected. code: ${status.rejectCode}, message: ${status.rejectMessage}")
                else -> {
                }
            }

            delay(pollingInterval)
        }

        return (status as ICStatusResponse.Replied).reply.arg
    }

    suspend fun query(funcName: String, arg: ByteArray): ByteArray {
        val req = ICRequest.ICCommonRequest(
            IDLFuncRequestType.Query,
            canisterId!!,
            funcName,
            arg,
            SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte)
        )
        val responseBody = read(req)
        val status = ICStatusResponse.fromCBORBytes(responseBody)

        return when (status) {
            is ICStatusResponse.Replied -> status.reply.arg
            is ICStatusResponse.Rejected -> throw RuntimeException("Request was rejected. code: ${status.rejectCode}, message: ${status.rejectMessage}")
            else -> throw RuntimeException("Unknown response for query")
        }
    }

    suspend fun requestStatus(requestId: RequestId): ICStatusResponse {
        val req = ICRequest.ICStatusRequest(requestId)
        val responseBody = read(req)

        return ICStatusResponse.fromCBORBytes(responseBody)
    }

    suspend fun submit(req: ICRequest.ICCommonRequest): RequestId {
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        val (_, error) = Fuel.post("${host!!}/api/$apiVersion/submit")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArrayResult()

        if (error != null) {
            val errorMessage = error.response.body().toByteArray().toString(StandardCharsets.ISO_8859_1)
            throw RuntimeException(errorMessage, error)
        }

        return req.requestId
    }

    suspend fun read(req: ICRequest): ByteArray {
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        val (responseBody, error) = Fuel.post("${host!!}/api/$apiVersion/read")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArrayResult()

        if (error != null) {
            val errorMessage = error.response.body().toByteArray().toString(StandardCharsets.ISO_8859_1)
            throw RuntimeException(errorMessage, error)
        }

        return responseBody!!
    }
}

enum class IDLFuncRequestType(val value: String) {
    Call("call"),
    Query("query"),
    RequestStatus("request_status")
}

open class SimpleIDLFunc(
    val funcName: String?,
    val service: SimpleIDLService?
)

open class SimpleIDLPrincipal(
    val id: ByteArray?
) {
    companion object {
        fun selfAuthenticating(pubKey: ByteArray): SimpleIDLPrincipal {
            val pubKeyHash = hash(pubKey, HashAlgorithm.SHA224)
            val buf = ByteBuffer.allocate(pubKeyHash.size + 1)

            buf.put(pubKeyHash)
            buf.put(2)
            val id = ByteArray(pubKeyHash.size + 1)
            buf.rewind()
            buf.get(id)

            return SimpleIDLPrincipal(id)
        }

        fun fromHex(hex: String): SimpleIDLPrincipal {
            return SimpleIDLPrincipal(hex.hexStringToByteArray())
        }

        fun fromText(principal: String): SimpleIDLPrincipal {
            val text = principal.toLowerCase().replace("-", "")
            val bytes = Base32().decode(text)

            return SimpleIDLPrincipal(bytes.copyOfRange(4, bytes.size))
        }
    }

    fun toText(): String? {
        if (id == null)
            return null

        val capacity = 4 + id.size
        val buf = ByteBuffer.allocate(capacity)
        val crc = CRC32()
        crc.update(id)
        buf.putInt(crc.value.toInt())
        buf.put(id)

        val arr = buf.array()
        val str = Base32().encodeToString(arr)

        return str.replace("=", "").chunked(5).joinToString("-").toLowerCase()
    }

    fun toHex(): String? = id?.toHex()
}

object Null
object Reserved
object Empty
