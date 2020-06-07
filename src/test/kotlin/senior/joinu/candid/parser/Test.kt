import senior.joinu.candid.IDLType
import senior.joinu.candid.Null
import senior.joinu.candid.TypeTable
import senior.joinu.candid.serialize.AbstractIDLFunc
import senior.joinu.candid.serialize.AbstractIDLService
import senior.joinu.candid.serialize.IDLDeserializable
import senior.joinu.candid.serialize.IDLSerializable
import senior.joinu.leb128.Leb128
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

typealias my_type = UByte

class List(
    val head: BigInteger,
    val tail: List?
) : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(headType, head, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(tailType, tail, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int = 0 +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(headType, head, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(tailType, tail, typeTable)

    companion object : IDLDeserializable<List> {
        val headType: IDLType = senior.joinu.candid.IDLType.Primitive.Integer

        val tailType: IDLType =
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))

        override fun deserialize(buf: ByteBuffer, typeTable: TypeTable): List = List(
            head =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, headType, typeTable,
                null
            ) as java.math.BigInteger, tail =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, tailType, typeTable,
                null
            ) as List?
        )
    }
}

data class AnonFunc0Result(
    val `0`: Long
)

class AnonFunc0(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(arg0: Int): AnonFunc0Result {
        val bufSize = staticPayload.size +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(arg0Type, arg0, typeTable)
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
    }

    companion object {
        val arg0Type: IDLType = senior.joinu.candid.IDLType.Primitive.Int32

        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry = mutableListOf(), labels =
            mutableMapOf()
        )

        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABdQ==")
    }
}

data class FResult(
    val `0`: List?
)

class f(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(arg0: List, arg1: AnonFunc0): FResult {
        val bufSize = staticPayload.size +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    arg0Type, arg0,
                    typeTable
                ) + senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
            arg1Type,
            arg1, typeTable
        )
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
    }

    companion object {
        val arg0Type: IDLType = senior.joinu.candid.IDLType.Id("List")

        val arg1Type: IDLType = senior.joinu.candid.IDLType.Reference.Func(
            arguments =
            listOf(senior.joinu.candid.IDLArgType("null", senior.joinu.candid.IDLType.Primitive.Int32)),
            results = listOf(
                senior.joinu.candid.IDLArgType(
                    "null",
                    senior.joinu.candid.IDLType.Primitive.Int64
                )
            ), annotations = listOf()
        )

        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry =
            mutableListOf(
                senior.joinu.candid.IDLType.Constructive.Record(
                    fields =
                    listOf(
                        senior.joinu.candid.IDLFieldType(
                            "head",
                            senior.joinu.candid.IDLType.Primitive.Integer, 1158359328
                        ),
                        senior.joinu.candid.IDLFieldType(
                            "tail",
                            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List")),
                            1291237008
                        )
                    )
                ),
                senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))
            ),
            labels = mutableMapOf(senior.joinu.candid.IDLType.Id("List") to 0)
        )

        val staticPayload: ByteArray =
            Base64.getDecoder().decode("RElETAJsAqDSrKgEfJDt2ucEAW4AAgBqAXUBdAA=")
    }
}

data class AnonFunc2Result(
    val `0`: UInt
)

class AnonFunc2(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(): AnonFunc2Result {
        val bufSize = staticPayload.size
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
    }

    companion object {
        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry = mutableListOf(), labels =
            mutableMapOf()
        )

        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")
    }
}

class AnonFunc3(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke() {
        val bufSize = staticPayload.size
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
    }

    companion object {
        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry = mutableListOf(), labels =
            mutableMapOf()
        )

        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")
    }
}

class AnonIDLType0(
    override val id: ByteArray?
) : AbstractIDLService() {
    val current: AnonFunc2 = AnonFunc2(this, "current")

    val up: AnonFunc3 = AnonFunc3(this, "up")
}

data class AnonFunc1Result(
    val `0`: AnonIDLType0
)

