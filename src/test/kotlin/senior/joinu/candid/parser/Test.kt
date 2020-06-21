import com.squareup.kotlinpoet.CodeBlock
import senior.joinu.candid.Null
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.TypeTable
import senior.joinu.candid.serialize.*
import senior.joinu.leb128.Leb128
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

typealias my_type = Byte

val my_typeValueSer: ValueSer<Byte> = Nat8ValueSer

data class List(
    val head: BigInteger,
    val tail: List?
)

object ListValueSer : ValueSer<List> {
    val headValueSer: ValueSer<BigInteger> = IntValueSer

    val tailValueSer: ValueSer<List?> = OptValueSer(ListValueSer)

    override fun calcSizeBytes(value: List): Int = this.headValueSer.calcSizeBytes(value.head) +
            this.tailValueSer.calcSizeBytes(value.tail)

    override fun ser(buf: ByteBuffer, value: List) {
        this.headValueSer.ser(buf, value.head)
        this.tailValueSer.ser(buf, value.tail)
    }

    override fun deser(buf: ByteBuffer): List = List(
        this.headValueSer.deser(buf),
        this.tailValueSer.deser(buf)
    )

    override fun poetize(): String = CodeBlock.of("%T", ListValueSer::class).toString()
}

typealias fValueSer = FuncValueSer

data class fResult(
    val `0`: List?
)

typealias AnonFunc0ValueSer = FuncValueSer

data class AnonFunc0Result(
    val `0`: Long
)

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABdQ==")

    suspend operator fun invoke(arg0: Int): AnonFunc0Result {
        val arg0ValueSer = senior.joinu.candid.serialize.Int32ValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc0Result(senior.joinu.candid.serialize.Int64ValueSer.deser(receiveBuf))
    }
}

class f(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray =
        Base64.getDecoder().decode("RElETAJsAqDSrKgEfJDt2ucEAW4AAgBqAXUBdAA=")

    suspend operator fun invoke(arg0: List, arg1: AnonFunc0): fResult {
        val arg0ValueSer = ListValueSer
        val arg1ValueSer = senior.joinu.candid.serialize.FuncValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return fResult(senior.joinu.candid.serialize.OptValueSer(ListValueSer).deser(receiveBuf))
    }
}

typealias brokerValueSer = ServiceValueSer

typealias AnonFunc1ValueSer = FuncValueSer

typealias AnonIDLType0ValueSer = ServiceValueSer

typealias AnonFunc2ValueSer = FuncValueSer

data class AnonFunc2Result(
    val `0`: Int
)

class AnonFunc2(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke(): AnonFunc2Result {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc2Result(senior.joinu.candid.serialize.Nat32ValueSer.deser(receiveBuf))
    }
}

typealias AnonFunc3ValueSer = FuncValueSer

class AnonFunc3Result

class AnonFunc3(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")

    suspend operator fun invoke(): AnonFunc3Result {
        val valueSizeBytes = 0
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc3Result()
    }
}

class AnonIDLType0(
    host: String,
    id: ByteArray?
) : SimpleIDLService(host, id) {
    val current: AnonFunc2 = AnonFunc2("current", this)

    val up: AnonFunc3 = AnonFunc3("up", this)
}

data class AnonFunc1Result(
    val `0`: AnonIDLType0
)

class AnonFunc1(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")

    suspend operator fun invoke(arg0: String): AnonFunc1Result {
        val arg0ValueSer = senior.joinu.candid.serialize.TextValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc1Result(
            senior.joinu.candid.serialize.ServiceValueSer.deser(receiveBuf) as
                    AnonIDLType0
        )
    }
}

class broker(
    host: String,
    id: ByteArray?
) : SimpleIDLService(host, id) {
    val find: AnonFunc1 = AnonFunc1("find", this)
}

data class AnonIDLType1(
    val `0`: BigInteger,
    val `2`: Byte,
    val `0x2a`: BigInteger
)

object AnonIDLType1ValueSer : ValueSer<AnonIDLType1> {
    val `0ValueSer`: ValueSer<BigInteger> = NatValueSer

