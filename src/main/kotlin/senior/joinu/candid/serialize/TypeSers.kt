package senior.joinu.candid.serialize

import com.squareup.kotlinpoet.CodeBlock
import senior.joinu.candid.IDLFuncAnn
import senior.joinu.candid.IDLOpcode
import senior.joinu.candid.IDLType
import senior.joinu.candid.TypeTable
import senior.joinu.leb128.Leb128
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

interface TypeSer : Ser {
    fun serType(buf: ByteBuffer)
    fun calcTypeSizeBytes(): Int
}

object NatTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.NAT.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.NAT.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", NatTypeSer::class).toString()
    }
}

object Nat8TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.NAT8.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.NAT8.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat8TypeSer::class).toString()
    }
}

object Nat16TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.NAT16.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.NAT16.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat16TypeSer::class).toString()
    }
}

object Nat32TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.NAT32.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.NAT32.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat32TypeSer::class).toString()
    }
}

object Nat64TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.NAT64.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.NAT64.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat64TypeSer::class).toString()
    }
}

object IntTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.INT.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.INT.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", IntTypeSer::class).toString()
    }
}

object Int8TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.INT8.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.INT8.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int8TypeSer::class).toString()
    }
}

object Int16TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.INT16.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.INT16.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int16TypeSer::class).toString()
    }
}

object Int32TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.INT32.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.INT32.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int32TypeSer::class).toString()
    }
}

object Int64TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.INT64.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.INT64.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int64TypeSer::class).toString()
    }
}

object Float32TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.FLOAT32.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.FLOAT32.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Float32TypeSer::class).toString()
    }
}

object Float64TypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.FLOAT64.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.FLOAT64.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Float64TypeSer::class).toString()
    }
}

object BoolTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.BOOL.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.BOOL.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", BoolTypeSer::class).toString()
    }
}

object TextTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.TEXT.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.TEXT.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", TextTypeSer::class).toString()
    }
}

object NullTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.NULL.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.NULL.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", NullTypeSer::class).toString()
    }
}

object ReservedTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.RESERVED.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.RESERVED.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", ReservedTypeSer::class).toString()
    }
}

object EmptyTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.EMPTY.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.EMPTY.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", EmptyTypeSer::class).toString()
    }
}

class CustomTypeSer(val customTypeOpcode: Int) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, customTypeOpcode)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(customTypeOpcode)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T($customTypeOpcode)", CustomTypeSer::class).toString()
    }
}

class OptTypeSer(val innerSer: TypeSer) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.OPT.value)
        innerSer.serType(buf)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.OPT.value) + innerSer.calcTypeSizeBytes()
    }

    override fun poetize(): String {
        return CodeBlock.of("%T(${innerSer.poetize()})", OptTypeSer::class).toString()
    }
}

class VecTypeSer(val innerSer: TypeSer) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.VEC.value)
        innerSer.serType(buf)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.VEC.value) + innerSer.calcTypeSizeBytes()
    }

    override fun poetize(): String {
        return CodeBlock.of("%T(${innerSer.poetize()})", VecTypeSer::class).toString()
    }
}

object BlobTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.VEC.value)
        Leb128.writeSigned(buf, IDLOpcode.NAT8.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.VEC.value) + Leb128.sizeSigned(IDLOpcode.NAT8.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", BlobTypeSer::class).toString()
    }
}

class RecordTypeSer(val innerSers: Map<Int, TypeSer>) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.RECORD.value)
        innerSers.entries.forEach { (idx, ser) ->
            Leb128.writeUnsigned(buf, idx)
            ser.serType(buf)
        }
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.RECORD.value) +
                innerSers.entries
                    .map { (idx, ser) -> Leb128.sizeUnsigned(idx) + ser.calcTypeSizeBytes() }
                    .sum()
    }

    override fun poetize(): String {
        val poetizedInnerSers = innerSers.entries
            .joinToString(prefix = "mapOf(", postfix = ")") { (key, ser) -> "$key to ${ser.poetize()}" }

        return CodeBlock.of("%T(${poetizedInnerSers})", RecordTypeSer::class).toString()
    }
}

class VariantTypeSer(val innerSers: Map<Int, TypeSer>) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.VARIANT.value)
        innerSers.entries.forEach { (idx, ser) ->
            Leb128.writeUnsigned(buf, idx)
            ser.serType(buf)
        }
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.VARIANT.value) +
                innerSers.entries
                    .map { (idx, ser) -> Leb128.sizeUnsigned(idx) + ser.calcTypeSizeBytes() }
                    .sum()
    }

    override fun poetize(): String {
        val poetizedInnerSers = innerSers.entries
            .joinToString(prefix = "mapOf(", postfix = ")") { (idx, ser) -> "$idx to ${ser.poetize()}" }

        return CodeBlock.of("%T(${poetizedInnerSers})", VariantTypeSer::class).toString()
    }
}

object QueryAnnTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        buf.put(1)
    }

    override fun calcTypeSizeBytes(): Int {
        return Byte.SIZE_BYTES
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", QueryAnnTypeSer::class).toString()
    }
}

object OnewayAnnTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        buf.put(2)
    }

    override fun calcTypeSizeBytes(): Int {
        return Byte.SIZE_BYTES
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", OnewayAnnTypeSer::class).toString()
    }
}