class AnonFunc1(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(name: String): AnonFunc1Result {
        val bufSize = staticPayload.size +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(nameType, name, typeTable)
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(nameType, name, buf, typeTable)
    }

    companion object {
        val nameType: IDLType = senior.joinu.candid.IDLType.Primitive.Text

        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry = mutableListOf(), labels =
            mutableMapOf()
        )

        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")
    }
}

class broker(
    override val id: ByteArray?
) : AbstractIDLService() {
    val find: AnonFunc1 = AnonFunc1(this, "find")
}

class AnonIDLType1(
    val `0`: BigInteger,
    val `2`: UByte,
    val `0x2a`: BigInteger
) : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`0Type`, `0`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`2Type`, `2`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`0x2aType`, `0x2a`, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int = 0 +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`0Type`, `0`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`2Type`, `2`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                `0x2aType`, `0x2a`,
                typeTable
            )

    companion object : IDLDeserializable<AnonIDLType1> {
        val `0Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        val `2Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Nat8

        val `0x2aType`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        override fun deserialize(buf: ByteBuffer, typeTable: TypeTable): AnonIDLType1 = AnonIDLType1(
            `0`
            = senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `0Type`, typeTable,
                null
            ) as java.math.BigInteger, `2` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `2Type`, typeTable,
                null
            ) as kotlin.UByte, `0x2a` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `0x2aType`,
                typeTable, null
            ) as java.math.BigInteger
        )
    }
}

sealed class AnonIDLType2 : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable): Unit = throw
    RuntimeException("Unable to serialize sealed superclass AnonIDLType2")

    override fun sizeBytes(typeTable: TypeTable): Int = throw
    RuntimeException("Unable to get byte size of sealed superclass AnonIDLType2")

    data class `0x2a`(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 42.toUInt())
            senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(42) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    valueType, value,
                    typeTable
                )

        companion object {
            val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null
        }
    }

    data class A(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 65.toUInt())
            senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(65) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    valueType, value,
                    typeTable
                )

        companion object {
            val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null
        }
    }

    data class B(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 66.toUInt())
            senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(66) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    valueType, value,
                    typeTable
                )

        companion object {
            val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null
        }
    }

    data class C(
        val value: Null
    ) : AnonIDLType2(), IDLSerializable {
        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 67.toUInt())
            senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(67) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    valueType, value,
                    typeTable
                )

        companion object {
            val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Null
        }
    }
}