    val `2ValueSer`: ValueSer<Byte> = Nat8ValueSer

    val `0x2aValueSer`: ValueSer<BigInteger> = NatValueSer

    override fun calcSizeBytes(value: AnonIDLType1): Int = this.`0ValueSer`.calcSizeBytes(value.`0`) +
            this.`2ValueSer`.calcSizeBytes(value.`2`) + this.`0x2aValueSer`.calcSizeBytes(value.`0x2a`)

    override fun ser(buf: ByteBuffer, value: AnonIDLType1) {
        this.`0ValueSer`.ser(buf, value.`0`)
        this.`2ValueSer`.ser(buf, value.`2`)
        this.`0x2aValueSer`.ser(buf, value.`0x2a`)
    }

    override fun deser(buf: ByteBuffer): AnonIDLType1 = AnonIDLType1(
        this.`0ValueSer`.deser(buf),
        this.`2ValueSer`.deser(buf), this.`0x2aValueSer`.deser(buf)
    )

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType1ValueSer::class).toString()
}

sealed class AnonIDLType2 {
    data class `0x2a`(
        val value: Null
    ) : AnonIDLType2()

    data class A(
        val value: Null
    ) : AnonIDLType2()

    data class B(
        val value: Null
    ) : AnonIDLType2()

    data class C(
        val value: Null
    ) : AnonIDLType2()
}

object AnonIDLType2ValueSer : ValueSer<AnonIDLType2> {
    override fun calcSizeBytes(value: AnonIDLType2): Int = when (value) {
        is AnonIDLType2.`0x2a` -> senior.joinu.leb128.Leb128.sizeUnsigned(42) +
                senior.joinu.candid.serialize.NullValueSer.calcSizeBytes(value.value)
        is AnonIDLType2.A -> senior.joinu.leb128.Leb128.sizeUnsigned(65) +
                senior.joinu.candid.serialize.NullValueSer.calcSizeBytes(value.value)
        is AnonIDLType2.B -> senior.joinu.leb128.Leb128.sizeUnsigned(66) +
                senior.joinu.candid.serialize.NullValueSer.calcSizeBytes(value.value)
        is AnonIDLType2.C -> senior.joinu.leb128.Leb128.sizeUnsigned(67) +
                senior.joinu.candid.serialize.NullValueSer.calcSizeBytes(value.value)
    }

    override fun ser(buf: ByteBuffer, value: AnonIDLType2) {
        when (value) {
            is AnonIDLType2.`0x2a` -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 42)
                senior.joinu.candid.serialize.NullValueSer.ser(buf, value.value)
            }
            is AnonIDLType2.A -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 65)
                senior.joinu.candid.serialize.NullValueSer.ser(buf, value.value)
            }
            is AnonIDLType2.B -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 66)
                senior.joinu.candid.serialize.NullValueSer.ser(buf, value.value)
            }
            is AnonIDLType2.C -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 67)
                senior.joinu.candid.serialize.NullValueSer.ser(buf, value.value)
            }
        }
    }

    override fun deser(buf: ByteBuffer): AnonIDLType2 {
        val idx = Leb128.readUnsigned(buf)
        return when (idx) {
            42 -> AnonIDLType2.`0x2a`(senior.joinu.candid.serialize.NullValueSer.deser(buf))
            65 -> AnonIDLType2.A(senior.joinu.candid.serialize.NullValueSer.deser(buf))
            66 -> AnonIDLType2.B(senior.joinu.candid.serialize.NullValueSer.deser(buf))
            67 -> AnonIDLType2.C(senior.joinu.candid.serialize.NullValueSer.deser(buf))
            else -> throw java.lang.RuntimeException("Unknown idx met during variant deserialization")
        }
    }

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType2ValueSer::class).toString()
}

data class nested(
    val `0`: BigInteger,
    val `1`: BigInteger,
    val `2`: AnonIDLType1,
    val `5`: AnonIDLType2,
    val `40`: BigInteger,
    val `42`: BigInteger
)

object nestedValueSer : ValueSer<nested> {
    val `0ValueSer`: ValueSer<BigInteger> = NatValueSer