class FuncTypeSer(
    val argsSer: List<TypeSer>,
    val ressSer: List<TypeSer>,
    val annsSer: List<TypeSer>
) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.FUNC.value)
        argsSer.forEach { it.serType(buf) }
        ressSer.forEach { it.serType(buf) }
        annsSer.forEach { it.serType(buf) }
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.FUNC.value) +
                argsSer.map { it.calcTypeSizeBytes() }.sum() +
                ressSer.map { it.calcTypeSizeBytes() }.sum() +
                annsSer.map { it.calcTypeSizeBytes() }.sum()
    }

    override fun poetize(): String {
        val poetizedArgsSer = argsSer.joinToString(prefix = "listOf(", postfix = ")") { it.poetize() }
        val poetizedRessSer = ressSer.joinToString(prefix = "listOf(", postfix = ")") { it.poetize() }
        val poetizedAnnsSer = ressSer.joinToString(prefix = "listOf(", postfix = ")") { it.poetize() }

        return CodeBlock.of("%T($poetizedArgsSer, $poetizedRessSer, $poetizedAnnsSer)", FuncTypeSer::class).toString()
    }
}

class ServiceTypeSer(val innerSers: Map<String, TypeSer>) : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.SERVICE.value)
        innerSers.entries.forEach { (key, ser) ->
            val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
            Leb128.writeUnsigned(buf, keyBytes.size)
            buf.put(keyBytes)

            ser.serType(buf)
        }
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.SERVICE.value) +
                innerSers.entries
                    .map { (key, ser) ->
                        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)

                        Leb128.sizeUnsigned(keyBytes.size) + keyBytes.size + ser.calcTypeSizeBytes()
                    }
                    .sum()
    }

    override fun poetize(): String {
        val poetizedInnerSers = innerSers.entries
            .joinToString(prefix = "mapOf(", postfix = ")") { (key, ser) -> "\"$key\" to ${ser.poetize()}" }

        return CodeBlock.of("%T($poetizedInnerSers)", ServiceTypeSer::class).toString()
    }
}

object PrincipalTypeSer : TypeSer {
    override fun serType(buf: ByteBuffer) {
        Leb128.writeSigned(buf, IDLOpcode.PRINCIPAL.value)
    }

    override fun calcTypeSizeBytes(): Int {
        return Leb128.sizeSigned(IDLOpcode.PRINCIPAL.value)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", PrincipalTypeSer::class).toString()
    }
}

fun getTypeSerForInnerType(type: IDLType, typeTable: TypeTable): TypeSer {
    return if (type is IDLType.Primitive)
        getTypeSerForType(type, typeTable)
    else {
        val opcode = typeTable.registerType(type)
        CustomTypeSer(opcode)
    }
}

fun getTypeSerForType(type: IDLType, typeTable: TypeTable): TypeSer {
    return when (type) {
        is IDLType.Id -> {
            val labeledType = typeTable.getTypeByLabel(type)
            getTypeSerForType(labeledType, typeTable)
        }

        is IDLType.Primitive.Natural -> NatTypeSer
        is IDLType.Primitive.Nat8 -> Nat8TypeSer
        is IDLType.Primitive.Nat16 -> Nat16TypeSer
        is IDLType.Primitive.Nat32 -> Nat32TypeSer
        is IDLType.Primitive.Nat64 -> Nat64TypeSer

        is IDLType.Primitive.Integer -> IntTypeSer
        is IDLType.Primitive.Int8 -> Int8TypeSer
        is IDLType.Primitive.Int16 -> Int16TypeSer
        is IDLType.Primitive.Int32 -> Int32TypeSer
        is IDLType.Primitive.Int64 -> Int64TypeSer

        is IDLType.Primitive.Float32 -> Float32TypeSer
        is IDLType.Primitive.Float64 -> Float64TypeSer

        is IDLType.Primitive.Bool -> BoolTypeSer
        is IDLType.Primitive.Text -> TextTypeSer
        is IDLType.Primitive.Null -> NullTypeSer
        is IDLType.Primitive.Reserved -> ReservedTypeSer
        is IDLType.Primitive.Empty -> EmptyTypeSer

        is IDLType.Constructive.Opt -> {
            val innerSer = getTypeSerForInnerType(type.type, typeTable)
            OptTypeSer(innerSer)
        }
        is IDLType.Constructive.Vec -> {
            val innerSer = getTypeSerForInnerType(type.type, typeTable)
            VecTypeSer(innerSer)
        }
        is IDLType.Constructive.Blob -> BlobTypeSer
        is IDLType.Constructive.Record -> {
            val innerSers = type.fields.associate { it.idx to getTypeSerForInnerType(it.type, typeTable) }
            RecordTypeSer(innerSers)
        }
        is IDLType.Constructive.Variant -> {
            val innerSers = type.fields.associate { it.idx to getTypeSerForInnerType(it.type, typeTable) }
            VariantTypeSer(innerSers)
        }

        is IDLType.Reference.Func -> {
            val argsSer = type.arguments.map { getTypeSerForType(it.type, typeTable) }
            val ressSer = type.results.map { getTypeSerForType(it.type, typeTable) }
            val annsSer = type.annotations.map {
                when (it) {
                    IDLFuncAnn.Query -> QueryAnnTypeSer
                    IDLFuncAnn.Oneway -> OnewayAnnTypeSer
                    else -> throw RuntimeException("Unable to get serializer for function annotation $it")
                }
            }

            FuncTypeSer(argsSer, ressSer, annsSer)
        }
        is IDLType.Reference.Service -> {
            val innerSers = type.methods.associate { it.name to getTypeSerForInnerType(it.type as IDLType, typeTable) }

            ServiceTypeSer(innerSers)
        }
        is IDLType.Reference.Principal -> PrincipalTypeSer

        else -> throw RuntimeException("Unable to get serializer for type $type")
    }
}