class nested(
    val `0`: BigInteger,
    val `1`: BigInteger,
    val `2`: AnonIDLType1,
    val `5`: AnonIDLType2,
    val `40`: BigInteger,
    val `42`: BigInteger
) : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`0Type`, `0`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`1Type`, `1`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`2Type`, `2`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`5Type`, `5`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`40Type`, `40`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`42Type`, `42`, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int = 0 +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`0Type`, `0`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`1Type`, `1`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`2Type`, `2`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`5Type`, `5`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`40Type`, `40`, typeTable) +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(`42Type`, `42`, typeTable)

    companion object : IDLDeserializable<nested> {
        val `0Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        val `1Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        val `2Type`: IDLType = senior.joinu.candid.IDLType.Constructive.Record(
            fields =
            listOf(
                senior.joinu.candid.IDLFieldType(
                    "null",
                    senior.joinu.candid.IDLType.Primitive.Natural, 0
                ), senior.joinu.candid.IDLFieldType(
                    "null",
                    senior.joinu.candid.IDLType.Primitive.Nat8, 2
                ), senior.joinu.candid.IDLFieldType(
                    "0x2a",
                    senior.joinu.candid.IDLType.Primitive.Natural, 42
                )
            )
        )

        val `5Type`: IDLType = senior.joinu.candid.IDLType.Constructive.Variant(
            fields =
            listOf(
                senior.joinu.candid.IDLFieldType(
                    "0x2a", senior.joinu.candid.IDLType.Primitive.Null,
                    42
                ), senior.joinu.candid.IDLFieldType("A", senior.joinu.candid.IDLType.Primitive.Null, 65),
                senior.joinu.candid.IDLFieldType("B", senior.joinu.candid.IDLType.Primitive.Null, 66),
                senior.joinu.candid.IDLFieldType("C", senior.joinu.candid.IDLType.Primitive.Null, 67)
            )
        )

        val `40Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        val `42Type`: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        override fun deserialize(buf: ByteBuffer, typeTable: TypeTable): nested = nested(
            `0` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `0Type`, typeTable,
                null
            ) as java.math.BigInteger, `1` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `1Type`, typeTable,
                null
            ) as java.math.BigInteger, `2` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `2Type`, typeTable,
                AnonIDLType1.Companion
            ) as AnonIDLType1, `5` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `5Type`, typeTable,
                null
            ) as AnonIDLType2, `40` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `40Type`, typeTable,
                null
            ) as java.math.BigInteger, `42` =
            senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                buf, `42Type`, typeTable,
                null
            ) as java.math.BigInteger
        )
    }
}

class AnonFunc4(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(test: ByteArray, arg1: Boolean?) {
        val bufSize = staticPayload.size +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    testType, test,
                    typeTable
                ) + senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
            arg1Type,
            arg1, typeTable
        )
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(testType, test, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
    }

    companion object {
        val testType: IDLType = senior.joinu.candid.IDLType.Constructive.Blob

        val arg1Type: IDLType =
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Bool)

        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry = mutableListOf(), labels =
            mutableMapOf()
        )

        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAACbXtufg==")
    }
}

data class AnonFunc5Result(
    val `0`: BigInteger
)

class AnonFunc5(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(
        arg0: my_type,
        arg1: List,
        arg2: List?
    ): AnonFunc5Result {
        val bufSize = staticPayload.size +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    arg0Type, arg0,
                    typeTable
                ) + senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
            arg1Type,
            arg1, typeTable
        ) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(arg2Type, arg2, typeTable)
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg2Type, arg2, buf, typeTable)
    }

    companion object {
        val arg0Type: IDLType = senior.joinu.candid.IDLType.Id("my_type")

        val arg1Type: IDLType = senior.joinu.candid.IDLType.Id("List")

        val arg2Type: IDLType =
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))

        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry =
            mutableListOf(
                senior.joinu.candid.IDLType.Primitive.Nat8,
                senior.joinu.candid.IDLType.Constructive.Record(
                    fields =
                    listOf(
                        senior.joinu.candid.IDLFieldType(
                            "head",
                            senior.joinu.candid.IDLType.Primitive.Integer, 1158359328
                        ),
                        senior.joinu.candid.IDLFieldType(
                            "tail",
                            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List")),
                            1291237008
                        )
                    )
                ),
                senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))
            ),
            labels = mutableMapOf(
                senior.joinu.candid.IDLType.Id("my_type") to 0,
                senior.joinu.candid.IDLType.Id("List") to 1
            )
        )

        val staticPayload: ByteArray =
            Base64.getDecoder().decode("RElETAN7bAKg0qyoBHyQ7drnBAJuAQMAAW4B")
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
        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 65.toUInt())
            senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(65) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    valueType, value,
                    typeTable
                )

        companion object {
            val valueType: IDLType = senior.joinu.candid.IDLType.Primitive.Natural
        }
    }

    data class B(
        val value: String?
    ) : AnonIDLType3(), IDLSerializable {
        override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
            Leb128.writeUnsigned(buf, 66.toUInt())
            senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(valueType, value, buf, typeTable)
        }

        override fun sizeBytes(typeTable: TypeTable): Int = Leb128.sizeUnsigned(66) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    valueType, value,
                    typeTable
                )

        companion object {
            val valueType: IDLType =
                senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Text)
        }
    }
}

class AnonIDLType5 : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
    }

    override fun sizeBytes(typeTable: TypeTable): Int = 0

    companion object : IDLDeserializable<AnonIDLType5> {
        override fun deserialize(buf: ByteBuffer, typeTable: TypeTable): AnonIDLType5 = AnonIDLType5()
    }
}

class AnonIDLType4(
    val `0x2a`: AnonIDLType5,
    val id: BigInteger
) : IDLSerializable {
    override fun serialize(buf: ByteBuffer, typeTable: TypeTable) {
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(`0x2aType`, `0x2a`, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(idType, id, buf, typeTable)
    }

    override fun sizeBytes(typeTable: TypeTable): Int = 0 +
            senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                `0x2aType`, `0x2a`,
                typeTable
            ) + senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
        idType, id,
        typeTable
    )

    companion object : IDLDeserializable<AnonIDLType4> {
        val `0x2aType`: IDLType = senior.joinu.candid.IDLType.Constructive.Record(fields = listOf())

        val idType: IDLType = senior.joinu.candid.IDLType.Primitive.Natural

        override fun deserialize(buf: ByteBuffer, typeTable: TypeTable): AnonIDLType4 =
            AnonIDLType4(
                `0x2a` = senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                    buf,
                    `0x2aType`, typeTable, AnonIDLType5.Companion
                ) as AnonIDLType5, id =
                senior.joinu.candid.serialize.IDLDeserializer.deserializeIDLValue(
                    buf, idType, typeTable,
                    null
                ) as java.math.BigInteger
            )
    }
}

data class AnonFunc6Result(
    val `0`: AnonIDLType4
)

class AnonFunc6(
    override val service: AbstractIDLService?,
    override val funcName: String?
) : AbstractIDLFunc() {
    suspend operator fun invoke(
        arg0: kotlin.collections.List<String?>,
        arg1: AnonIDLType3,
        arg2: List?
    ): AnonFunc6Result {
        val bufSize = staticPayload.size +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
                    arg0Type, arg0,
                    typeTable
                ) + senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(
            arg1Type,
            arg1, typeTable
        ) +
                senior.joinu.candid.serialize.KtSerializer.getSizeBytesOfIDLValue(arg2Type, arg2, typeTable)
        val buf = ByteBuffer.allocate(bufSize)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.put(staticPayload)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg0Type, arg0, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg1Type, arg1, buf, typeTable)
        senior.joinu.candid.serialize.KtSerializer.serializeIDLValue(arg2Type, arg2, buf, typeTable)
    }

    companion object {
        val arg0Type: IDLType =
            senior.joinu.candid.IDLType.Constructive.Vec(senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Text))

        val arg1Type: IDLType = senior.joinu.candid.IDLType.Constructive.Variant(
            fields =
            listOf(
                senior.joinu.candid.IDLFieldType(
                    "A", senior.joinu.candid.IDLType.Primitive.Natural,
                    65
                ), senior.joinu.candid.IDLFieldType(
                    "B",
                    senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Primitive.Text),
                    66
                )
            )
        )

        val arg2Type: IDLType =
            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))

        val typeTable: TypeTable = senior.joinu.candid.TypeTable(
            registry =
            mutableListOf(
                senior.joinu.candid.IDLType.Constructive.Record(
                    fields =
                    listOf(
                        senior.joinu.candid.IDLFieldType(
                            "head",
                            senior.joinu.candid.IDLType.Primitive.Integer, 1158359328
                        ),
                        senior.joinu.candid.IDLFieldType(
                            "tail",
                            senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List")),
                            1291237008
                        )
                    )
                ),
                senior.joinu.candid.IDLType.Constructive.Opt(senior.joinu.candid.IDLType.Id("List"))
            ),
            labels = mutableMapOf(senior.joinu.candid.IDLType.Id("List") to 0)
        )

        val staticPayload: ByteArray =
            Base64.getDecoder().decode("RElETANsAqDSrKgEfJDt2ucEAW4AbnEDbQJrAkF9QgJuAA==")
    }
}

class server(
    override val id: ByteArray?
) : AbstractIDLService() {
    val f: AnonFunc4 = AnonFunc4(this, "f")

    val g: AnonFunc5 = AnonFunc5(this, "g")

    val h: AnonFunc6 = AnonFunc6(this, "h")

    val i: f = f(this, "i")
}
