import kotlinx.coroutines.runBlocking
import senior.joinu.candid.serialize.FuncValueSer
import senior.joinu.candid.serialize.ServiceValueSer
import senior.joinu.candid.serialize.TypeDeser
import senior.joinu.candid.transpile.SimpleIDLFunc
import senior.joinu.candid.transpile.SimpleIDLPrincipal
import senior.joinu.candid.transpile.SimpleIDLService
import senior.joinu.candid.utils.EdDSAKeyPair
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

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
        val sendBytes = sendBuf.array()

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.wrap(receiveBytes)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.TextValueSer.deser(receiveBuf)
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
        val host = "http://localhost:8000"
        val canisterId = SimpleIDLPrincipal.fromText("75hes-oqbaa-aaaaa-aaaaa-aaaaa-aaaaa-aaaaa-q")
        val keyPair = EdDSAKeyPair.generateInsecure()

        val helloWorld = MainActor(host, canisterId, keyPair)

        val response = helloWorld.greet("World")
        println(response) // Hello, World!
    }
}
