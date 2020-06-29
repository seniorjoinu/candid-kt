import com.squareup.kotlinpoet.CodeBlock
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import senior.joinu.candid.CanisterId
import senior.joinu.candid.EdDSAKeyPair
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.serialize.*
import senior.joinu.candid.Leb128

data class Value(
    val i: BigInteger,
    val n: BigInteger
)

object ValueValueSer : ValueSer<Value> {
    val iValueSer: ValueSer<BigInteger> = IntValueSer

    val nValueSer: ValueSer<BigInteger> = NatValueSer

    override fun calcSizeBytes(value: Value): Int = this.iValueSer.calcSizeBytes(value.i) +
            this.nValueSer.calcSizeBytes(value.n)

    override fun ser(buf: ByteBuffer, value: Value) {
        this.iValueSer.ser(buf, value.i)
        this.nValueSer.ser(buf, value.n)
    }

    override fun deser(buf: ByteBuffer): Value = Value(this.iValueSer.deser(buf),
        this.nValueSer.deser(buf))

    override fun poetize(): String = CodeBlock.of("%T", ValueValueSer::class).toString()
}

sealed class Sign {
    object Plus : Sign()

    object Minus : Sign()
}

object SignValueSer : ValueSer<Sign> {
    override fun calcSizeBytes(value: Sign): Int = when (value) {
        is Sign.Plus -> Leb128.sizeUnsigned(0)
        is Sign.Minus -> Leb128.sizeUnsigned(1)
    }

    override fun ser(buf: ByteBuffer, value: Sign) {
        when (value) {
            is Sign.Plus -> {
                Leb128.writeUnsigned(buf, 0)
            }
            is Sign.Minus -> {
                Leb128.writeUnsigned(buf, 1)
            }
        }
    }

    override fun deser(buf: ByteBuffer): Sign {
        val idx = Leb128.readUnsigned(buf)
        return when (idx) {
            0 -> Sign.Plus
            1 -> Sign.Minus
            else -> throw java.lang.RuntimeException("Unknown idx met during variant deserialization")
        }
    }

    override fun poetize(): String = CodeBlock.of("%T", SignValueSer::class).toString()
}

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

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAFsArWPk9wGccfrxNAJcQEA")
    }
}

typealias AnonFunc1ValueSer = FuncValueSer

class AnonFunc1(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    suspend operator fun invoke(arg0: Sign): Value {
        val arg0ValueSer = SignValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.query(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return ValueValueSer.deser(receiveBuf) as Value
    }

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAFrAvrWzakDf9Dg19wJfwEA")
    }
}

typealias AnonFunc2ValueSer = FuncValueSer

class AnonFunc2(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
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

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")
    }
}

class MainActor(
    host: String,
    id: CanisterId?,
    keyPair: EdDSAKeyPair?,
    apiVersion: String = "v1"
) : SimpleIDLService(host, id, keyPair, apiVersion) {
    val addMessageAndReturnChat: AnonFunc0 = AnonFunc0("addMessageAndReturnChat", this)

    val getValue: AnonFunc1 = AnonFunc1("getValue", this)

    val returnChat: AnonFunc2 = AnonFunc2("returnChat", this)
}

fun main() {
    val id = CanisterId.fromCanonical("ic:9F74DCB2E416E3710E")
    val keyPair = EdDSAKeyPair.generateInsecure()

    val actor = MainActor("http://localhost:8000", id, keyPair)

    runBlocking {
        val message = Message("Hello, chat!", "Sasha Vtyurin")
        val chat = actor.addMessageAndReturnChat(message)
        val chat1 = actor.returnChat()
        println(chat)
        println(chat1)

        val value = actor.getValue(Sign.Plus)
        println(value)
    }
}
