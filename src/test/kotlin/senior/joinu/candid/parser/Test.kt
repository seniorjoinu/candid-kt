import senior.joinu.candid.*
import senior.joinu.leb128.Leb128
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

typealias my_type = UByte

data class List(
    val head: BigInteger,
    val tail: List?
) : IDLSerializable {
    val headType: IDLType = senior.joinu.candid.IDLType.Primitive.Integer

    val tailType: IDLType =
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))

    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.KtSerilializer.serializeIDLValue(headType, head, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(tailType, tail, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int =  +
    senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(headType, head, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(tailType, tail, typeTable)
}

class AnonFunc0(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val arg0Type: IDLType = senior.joinu.candid.IDLType.Primitive.Int32

    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry = mutableListOf(), labels =
    mutableMapOf())

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAA=")

    suspend operator fun invoke(arg0: Int): Long {
        val mSize =  + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg0Type, arg0,
            typeTable)
        val buf = ByteBuffer.allocate(mSize)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
    }
}

class f(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val arg0Type: IDLType = senior.joinu.candid.IDLType.Id("List")

    val arg1Type: IDLType = senior.joinu.candid.IDLType.Reference.Func(arguments =
    listOf(senior.joinu.candid.IDLArgType("null", senior.joinu.candid.IDLType.Primitive.Int32)),
        results = listOf(senior.joinu.candid.IDLArgType("null",
            senior.joinu.candid.IDLType.Primitive.Int64)), annotations = listOf())

    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry =
    mutableListOf(senior.joinu.candid.IDLType.Constructive.Record(fields =
    listOf(senior.joinu.candid.IDLFieldType("head",
        senior.joinu.candid.IDLType.Primitive.Integer), senior.joinu.candid.IDLFieldType("tail",
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))))),
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))), labels
    = mutableMapOf(senior.joinu.candid.IDLType.Id("List") to 0))

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAJsAqDSrKgEfJDt2ucEAW4A")

    suspend operator fun invoke(arg0: List, arg1: AnonFunc0): List? {
        val mSize =  + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg0Type, arg0,
            typeTable) + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg1Type, arg1,
            typeTable)
        val buf = ByteBuffer.allocate(mSize)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
    }
}

class AnonFunc2(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry = mutableListOf(), labels =
    mutableMapOf())

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAA=")

    suspend operator fun invoke() {
        val mSize = 0
        val buf = ByteBuffer.allocate(mSize)
    }
}

class AnonFunc3(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry = mutableListOf(), labels =
    mutableMapOf())

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAA=")

    suspend operator fun invoke(): UInt {
        val mSize = 0
        val buf = ByteBuffer.allocate(mSize)
    }
}

class AnonIDLType0(
    override val id: ByteArray?
) : IDLService() {
    val up: AnonFunc2 = AnonFunc2(this, "up")

    val current: AnonFunc3 = AnonFunc3(this, "current")
}

class AnonFunc1(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val nameType: IDLType = senior.joinu.candid.IDLType.Primitive.Text

    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry = mutableListOf(), labels =
    mutableMapOf())

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAA=")

    suspend operator fun invoke(name: String): AnonIDLType0 {
        val mSize =  + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(nameType, name,
            typeTable)
        val buf = ByteBuffer.allocate(mSize)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(nameType, name, buf, typeTable)
    }
}

class broker(
    override val id: ByteArray?
) : IDLService() {
    val find: AnonFunc1 = AnonFunc1(this, "find")
}

data class AnonIDLType1(
    val `0`: BigInteger,
    val `0x2a`: BigInteger,
    val `2`: UByte
) : IDLSerializable {
    val `0Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `0x2aType`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `2Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Nat8

    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`0Type`, `0`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`0x2aType`, `0x2a`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`2Type`, `2`, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int =  +
    senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`0Type`, `0`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`0x2aType`, `0x2a`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`2Type`, `2`, typeTable)
}

