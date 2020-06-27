package senior.joinu.candid

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.CRC32
import java.util.zip.Checksum
import kotlin.experimental.and
import kotlin.experimental.xor
import kotlin.math.absoluteValue

open class SimpleIDLService(
    var host: String?,
    val id: CanisterId?,
    var keyPair: EdDSAKeyPair?,
    var apiVersion: String = "v1",
    var pollingInterval: Long = 100
) {
    suspend fun call(funcName: String, arg: ByteArray): ByteArray {
        val requestId = submit(funcName, IDLFuncRequestType.Call, arg)

        var status: ICStatusResponse
        loop@while (true) {
            status = requestStatus(requestId)
            when (status) {
                is ICStatusResponse.Replied -> break@loop
                is ICStatusResponse.Rejected -> throw RuntimeException("Request was rejected. code: ${status.rejectCode}, message: ${status.rejectMessage}")
                else -> {}
            }

            delay(pollingInterval)
        }

        return (status as ICStatusResponse.Replied).reply.arg
    }

    suspend fun query(funcName: String, arg: ByteArray): ByteArray {
        return read(funcName, IDLFuncRequestType.Query, arg)
    }

    suspend fun requestStatus(requestId: ByteArray): ICStatusResponse {
        val req = ICStatusRequest(
            requestId,
            SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte)
        )
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

        return ICStatusResponse.fromCBORBytes(responseBody!!)
    }

    suspend fun submit(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICCommonRequest(type, id!!.data, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte))
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

        return req.id
    }

    suspend fun read(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICCommonRequest(type, id!!.data, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte))
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
            val pubKeyHash = hash(pubKey)
            val buf = ByteBuffer.allocate(pubKeyHash.size + 1)

            buf.put(pubKeyHash)
            buf.put(2)
            val id = ByteArray(pubKeyHash.size + 1)
            buf.rewind()
            buf.get(id)

            return SimpleIDLPrincipal(id)
        }
    }
}

object Null
object Reserved
object Empty

data class CanisterId(val data: ByteArray) {
    companion object {
        fun fromCanonical(id: String): CanisterId {
            check(id.substring(0 until 3) == "ic:") { "'ic' prefix not found" }
            val idHex = id.substring(3 until id.length-2)
            val actualCRCHex = id.substring(id.length-2 until id.length)
            val expectedCRCHex = CRC8.calc(idHex.hexStringToByteArray()).toInt().absoluteValue.toString(16)
            check(actualCRCHex.toLowerCase() == expectedCRCHex.toLowerCase()) { "CRC doesn't match" }

            return CanisterId(idHex.hexStringToByteArray())
        }
    }
}

object CRC8 {
    private val table = Base64.getDecoder().decode("AAcMCRwXEhEuMywxHh0iJXB3fnlsa2JlOk84NVRTWl3g5+7p/Pvy9djf1tHEw8rNkJeemYyLgoWor6ahtLO6vcfAyc7b3NXS//jx9uPk7eq3sLm+q6yloo+IgYaTlJ2aJxohJjswKygZFA8SAwQLCFdQWV49PkU2b2hhZnN0fXqJjoeAlZKbnLG2v7itqqOk+f738OXi6+zBxs/I3drT1GluZ2B1cnt8UVZfWD88N0QVGBMOBQIJChsmJyAxOikqTjs0OVJVXFt2cXh/am1kYzIvMC0cHyQjBgEIDRYdEBOuqaCnsrW8u5aRmJ+KjYSD3tnQ18LFzMvm4ejv+v308w==")

    fun calc(data: ByteArray): Byte {
        var result = 0.toByte()
        for (byte in data) {
            result = table[(result xor byte).toInt().absoluteValue]
        }

        return result
    }
}
