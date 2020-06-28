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
import senior.joinu.candid.serialize.FuncValueSer
import senior.joinu.candid.serialize.ServiceValueSer
import senior.joinu.candid.serialize.TypeDeser
import senior.joinu.candid.serialize.ValueSer
import senior.joinu.leb128.Leb128

sealed class Version_3 {
    data class Version(
        val value: BigInteger
    ) : Version_3()
}

object Version_3ValueSer : ValueSer<Version_3> {
    override fun calcSizeBytes(value: Version_3): Int = when (value) {
        is Version_3.Version -> senior.joinu.leb128.Leb128.sizeUnsigned(1245908728) +
                senior.joinu.candid.serialize.NatValueSer.calcSizeBytes(value.value)
    }

    override fun ser(buf: ByteBuffer, value: Version_3) {
        when (value) {
            is Version_3.Version -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 1245908728)
                senior.joinu.candid.serialize.NatValueSer.ser(buf, value.value)
            }
        }
    }

    override fun deser(buf: ByteBuffer): Version_3 {
        val idx = Leb128.readUnsigned(buf).toInt()
        return when (idx) {
            1245908728 -> Version_3.Version(senior.joinu.candid.serialize.NatValueSer.deser(buf))
            else -> throw java.lang.RuntimeException("Unknown idx met during variant deserialization")
        }
    }

    override fun poetize(): String = CodeBlock.of("%T", Version_3ValueSer::class).toString()
}

typealias Version_2 = Version_3

val Version_2ValueSer: ValueSer<Version_3> = Version_3ValueSer

typealias Version = Version_2

val VersionValueSer: ValueSer<Version_2> = Version_2ValueSer

sealed class Mode_3 {
    object EightBit : Mode_3()

    object Alphanumeric : Mode_3()

    object Kanji : Mode_3()

    object Numeric : Mode_3()
}

object Mode_3ValueSer : ValueSer<Mode_3> {
    override fun calcSizeBytes(value: Mode_3): Int = when (value) {
        is Mode_3.EightBit -> senior.joinu.leb128.Leb128.sizeUnsigned(-701225250)
        is Mode_3.Alphanumeric -> senior.joinu.leb128.Leb128.sizeUnsigned(972377935)
        is Mode_3.Kanji -> senior.joinu.leb128.Leb128.sizeUnsigned(1870596279)
        is Mode_3.Numeric -> senior.joinu.leb128.Leb128.sizeUnsigned(2031225517)
    }

    override fun ser(buf: ByteBuffer, value: Mode_3) {
        when (value) {
            is Mode_3.EightBit -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, -701225250)
            }
            is Mode_3.Alphanumeric -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 972377935)
            }
            is Mode_3.Kanji -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 1870596279)
            }
            is Mode_3.Numeric -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 2031225517)
            }
        }
    }

    override fun deser(buf: ByteBuffer): Mode_3 {
        val idx = Leb128.readUnsigned(buf).toInt()
        return when (idx) {
            -701225250 -> Mode_3.EightBit
            972377935 -> Mode_3.Alphanumeric
            1870596279 -> Mode_3.Kanji
            2031225517 -> Mode_3.Numeric
            else -> throw java.lang.RuntimeException("Unknown idx met during variant deserialization")
        }
    }

    override fun poetize(): String = CodeBlock.of("%T", Mode_3ValueSer::class).toString()
}

typealias Mode_2 = Mode_3

val Mode_2ValueSer: ValueSer<Mode_3> = Mode_3ValueSer

typealias Mode = Mode_2

val ModeValueSer: ValueSer<Mode_2> = Mode_2ValueSer

sealed class ErrorCorrection_3 {
    object H : ErrorCorrection_3()

    object L : ErrorCorrection_3()

    object M : ErrorCorrection_3()

    object Q : ErrorCorrection_3()
}

object ErrorCorrection_3ValueSer : ValueSer<ErrorCorrection_3> {
    override fun calcSizeBytes(value: ErrorCorrection_3): Int = when (value) {
        is ErrorCorrection_3.H -> senior.joinu.leb128.Leb128.sizeUnsigned(72)
        is ErrorCorrection_3.L -> senior.joinu.leb128.Leb128.sizeUnsigned(76)
        is ErrorCorrection_3.M -> senior.joinu.leb128.Leb128.sizeUnsigned(77)
        is ErrorCorrection_3.Q -> senior.joinu.leb128.Leb128.sizeUnsigned(81)
    }

    override fun ser(buf: ByteBuffer, value: ErrorCorrection_3) {
        when (value) {
            is ErrorCorrection_3.H -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 72)
            }
            is ErrorCorrection_3.L -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 76)
            }
            is ErrorCorrection_3.M -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 77)
            }
            is ErrorCorrection_3.Q -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 81)
            }
        }
    }

    override fun deser(buf: ByteBuffer): ErrorCorrection_3 {
        val idx = Leb128.readUnsigned(buf).toInt()
        return when (idx) {
            72 -> ErrorCorrection_3.H
            76 -> ErrorCorrection_3.L
            77 -> ErrorCorrection_3.M
            81 -> ErrorCorrection_3.Q
            else -> throw java.lang.RuntimeException("Unknown idx met during variant deserialization")
        }
    }

    override fun poetize(): String = CodeBlock.of("%T", ErrorCorrection_3ValueSer::class).toString()
}

typealias ErrorCorrection_2 = ErrorCorrection_3

val ErrorCorrection_2ValueSer: ValueSer<ErrorCorrection_3> = ErrorCorrection_3ValueSer

typealias ErrorCorrection = ErrorCorrection_2

val ErrorCorrectionValueSer: ValueSer<ErrorCorrection_2> = ErrorCorrection_2ValueSer

typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray =
        Base64.getDecoder().decode("RElETAkBAmsB+J2M0gR9BAVrBEh/TH9Nf1F/BwhrBN7N0LENf8+e1c8Df7eR/PsGf62VyMgHfwQAAwZx")

    suspend operator fun invoke(
        arg0: Version,
        arg1: ErrorCorrection,
        arg2: Mode,
        arg3: String
    ): String {
        val arg0ValueSer = VersionValueSer
        val arg1ValueSer = ErrorCorrectionValueSer
        val arg2ValueSer = ModeValueSer
        val arg3ValueSer = senior.joinu.candid.serialize.TextValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1) +
                arg2ValueSer.calcSizeBytes(arg2) + arg3ValueSer.calcSizeBytes(arg3)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        arg2ValueSer.ser(sendBuf, arg2)
        arg3ValueSer.ser(sendBuf, arg3)
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
    val encode: AnonFunc0 = AnonFunc0("encode", this)
}

fun main() {
    val id = CanisterId.fromCanonical("ic:F5ECD58694FEA0AFD3")
    val keyPair = EdDSAKeyPair.generateInsecure()

    val actor = MainActor("http://localhost:8000", id, keyPair)

    runBlocking {
        val resp = actor.encode(
            Version_3.Version(BigInteger.ONE),
            ErrorCorrection_3.H,
            Mode_3.Alphanumeric,
            "Hello, candid-kt!"
        )

        println(resp)
    }
}
