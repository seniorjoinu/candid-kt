package senior.joinu.candid

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import kotlinx.coroutines.delay
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

open class SimpleIDLService(
    var host: String?,
    val id: ByteArray?,
    var keyPair: EdDSAKeyPair?,
    var apiVersion: String = "v1"
) {
    suspend fun call(funcName: String, arg: ByteArray): ByteArray {
        val requestId = submit(funcName, IDLFuncRequestType.Call, arg)

        val result = requestStatus(requestId)
        delay(1000)
        val result1 = requestStatus(requestId)
        delay(1000)
        val result2 = requestStatus(requestId)
        delay(1000)
        val result3 = requestStatus(requestId)

        return requestId
    }

    suspend fun query(funcName: String, arg: ByteArray): ByteArray {
        return read(funcName, IDLFuncRequestType.Query, arg)
    }

    suspend fun requestStatus(requestId: ByteArray): ByteArray {
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

        ICStatusResponse.fromCBORBytes(responseBody!!)

        return responseBody
    }

    suspend fun submit(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICCommonRequest(type, id!!, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte))
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
        val req = ICCommonRequest(type, id!!, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte))
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
