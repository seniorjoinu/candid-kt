package senior.joinu.candid

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArray
import org.whispersystems.curve25519.Curve25519KeyPair
import java.nio.ByteBuffer

open class SimpleIDLService(
    var host: String?,
    val id: ByteArray?,
    var keyPair: Curve25519KeyPair?,
    var apiVersion: String = "v1"
) {
    suspend fun call(funcName: String, arg: ByteArray): ByteArray {
        return submit(funcName, IDLFuncRequestType.Call, arg)
    }

    suspend fun query(funcName: String, arg: ByteArray): ByteArray {
        return read(funcName, IDLFuncRequestType.Query, arg)
    }

    suspend fun submit(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICRequest(type, id!!, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.publicKey))
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        return Fuel.post("${host!!}/api/$apiVersion/submit")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArray()
    }

    suspend fun read(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICRequest(type, id!!, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.publicKey))
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        return Fuel.post("${host!!}/api/$apiVersion/read")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArray()
    }
}

enum class IDLFuncRequestType(val value: String) {
    Call("call"),
    Query("query")
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