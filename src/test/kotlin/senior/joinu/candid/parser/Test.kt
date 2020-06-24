import kotlinx.coroutines.runBlocking
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import senior.joinu.candid.*
import senior.joinu.candid.serialize.FuncValueSer
import senior.joinu.candid.serialize.ServiceValueSer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.*

typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

data class AnonFunc0Result(
    val `0`: String
)

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")

    suspend operator fun invoke(arg0: String): AnonFunc0Result {
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
        val responseTypeTable = TypeTable()
        return AnonFunc0Result(senior.joinu.candid.serialize.TextValueSer.deser(receiveBuf))
    }
}

class MainActor(
    host: String,
    id: ByteArray?,
    keyPair: Curve25519KeyPair?
) : SimpleIDLService(host, id, keyPair) {
    val greet: AnonFunc0 = AnonFunc0("greet", this)
}

fun main() {
    val id = "e599c5a96f29a019".hexStringToByteArray()
    val cipher = getCipher()
    val keyPair = cipher.generateKeyPair()

    val actor = MainActor("http://localhost:8000", id, keyPair)

    runBlocking {
        actor.greet("CANDID-KT")
    }
}