    val `1ValueSer`: ValueSer<BigInteger> = NatValueSer

    val `2ValueSer`: ValueSer<AnonIDLType1> = AnonIDLType1ValueSer

    val `5ValueSer`: ValueSer<AnonIDLType2> = AnonIDLType2ValueSer

    val `40ValueSer`: ValueSer<BigInteger> = NatValueSer

    val `42ValueSer`: ValueSer<BigInteger> = NatValueSer

    override fun calcSizeBytes(value: nested): Int = this.`0ValueSer`.calcSizeBytes(value.`0`) +
            this.`1ValueSer`.calcSizeBytes(value.`1`) + this.`2ValueSer`.calcSizeBytes(value.`2`) +
            this.`5ValueSer`.calcSizeBytes(value.`5`) + this.`40ValueSer`.calcSizeBytes(value.`40`) +
            this.`42ValueSer`.calcSizeBytes(value.`42`)

    override fun ser(buf: ByteBuffer, value: nested) {
        this.`0ValueSer`.ser(buf, value.`0`)
        this.`1ValueSer`.ser(buf, value.`1`)
        this.`2ValueSer`.ser(buf, value.`2`)
        this.`5ValueSer`.ser(buf, value.`5`)
        this.`40ValueSer`.ser(buf, value.`40`)
        this.`42ValueSer`.ser(buf, value.`42`)
    }

    override fun deser(buf: ByteBuffer): nested = nested(
        this.`0ValueSer`.deser(buf),
        this.`1ValueSer`.deser(buf), this.`2ValueSer`.deser(buf), this.`5ValueSer`.deser(buf),
        this.`40ValueSer`.deser(buf), this.`42ValueSer`.deser(buf)
    )

    override fun poetize(): String = CodeBlock.of("%T", nestedValueSer::class).toString()
}

typealias serverValueSer = ServiceValueSer

typealias AnonFunc4ValueSer = FuncValueSer

class AnonFunc4Result

class AnonFunc4(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAACbXtufg==")

    suspend operator fun invoke(arg0: ByteArray, arg1: Boolean?): AnonFunc4Result {
        val arg0ValueSer = senior.joinu.candid.serialize.BlobValueSer
        val arg1ValueSer =
            senior.joinu.candid.serialize.OptValueSer(senior.joinu.candid.serialize.BoolValueSer)
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc4Result()
    }
}

typealias AnonFunc5ValueSer = FuncValueSer

data class AnonFunc5Result(
    val `0`: BigInteger
)

class AnonFunc5(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAN7bAKg0qyoBHyQ7drnBAJuAQMAAW4B")

    suspend operator fun invoke(
        arg0: my_type,
        arg1: List,
        arg2: List?
    ): AnonFunc5Result {
        val arg0ValueSer = my_typeValueSer
        val arg1ValueSer = ListValueSer
        val arg2ValueSer = senior.joinu.candid.serialize.OptValueSer(ListValueSer)
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1) +
                arg2ValueSer.calcSizeBytes(arg2)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        arg2ValueSer.ser(sendBuf, arg2)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc5Result(senior.joinu.candid.serialize.IntValueSer.deser(receiveBuf))
    }
}

typealias AnonFunc6ValueSer = FuncValueSer

class AnonIDLType4

object AnonIDLType4ValueSer : ValueSer<AnonIDLType4> {
    override fun calcSizeBytes(value: AnonIDLType4): Int = 0

    override fun ser(buf: ByteBuffer, value: AnonIDLType4) {
    }

    override fun deser(buf: ByteBuffer): AnonIDLType4 = AnonIDLType4()

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType4ValueSer::class).toString()
}

data class AnonIDLType3(
    val `0x2a`: AnonIDLType4,
    val id: BigInteger
)

object AnonIDLType3ValueSer : ValueSer<AnonIDLType3> {
    val `0x2aValueSer`: ValueSer<AnonIDLType4> = AnonIDLType4ValueSer

    val idValueSer: ValueSer<BigInteger> = NatValueSer

