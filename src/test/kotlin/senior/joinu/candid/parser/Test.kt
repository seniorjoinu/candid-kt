import com.squareup.kotlinpoet.CodeBlock
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import senior.joinu.candid.CanisterId
import senior.joinu.candid.EdDSAKeyPair
import senior.joinu.candid.Null
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.serialize.*
import senior.joinu.leb128.Leb128

data class Message(
    val sender: String,
    val message: String
)

object MessageValueSer : ValueSer<Message> {
    val senderValueSer: ValueSer<String> = TextValueSer

    val messageValueSer: ValueSer<String> = TextValueSer

    override fun calcSizeBytes(value: Message): Int =
        this.senderValueSer.calcSizeBytes(value.sender) +
                this.messageValueSer.calcSizeBytes(value.message)

    override fun ser(buf: ByteBuffer, value: Message) {
        this.senderValueSer.ser(buf, value.sender)
        this.messageValueSer.ser(buf, value.message)
    }

    override fun deser(buf: ByteBuffer): Message = Message(this.senderValueSer.deser(buf),
        this.messageValueSer.deser(buf))

    override fun poetize(): String = CodeBlock.of("%T", MessageValueSer::class).toString()
}

typealias Chat = List<Message>

val ChatValueSer: ValueSer<List<Message>> = VecValueSer( MessageValueSer )

typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAFsArWPk9wGccfrxNAJcQEA")

    suspend operator fun invoke(arg0: Message): Chat {
        val arg0ValueSer = MessageValueSer
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
        return ChatValueSer.deser(receiveBuf) as Chat
    }
}

typealias AnonFunc1ValueSer = FuncValueSer

class AnonFunc1(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke(): Chat {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.query(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return ChatValueSer.deser(receiveBuf) as Chat
    }
}

class MainActor(
    host: String,
    id: CanisterId?,
    keyPair: EdDSAKeyPair?,
    apiVersion: String = "v1"
) : SimpleIDLService(host, id, keyPair, apiVersion) {
    val addMessageAndReturnChat: AnonFunc0 = AnonFunc0("addMessageAndReturnChat", this)

    val returnChat: AnonFunc1 = AnonFunc1("returnChat", this)
}

fun main() {
    val id = CanisterId.fromCanonical("ic:859EEAD289A52D15B9")
    val keyPair = EdDSAKeyPair.generateInsecure()

    val actor = MainActor("http://localhost:8000", id, keyPair)

    runBlocking {
        val message = Message("Hello, chat!", "Sasha Vtyurin")
        val chat = actor.addMessageAndReturnChat(message)

        println(chat)
    }
}
