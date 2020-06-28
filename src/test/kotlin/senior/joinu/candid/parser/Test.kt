import com.squareup.kotlinpoet.CodeBlock
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.Boolean
import kotlin.Byte
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import senior.joinu.candid.CanisterId
import senior.joinu.candid.EdDSAKeyPair
import senior.joinu.candid.Null
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.serialize.*

data class AnonIDLType0(
    val `0`: String,
    val `1`: List_2
)

object AnonIDLType0ValueSer : ValueSer<AnonIDLType0> {
    val `0ValueSer`: ValueSer<String> = TextValueSer

    val `1ValueSer`: ValueSer<List_2> = List_2ValueSer

    override fun calcSizeBytes(value: AnonIDLType0): Int = this.`0ValueSer`.calcSizeBytes(value.`0`) +
            this.`1ValueSer`.calcSizeBytes(value.`1`)

    override fun ser(buf: ByteBuffer, value: AnonIDLType0) {
        this.`0ValueSer`.ser(buf, value.`0`)
        this.`1ValueSer`.ser(buf, value.`1`)
    }

    override fun deser(buf: ByteBuffer): AnonIDLType0 = AnonIDLType0(this.`0ValueSer`.deser(buf),
        this.`1ValueSer`.deser(buf))

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType0ValueSer::class).toString()
}

typealias List_2 = AnonIDLType0?

val List_2ValueSer: ValueSer<AnonIDLType0?> = OptValueSer( AnonIDLType0ValueSer )

data class AnonIDLType1(
    val `0`: Key,
    val `1`: List
)

object AnonIDLType1ValueSer : ValueSer<AnonIDLType1> {
    val `0ValueSer`: ValueSer<Key> = KeyValueSer

    val `1ValueSer`: ValueSer<List> = ListValueSer

    override fun calcSizeBytes(value: AnonIDLType1): Int = this.`0ValueSer`.calcSizeBytes(value.`0`) +
            this.`1ValueSer`.calcSizeBytes(value.`1`)

    override fun ser(buf: ByteBuffer, value: AnonIDLType1) {
        this.`0ValueSer`.ser(buf, value.`0`)
        this.`1ValueSer`.ser(buf, value.`1`)
    }

    override fun deser(buf: ByteBuffer): AnonIDLType1 = AnonIDLType1(this.`0ValueSer`.deser(buf),
        this.`1ValueSer`.deser(buf))

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType1ValueSer::class).toString()
}

typealias List = AnonIDLType1?

val ListValueSer: ValueSer<AnonIDLType1?> = OptValueSer( AnonIDLType1ValueSer )

data class Key(
    val preimage: ByteArray,
    val image: ByteArray
)

object KeyValueSer : ValueSer<Key> {
    val preimageValueSer: ValueSer<ByteArray> = BlobValueSer

    val imageValueSer: ValueSer<ByteArray> = BlobValueSer

    override fun calcSizeBytes(value: Key): Int =
        this.preimageValueSer.calcSizeBytes(value.preimage) +
                this.imageValueSer.calcSizeBytes(value.image)

    override fun ser(buf: ByteBuffer, value: Key) {
        this.preimageValueSer.ser(buf, value.preimage)
        this.imageValueSer.ser(buf, value.image)
    }

    override fun deser(buf: ByteBuffer): Key = Key(this.preimageValueSer.deser(buf),
        this.imageValueSer.deser(buf))

    override fun poetize(): String = CodeBlock.of("%T", KeyValueSer::class).toString()
}

typealias Bucket = List

val BucketValueSer: ValueSer<List> = ListValueSer

typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")

    suspend operator fun invoke(arg0: String) {
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
    }
}

typealias AnonFunc1ValueSer = FuncValueSer

class AnonFunc1(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABbXs=")

    suspend operator fun invoke(arg0: ByteArray): ByteArray? {
        val arg0ValueSer = senior.joinu.candid.serialize.BlobValueSer
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
        return senior.joinu.candid.serialize.OptValueSer( senior.joinu.candid.serialize.BlobValueSer
        ).deser(receiveBuf) as kotlin.ByteArray?
    }
}

typealias AnonFunc2ValueSer = FuncValueSer

class AnonFunc2(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")

    suspend operator fun invoke(arg0: String): String? {
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
        return senior.joinu.candid.serialize.OptValueSer( senior.joinu.candid.serialize.TextValueSer
        ).deser(receiveBuf) as kotlin.String?
    }
}

typealias AnonFunc3ValueSer = FuncValueSer

