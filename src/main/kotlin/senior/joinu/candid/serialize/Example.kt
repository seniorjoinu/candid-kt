package senior.joinu.candid.serialize

import com.squareup.kotlinpoet.CodeBlock
import senior.joinu.candid.Null
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.transpile.deserUntilM
import senior.joinu.candid.transpile.receive
import senior.joinu.leb128.Leb128
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Record(val field1: Int, val field2: String, val field3: Record?)

object RecordValueSer : ValueSer<Record> {
    val field1ValueSer = Int32ValueSer
    val field2ValueSer = TextValueSer
    val field3ValueSer = OptValueSer(RecordValueSer)

    override fun calcSizeBytes(value: Record): Int {
        return field1ValueSer.calcSizeBytes(value.field1) +
                field2ValueSer.calcSizeBytes(value.field2) +
                field3ValueSer.calcSizeBytes(value.field3)
    }

    override fun ser(buf: ByteBuffer, value: Record) {
        field1ValueSer.ser(buf, value.field1)
        field2ValueSer.ser(buf, value.field2)
        field3ValueSer.ser(buf, value.field3)
    }

    override fun deser(buf: ByteBuffer): Record {
        return Record(
            field1ValueSer.deser(buf),
            field2ValueSer.deser(buf),
            field3ValueSer.deser(buf)
        )
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", RecordValueSer::class).toString()
    }
}

sealed class VariantSuper {
    data class VariantA(val value: Int) : VariantSuper()

    data class VariantB(val value: Record) : VariantSuper()

    data class VariantC(val value: Null = Null) : VariantSuper()
}

object VariantSuperValueSer : ValueSer<VariantSuper> {
    override fun calcSizeBytes(value: VariantSuper): Int {
        return when (value) {
            is VariantSuper.VariantA -> Leb128.sizeUnsigned(0) + Int32ValueSer.calcSizeBytes(value.value)
            is VariantSuper.VariantB -> Leb128.sizeUnsigned(1) + RecordValueSer.calcSizeBytes(value.value)
            is VariantSuper.VariantC -> Leb128.sizeUnsigned(2) + NullValueSer.calcSizeBytes(value.value)
        }
    }

    override fun ser(buf: ByteBuffer, value: VariantSuper) {
        when (value) {
            is VariantSuper.VariantA -> {
                Leb128.writeUnsigned(buf, 0)
                Int32ValueSer.ser(buf, value.value)
            }
            is VariantSuper.VariantB -> {
                Leb128.writeUnsigned(buf, 1)
                RecordValueSer.ser(buf, value.value)
            }
            is VariantSuper.VariantC -> {
                Leb128.writeUnsigned(buf, 2)
                NullValueSer.ser(buf, value.value)
            }
        }
    }

    override fun deser(buf: ByteBuffer): VariantSuper {
        val idx = Leb128.readUnsigned(buf)

        return when (idx) {
            0 -> VariantSuper.VariantA(Int32ValueSer.deser(buf))
            1 -> VariantSuper.VariantB(RecordValueSer.deser(buf))
            2 -> VariantSuper.VariantC(NullValueSer.deser(buf))
            else -> throw RuntimeException("Unknown idx met during VariantSuper deserialization $idx")
        }
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", VariantSuperValueSer::class).toString()
    }
}

data class FunResult(val `0`: Boolean, val `1`: Record, val `2`: VariantSuper)

typealias FunValueSer = FuncValueSer

class Fun(funcName: String?, service: SimpleIDLService?) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = byteArrayOf()

    suspend operator fun invoke(arg0: Int, arg1: Record, arg2: VariantSuper): FunResult {
        val arg0ValueSer = Int32ValueSer
        val arg1ValueSer = RecordValueSer
        val arg2ValueSer = VariantSuperValueSer

        val valueSizeBytes = arg0ValueSer.calcSizeBytes(arg0) +
                arg1ValueSer.calcSizeBytes(arg1) +
                arg2ValueSer.calcSizeBytes(arg2)

        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)

        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        arg2ValueSer.ser(sendBuf, arg2)

        // TODO: cbor encode and send

        val receiveBuf = receive()
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = deserUntilM(receiveBuf)

        return FunResult(
            BoolValueSer.deser(receiveBuf),
            RecordValueSer.deser(receiveBuf),
            VariantSuperValueSer.deser(receiveBuf)
        )
    }
}

typealias ExampleServiceValueSer = ServiceValueSer

class ExampleService(host: String, id: ByteArray?) : SimpleIDLService(host, id) {
    val f = Fun("f", this)
}

// TODO: design func, service and principal then implement transpilers for them