sealed class AnonIDLType2 : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable): Unit = throw
    RuntimeException("Unable to serialize sealed superclass AnonIDLType2")

    override fun sizeBytes(typeTable: TypeTable): Int = throw
    RuntimeException("Unable to get byte size of sealed superclass AnonIDLType2")

    data class A(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null

        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 0.toUInt())
            senior.joinu.candid.KtSerilializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(0) +
                senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(valueType, value, typeTable)
    }

    data class `0x2a`(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null

        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 1.toUInt())
            senior.joinu.candid.KtSerilializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(1) +
                senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(valueType, value, typeTable)
    }

    data class B(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null

        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 2.toUInt())
            senior.joinu.candid.KtSerilializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(2) +
                senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(valueType, value, typeTable)
    }

    data class C(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null

        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 3.toUInt())
            senior.joinu.candid.KtSerilializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(3) +
                senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(valueType, value, typeTable)
    }
}

data class nested(
    val `0`: BigInteger,
    val `1`: BigInteger,
    val `2`: AnonIDLType1,
    val `42`: BigInteger,
    val `40`: BigInteger,
    val `5`: AnonIDLType2
) : IDLSerializable {
    val `0Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `1Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `2Type`: IDLType = senior.joinu.candid.IDLType.Constructive.Record(fields =
    listOf(senior.joinu.candid.IDLFieldType("null",
        senior.joinu.candid.IDLType.Primitive.Natural), senior.joinu.candid.IDLFieldType("0x2a",
        senior.joinu.candid.IDLType.Primitive.Natural), senior.joinu.candid.IDLFieldType("null",
        senior.joinu.candid.IDLType.Primitive.Nat8)))

    val `42Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `40Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `5Type`: IDLType = senior.joinu.candid.IDLType.Constructive.Variant(fields =
    listOf(senior.joinu.candid.IDLFieldType("A", senior.joinu.candid.IDLType.Primitive.Null),
        senior.joinu.candid.IDLFieldType("0x2a", senior.joinu.candid.IDLType.Primitive.Null),
        senior.joinu.candid.IDLFieldType("B", senior.joinu.candid.IDLType.Primitive.Null),
        senior.joinu.candid.IDLFieldType("C", senior.joinu.candid.IDLType.Primitive.Null)))

    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`0Type`, `0`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`1Type`, `1`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`2Type`, `2`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`42Type`, `42`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`40Type`, `40`, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`5Type`, `5`, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int =  +
    senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`0Type`, `0`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`1Type`, `1`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`2Type`, `2`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`42Type`, `42`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`40Type`, `40`, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`5Type`, `5`, typeTable)
}

class AnonFunc4(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val testType: IDLType = senior.joinu.candid.IDLType.Constructive.Blob

    val arg1Type: IDLType =
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Bool)

    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry = mutableListOf(), labels =
    mutableMapOf())

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAA=")

    suspend operator fun invoke(test: ByteArray, arg1: Boolean?) {
        val mSize =  + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(testType, test,
            typeTable) + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg1Type, arg1,
            typeTable)
        val buf = ByteBuffer.allocate(mSize)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(testType, test, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
    }
}

class AnonFunc5(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val arg0Type: IDLType = senior.joinu.candid.IDLType.Id("my_type")

    val arg1Type: IDLType = senior.joinu.candid.IDLType.Id("List")

    val arg2Type: IDLType =
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))

    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry =
    mutableListOf(senior.joinu.candid.IDLType.Primitive.Nat8,
        senior.joinu.candid.IDLType.Constructive.Record(fields =
        listOf(senior.joinu.candid.IDLFieldType("head",
            senior.joinu.candid.IDLType.Primitive.Integer), senior.joinu.candid.IDLFieldType("tail",
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))))),
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))), labels
    = mutableMapOf(senior.joinu.candid.IDLType.Id("my_type") to 0,
        senior.joinu.candid.IDLType.Id("List") to 1))

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAN7bAKg0qyoBHyQ7drnBAJuAQ==")

    suspend operator fun invoke(
        arg0: my_type,
        arg1: List,
        arg2: List?
    ): BigInteger {
        val mSize =  + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg0Type, arg0,
            typeTable) + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg1Type, arg1,
            typeTable) + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg2Type, arg2,
            typeTable)
        val buf = ByteBuffer.allocate(mSize)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg2Type, arg2, buf, typeTable)
    }
}