class AnonFunc3(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray =
        Base64.getDecoder().decode("RElETAUBbgJsAgADAQFsAtjA1dAKBNu+pOsLBG17Am17AA==")

    suspend operator fun invoke(arg0: ByteArray, arg1: Bucket): ByteArray? {
        val arg0ValueSer = senior.joinu.candid.serialize.BlobValueSer
        val arg1ValueSer = BucketValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.OptValueSer( senior.joinu.candid.serialize.BlobValueSer
        ).deser(receiveBuf) as kotlin.ByteArray?
    }
}

typealias AnonFunc4ValueSer = FuncValueSer

class AnonFunc4(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke() {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
    }
}

typealias AnonFunc5ValueSer = FuncValueSer

class AnonFunc5(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke(): List_2 {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return List_2ValueSer.deser(receiveBuf) as List_2
    }
}

typealias AnonFunc6ValueSer = FuncValueSer

class AnonFunc6(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke() {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
    }
}

typealias AnonFunc7ValueSer = FuncValueSer

class AnonFunc7(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAACbXttew==")

    suspend operator fun invoke(arg0: ByteArray, arg1: ByteArray): Boolean {
        val arg0ValueSer = senior.joinu.candid.serialize.BlobValueSer
        val arg1ValueSer = senior.joinu.candid.serialize.BlobValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.BoolValueSer.deser(receiveBuf) as kotlin.Boolean
    }
}

typealias AnonFunc8ValueSer = FuncValueSer

class AnonFunc8(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAACcXE=")

    suspend operator fun invoke(arg0: String, arg1: String): Boolean {
        val arg0ValueSer = senior.joinu.candid.serialize.TextValueSer
        val arg1ValueSer = senior.joinu.candid.serialize.TextValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.BoolValueSer.deser(receiveBuf) as kotlin.Boolean
    }
}

typealias AnonFunc9ValueSer = FuncValueSer

class AnonFunc9(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray =
        Base64.getDecoder().decode("RElETAUBbgJsAgADAQFsAtjA1dAKBNu+pOsLBG17A217bXsA")

    suspend operator fun invoke(
        arg0: ByteArray,
        arg1: ByteArray,
        arg2: Bucket
    ): Boolean {
        val arg0ValueSer = senior.joinu.candid.serialize.BlobValueSer
        val arg1ValueSer = senior.joinu.candid.serialize.BlobValueSer
        val arg2ValueSer = BucketValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1) +
                arg2ValueSer.calcSizeBytes(arg2)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        arg2ValueSer.ser(sendBuf, arg2)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.BoolValueSer.deser(receiveBuf) as kotlin.Boolean
    }
}

typealias AnonFunc10ValueSer = FuncValueSer

class AnonFunc10(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke(): BigInteger {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
        sendBuf.rewind()
        sendBuf.get(sendBytes)

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.put(receiveBytes)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.NatValueSer.deser(receiveBuf) as java.math.BigInteger
    }
}

typealias AnonFunc11ValueSer = FuncValueSer

class AnonFunc11(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke(): String {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
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
}

class MainActor(
    host: String,
    id: CanisterId?,
    keyPair: EdDSAKeyPair?,
    apiVersion: String = "v1"
) : SimpleIDLService(host, id, keyPair, apiVersion) {
    val configure: AnonFunc0 = AnonFunc0("configure", this)

    val get: AnonFunc1 = AnonFunc1("get", this)

    val getInHex: AnonFunc2 = AnonFunc2("getInHex", this)

    val getWithTrace: AnonFunc3 = AnonFunc3("getWithTrace", this)

    val initialize: AnonFunc4 = AnonFunc4("initialize", this)

    val peers: AnonFunc5 = AnonFunc5("peers", this)

    val ping: AnonFunc6 = AnonFunc6("ping", this)

    val put: AnonFunc7 = AnonFunc7("put", this)

    val putInHex: AnonFunc8 = AnonFunc8("putInHex", this)

    val putWithTrace: AnonFunc9 = AnonFunc9("putWithTrace", this)

    val size: AnonFunc10 = AnonFunc10("size", this)

    val whoami: AnonFunc11 = AnonFunc11("whoami", this)
}

fun main() {
    val id = CanisterId.fromCanonical("ic:BFA73B5586EFB42FA3")
    val keyPair = EdDSAKeyPair.generateInsecure()

    val actor = MainActor("http://localhost:8000", id, keyPair)

    runBlocking {
        val resp = actor.peers()

        println(resp)
    }
}