    override fun calcSizeBytes(value: AnonIDLType3): Int =
        this.`0x2aValueSer`.calcSizeBytes(value.`0x2a`) + this.idValueSer.calcSizeBytes(value.id)

    override fun ser(buf: ByteBuffer, value: AnonIDLType3) {
        this.`0x2aValueSer`.ser(buf, value.`0x2a`)
        this.idValueSer.ser(buf, value.id)
    }

    override fun deser(buf: ByteBuffer): AnonIDLType3 = AnonIDLType3(
        this.`0x2aValueSer`.deser(buf),
        this.idValueSer.deser(buf)
    )

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType3ValueSer::class).toString()
}

data class AnonFunc6Result(
    val `0`: AnonIDLType3
)

sealed class AnonIDLType5 {
    data class A(
        val value: BigInteger
    ) : AnonIDLType5()

    data class B(
        val value: String?
    ) : AnonIDLType5()
}

object AnonIDLType5ValueSer : ValueSer<AnonIDLType5> {
    override fun calcSizeBytes(value: AnonIDLType5): Int = when (value) {
        is AnonIDLType5.A -> senior.joinu.leb128.Leb128.sizeUnsigned(65) +
                senior.joinu.candid.serialize.NatValueSer.calcSizeBytes(value.value)
        is AnonIDLType5.B -> senior.joinu.leb128.Leb128.sizeUnsigned(66) +
                senior.joinu.candid.serialize.OptValueSer(senior.joinu.candid.serialize.TextValueSer)
                    .calcSizeBytes(value.value)
    }

    override fun ser(buf: ByteBuffer, value: AnonIDLType5) {
        when (value) {
            is AnonIDLType5.A -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 65)
                senior.joinu.candid.serialize.NatValueSer.ser(buf, value.value)
            }
            is AnonIDLType5.B -> {
                senior.joinu.leb128.Leb128.writeUnsigned(buf, 66)

                senior.joinu.candid.serialize.OptValueSer(senior.joinu.candid.serialize.TextValueSer).ser(
                    buf,
                    value.value
                )
            }
        }
    }

    override fun deser(buf: ByteBuffer): AnonIDLType5 {
        val idx = Leb128.readUnsigned(buf)
        return when (idx) {
            65 -> AnonIDLType5.A(senior.joinu.candid.serialize.NatValueSer.deser(buf))
            66 ->
                AnonIDLType5.B(
                    senior.joinu.candid.serialize.OptValueSer(senior.joinu.candid.serialize.TextValueSer).deser(buf)
                )
            else -> throw java.lang.RuntimeException("Unknown idx met during variant deserialization")
        }
    }

    override fun poetize(): String = CodeBlock.of("%T", AnonIDLType5ValueSer::class).toString()
}

class AnonFunc6(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    val staticPayload: ByteArray =
        Base64.getDecoder().decode("RElETANucWwCoNKsqAR8kO3a5wQCbgEDbQBrAkF9QgBuAQ==")

    suspend operator fun invoke(
        arg0: kotlin.collections.List<String?>,
        arg1: AnonIDLType5,
        arg2: List?
    ): AnonFunc6Result {
        val arg0ValueSer =
            senior.joinu.candid.serialize.VecValueSer(senior.joinu.candid.serialize.OptValueSer(senior.joinu.candid.serialize.TextValueSer))
        val arg1ValueSer = AnonIDLType5ValueSer
        val arg2ValueSer = senior.joinu.candid.serialize.OptValueSer(ListValueSer)
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1) +
                arg2ValueSer.calcSizeBytes(arg2)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        arg2ValueSer.ser(sendBuf, arg2)
        val receiveBuf = ByteBuffer.allocate(0)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        val responseTypeTable = TypeTable()
        return AnonFunc6Result(AnonIDLType3ValueSer.deser(receiveBuf))
    }
}

class server(
    host: String,
    id: ByteArray?
) : SimpleIDLService(host, id) {
    val f: AnonFunc4 = AnonFunc4("f", this)

    val g: AnonFunc5 = AnonFunc5("g", this)

    val h: AnonFunc6 = AnonFunc6("h", this)

    val i: f = f("i", this)
}

