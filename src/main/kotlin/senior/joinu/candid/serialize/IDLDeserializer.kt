package senior.joinu.candid.serialize

import senior.joinu.candid.*
import senior.joinu.leb128.Leb128
import senior.joinu.leb128.Leb128BI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

data class DeserializationContext(
    val typeTable: TypeTable = TypeTable(),
    val types: List<IDLType> = listOf()
)

data class IDLRawValue(val type: IDLType, val content: Any?)

object IDLDeserializer {
    fun deserialize(buf: ByteBuffer): List<IDLRawValue> {
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val context = deserializeUntilM(buf)
        return deserializeM(buf, context)
    }

    fun deserializeUntilM(buf: ByteBuffer): DeserializationContext {
        readMagic(buf)
        val typeTable = readTypeTable(buf)
        val types = readTypes(buf, typeTable)

        return DeserializationContext(typeTable, types)
    }

    fun deserializeM(buf: ByteBuffer, context: DeserializationContext): List<IDLRawValue> {
        return context.types.map { type ->
            val value = readIDLValue(buf, type, context.typeTable)

            IDLRawValue(type, value)
        }
    }

    fun readIDLValue(buf: ByteBuffer, type: IDLType, typeTable: TypeTable): Any? {
        return when (type) {
            is IDLType.Primitive -> when (type) {
                is IDLType.Primitive.Natural -> Leb128BI.readNat(buf)
                is IDLType.Primitive.Nat8 -> buf.get().toUByte()
                is IDLType.Primitive.Nat16 -> buf.short.toUShort()
                is IDLType.Primitive.Nat32 -> buf.int.toUInt()
                is IDLType.Primitive.Nat64 -> buf.long.toULong()

                is IDLType.Primitive.Integer -> Leb128BI.readInt(buf)
                is IDLType.Primitive.Int8 -> buf.get()
                is IDLType.Primitive.Int16 -> buf.short
                is IDLType.Primitive.Int32 -> buf.int
                is IDLType.Primitive.Int64 -> buf.long

                is IDLType.Primitive.Float32 -> buf.float
                is IDLType.Primitive.Float64 -> buf.double

                is IDLType.Primitive.Bool -> {
                    when (val byte = buf.get()) {
                        1.toByte() -> true
                        0.toByte() -> false
                        else -> throw RuntimeException("Invalid boolean value $byte met during deserialization")
                    }
                }
                is IDLType.Primitive.Text -> {
                    val strLength = Leb128.readUnsigned(buf).toInt()
                    val strBytes = ByteArray(strLength)
                    buf.get(strBytes)

                    strBytes.toString(StandardCharsets.UTF_8)
                }
                is IDLType.Primitive.Null -> Null
                is IDLType.Primitive.Reserved -> Reserved
                is IDLType.Primitive.Empty -> Empty
            }
            is IDLType.Constructive -> when (type) {
                is IDLType.Constructive.Opt -> {
                    when (buf.get()) {
                        1.toByte() -> readIDLValue(buf, type.type, typeTable)
                        0.toByte() -> null
                        else -> throw RuntimeException("Invalid present flag met during optional value deserialization")
                    }
                }
                is IDLType.Constructive.Vec -> {
                    val size = Leb128.readUnsigned(buf).toInt()
                    (0..size).map { readIDLValue(buf, type.type, typeTable) }
                }
                is IDLType.Constructive.Blob -> {
                    val size = Leb128.readUnsigned(buf).toInt()
                    val bytes = ByteArray(size)
                    buf.get(bytes)
                }
                is IDLType.Constructive.Record -> {
                    type.fields.associate { fieldType ->
                        fieldType.idx to readIDLValue(buf, fieldType.type, typeTable)
                    }
                }
                is IDLType.Constructive.Variant -> {
                    type.fields.associate { fieldType ->
                        fieldType.idx to readIDLValue(buf, fieldType.type, typeTable)
                    }
                }
            }
            is IDLType.Reference -> when (type) {
                is IDLType.Reference.Service -> {
                    val bytes = when (buf.get()) {
                        1.toByte() -> readIDLValue(buf, IDLType.Constructive.Blob, typeTable)
                        0.toByte() -> null
                        else -> throw RuntimeException("Invalid notOpaque flag met during service deserialization")
                    }
                    SimpleIDLService(bytes as ByteArray?)
                }
                is IDLType.Reference.Func -> {
                    val (service, funcName) = when (buf.get()) {
                        1.toByte() -> Pair(
                            readIDLValue(buf, IDLType.Reference.Service(emptyList()), typeTable),
                            readIDLValue(buf, IDLType.Primitive.Text, typeTable)
                        )
                        0.toByte() -> Pair(null, null)
                        else -> throw RuntimeException("Invalid notOpaque flag met during function deserialization")
                    }
                    SimpleIDLFunc(funcName as String?, service as AbstractIDLService?)
                }
                is IDLType.Reference.Principal -> {
                    val bytes = when (buf.get()) {
                        1.toByte() -> readIDLValue(buf, IDLType.Constructive.Blob, typeTable)
                        0.toByte() -> null
                        else -> throw RuntimeException("Invalid notOpaque flag met during principal deserialization")
                    }
                    SimpleIDLPrincipal(bytes as ByteArray?)
                }
            }
            is IDLType.Other -> when (type) {
                is IDLType.Other.Future -> {
                    val size = Leb128.readUnsigned(buf).toInt()
                    val bytes = ByteArray(size)
                    buf.get(bytes)

                    IDLFutureValue(type.opcode, bytes)
                }
                is IDLType.Other.Custom -> {
                    val customType = typeTable.registry.getOrNull(type.opcode)
                        ?: throw RuntimeException("Unknown type met during deserialization of a type from the type table")

                    readIDLValue(buf, customType, typeTable)
                }
            }
            else -> throw RuntimeException("Unable to deserialize - invalid encoding")
        }
    }