sealed class AnonIDLType3 : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable): Unit = throw
    RuntimeException("Unable to serialize sealed superclass AnonIDLType3")

    override fun sizeBytes(typeTable: TypeTable): Int = throw
    RuntimeException("Unable to get byte size of sealed superclass AnonIDLType3")

    data class A(
        val value: BigInteger
    ) : AnonIDLType3(), IDLSerializable {
        val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 0.toUInt())
            senior.joinu.candid.KtSerilializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(0) +
                senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(valueType, value, typeTable)
    }

    data class B(
        val value: String?
    ) : AnonIDLType3(), IDLSerializable {
        val valueType: IDLType =
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Text)

        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 1.toUInt())
            senior.joinu.candid.KtSerilializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(1) +
                senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(valueType, value, typeTable)
    }
}

object AnonIDLType5 : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
    }

    override fun sizeBytes(typeTable: TypeTable): Int = 0
}

data class AnonIDLType4(
    val id: BigInteger,
    val `0x2a`: AnonIDLType5
) : IDLSerializable {
    val idType: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

    val `0x2aType`: IDLType = senior.joinu.candid.IDLType.Constructive.Record(fields = listOf())

    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.KtSerilializer.serializeIDLValue(idType, id, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(`0x2aType`, `0x2a`, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int =  +
    senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(idType, id, typeTable) +
            senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(`0x2aType`, `0x2a`, typeTable)
}

class AnonFunc6(
    override val service: IDLService?,
    override val funcName: String?
) : IDLFunc() {
    val arg0Type: IDLType =
        senior.joinu.candid.IDLType.Constructive.Vec(senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Text))

    val arg1Type: IDLType = senior.joinu.candid.IDLType.Constructive.Variant(fields =
    listOf(senior.joinu.candid.IDLFieldType("A", senior.joinu.candid.IDLType.Primitive.Natural),
        senior.joinu.candid.IDLFieldType("B",
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Text))))

    val arg2Type: IDLType =
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))

    val typeTable: TypeTable = senior.joinu.candid.TypeTable(registry =
    mutableListOf(senior.joinu.candid.IDLType.Constructive.Record(fields =
    listOf(senior.joinu.candid.IDLFieldType("head",
        senior.joinu.candid.IDLType.Primitive.Integer), senior.joinu.candid.IDLFieldType("tail",
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))))),
        senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))), labels
    = mutableMapOf(senior.joinu.candid.IDLType.Id("List") to 0))

    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAJsAqDSrKgEfJDt2ucEAW4A")

    suspend operator fun invoke(
        arg0: kotlin.collections.List<String?>,
        arg1: AnonIDLType3,
        arg2: List?
    ): AnonIDLType4 {
        val mSize =  + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg0Type, arg0,
            typeTable) + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg1Type, arg1,
            typeTable) + senior.joinu.candid.KtSerilializer.getSizeBytesOfIDLValue(arg2Type, arg2,
            typeTable)
        val buf = ByteBuffer.allocate(mSize)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
        senior.joinu.candid.KtSerilializer.serializeIDLValue(arg2Type, arg2, buf, typeTable)
    }
}

class server(
    override val id: ByteArray?
) : IDLService() {
    val f: AnonFunc4 = AnonFunc4(this, "f")

    val g: AnonFunc5 = AnonFunc5(this, "g")

    val h: AnonFunc6 = AnonFunc6(this, "h")

    val i: f = f(this, "i")
}