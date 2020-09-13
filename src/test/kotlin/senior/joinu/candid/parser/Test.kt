import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.ByteArray
import kotlin.String
import senior.joinu.candid.EdDSAKeyPair
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLPrincipal
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.serialize.FuncValueSer
import senior.joinu.candid.serialize.ServiceValueSer
import senior.joinu.candid.serialize.TypeDeser

typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    suspend operator fun invoke(arg0: String): String {
        val arg0ValueSer = senior.joinu.candid.serialize.TextValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.TextValueSer.deser(receiveBuf) as kotlin.String
    }

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")
    }
}

class MainActor(
    host: String,
    canisterId: SimpleIDLPrincipal?,
    keyPair: EdDSAKeyPair?,
    apiVersion: String = "v1"
) : SimpleIDLService(host, canisterId, keyPair, apiVersion) {
    val greet: AnonFunc0 = AnonFunc0("greet", this)
}

fun main() {
    runBlocking {
        val id = SimpleIDLPrincipal.fromText("7kncf-oidaa-aaaaa-aaaaa-aaaaa-aaaaa-aaaaa-q")
        val keyPair = EdDSAKeyPair.generateInsecure()
        val host = "http://localhost:8000"

        val actor = MainActor(host, id, keyPair)

        val response = actor.greet("joinu")
        println(response)
    }
}