    fun readMagic(buf: ByteBuffer) {
        val byteArray = ByteArray(4)
        buf.get(byteArray)

        assert(byteArray.contentEquals(MAGIC_PREFIX)) { "The first 4 bytes of the response are not magic :c" }
    }

    fun readTypeTable(buf: ByteBuffer): TypeTable {
        val typeTable = TypeTable()

        val tableSize = Leb128.readUnsigned(buf).toInt()

        for (i in 0..tableSize) {
            val type = readIDLType(buf, typeTable)
            typeTable.registerType(type)
        }

        return typeTable
    }

    fun readTypes(buf: ByteBuffer, typeTable: TypeTable): List<IDLType> {
        val typesCount = Leb128.readUnsigned(buf).toInt()

        return (0..typesCount).map { readIDLType(buf, typeTable) }
    }

    fun readIDLType(buf: ByteBuffer, typeTable: TypeTable): IDLType {
        val opcode = Leb128.readSigned(buf)

        return when (opcode) {
            IDLOpcode.NAT.value -> IDLType.Primitive.Natural
            IDLOpcode.NAT8.value -> IDLType.Primitive.Nat8
            IDLOpcode.NAT16.value -> IDLType.Primitive.Nat16
            IDLOpcode.NAT32.value -> IDLType.Primitive.Nat32
            IDLOpcode.NAT64.value -> IDLType.Primitive.Nat64

            IDLOpcode.INT.value -> IDLType.Primitive.Integer
            IDLOpcode.INT8.value -> IDLType.Primitive.Int8
            IDLOpcode.INT16.value -> IDLType.Primitive.Int16
            IDLOpcode.INT32.value -> IDLType.Primitive.Int32
            IDLOpcode.INT64.value -> IDLType.Primitive.Int64

            IDLOpcode.FLOAT32.value -> IDLType.Primitive.Float32
            IDLOpcode.FLOAT64.value -> IDLType.Primitive.Float64

            IDLOpcode.BOOL.value -> IDLType.Primitive.Bool
            IDLOpcode.TEXT.value -> IDLType.Primitive.Text
            IDLOpcode.NULL.value -> IDLType.Primitive.Null
            IDLOpcode.RESERVED.value -> IDLType.Primitive.Reserved
            IDLOpcode.EMPTY.value -> IDLType.Primitive.Empty

            IDLOpcode.OPT.value -> {
                val innerType = readIDLType(buf, typeTable)
                IDLType.Constructive.Opt(innerType)
            }
            IDLOpcode.VEC.value -> {
                val innerType = readIDLType(buf, typeTable)

                if (innerType is IDLType.Primitive.Nat8)
                    IDLType.Constructive.Blob
                else
                    IDLType.Constructive.Vec(innerType)
            }
            IDLOpcode.RECORD.value -> {
                val fieldCount = Leb128.readUnsigned(buf).toInt()
                val fields = (0..fieldCount).map {
                    val id = Leb128.readUnsigned(buf).toInt()
                    val type = readIDLType(buf, typeTable)

                    IDLFieldType(null, type, id)
                }

                IDLType.Constructive.Record(fields)
            }
            IDLOpcode.VARIANT.value -> {
                val fieldCount = Leb128.readUnsigned(buf).toInt()
                val fields = (0..fieldCount).map {
                    val id = Leb128.readUnsigned(buf).toInt()
                    val type = readIDLType(buf, typeTable)

                    IDLFieldType(null, type, id)
                }

                IDLType.Constructive.Variant(fields)
            }

            IDLOpcode.FUNC.value -> {
                val argumentCount = Leb128.readUnsigned(buf).toInt()
                val arguments = (0..argumentCount).map { IDLArgType(null, readIDLType(buf, typeTable)) }

                val resultsCount = Leb128.readUnsigned(buf).toInt()
                val results = (0..resultsCount).map { IDLArgType(null, readIDLType(buf, typeTable)) }

                val annotationsCount = Leb128.readUnsigned(buf).toInt()
                val annotations = (0..annotationsCount).map {
                    val byte = buf.get()
                    when (byte.toInt()) {
                        1 -> IDLFuncAnn.Query
                        2 -> IDLFuncAnn.Oneway
                        else -> IDLFuncAnn.Unknown
                    }
                }

                IDLType.Reference.Func(arguments, results, annotations)
            }
            IDLOpcode.SERVICE.value -> {
                val methodCount = Leb128.readUnsigned(buf).toInt()
                val methods = (0..methodCount).map {
                    val nameLength = Leb128.readUnsigned(buf).toInt()
                    val nameBytes = ByteArray(nameLength)
                    buf.get(nameBytes)
                    val nameStr = nameBytes.toString(StandardCharsets.UTF_8)
                    val type = readIDLType(buf, typeTable)

                    assert((type is IDLType.Id) or (type is IDLType.Reference.Func)) { "Method type is not valid!" }

                    IDLMethod(nameStr, type as IDLMethodType)
                }

                IDLType.Reference.Service(methods)
            }
            IDLOpcode.PRINCIPAL.value -> IDLType.Reference.Principal
            else -> {
                if (opcode >= 0) {
                    assert(typeTable.registry.size > opcode) { "Unknown type met during deserialization!" }
                    IDLType.Other.Custom(opcode)
                } else
                    IDLType.Other.Future(opcode)
            }
        }
    }
}

data class IDLFutureValue(val opcode: Int, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IDLFutureValue

        if (opcode != other.opcode) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = opcode.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}