package senior.joinu.candid.serialize

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import senior.joinu.candid.*
import senior.joinu.leb128.Leb128
import senior.joinu.leb128.Leb128BI
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets


interface ValueSer<T> : Ser {
    fun calcSizeBytes(value: T): Int
    fun ser(buf: ByteBuffer, value: T)
    fun deser(buf: ByteBuffer): T
}

object NatValueSer : ValueSer<BigInteger> {
    override fun calcSizeBytes(value: BigInteger): Int {
        return Leb128BI.sizeNat(value)
    }

    override fun ser(buf: ByteBuffer, value: BigInteger) {
        Leb128BI.writeNat(buf, value)
    }

    override fun deser(buf: ByteBuffer): BigInteger {
        return Leb128BI.readNat(buf)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", NatValueSer::class).toString()
    }
}

object Nat8ValueSer : ValueSer<Byte> {
    override fun calcSizeBytes(value: Byte): Int {
        return Byte.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Byte) {
        buf.put(value)
    }

    override fun deser(buf: ByteBuffer): Byte {
        return buf.get()
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat8ValueSer::class).toString()
    }
}

object Nat16ValueSer : ValueSer<Short> {
    override fun calcSizeBytes(value: Short): Int {
        return Short.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Short) {
        buf.putShort(value)
    }

    override fun deser(buf: ByteBuffer): Short {
        return buf.short
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat16ValueSer::class).toString()
    }
}

object Nat32ValueSer : ValueSer<Int> {
    override fun calcSizeBytes(value: Int): Int {
        return Int.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Int) {
        buf.putInt(value)
    }

    override fun deser(buf: ByteBuffer): Int {
        return buf.int
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat32ValueSer::class).toString()
    }
}

object Nat64ValueSer : ValueSer<Long> {
    override fun calcSizeBytes(value: Long): Int {
        return Long.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Long) {
        buf.putLong(value)
    }

    override fun deser(buf: ByteBuffer): Long {
        return buf.long
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Nat64ValueSer::class).toString()
    }
}

object IntValueSer : ValueSer<BigInteger> {
    override fun calcSizeBytes(value: BigInteger): Int {
        return Leb128BI.sizeInt(value)
    }

    override fun ser(buf: ByteBuffer, value: BigInteger) {
        Leb128BI.writeInt(buf, value)
    }

    override fun deser(buf: ByteBuffer): BigInteger {
        return Leb128BI.readInt(buf)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", IntValueSer::class).toString()
    }
}

object Int8ValueSer : ValueSer<Byte> {
    override fun calcSizeBytes(value: Byte): Int {
        return Byte.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Byte) {
        buf.put(value)
    }

    override fun deser(buf: ByteBuffer): Byte {
        return buf.get()
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int8ValueSer::class).toString()
    }
}

object Int16ValueSer : ValueSer<Short> {
    override fun calcSizeBytes(value: Short): Int {
        return Short.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Short) {
        buf.putShort(value)
    }

    override fun deser(buf: ByteBuffer): Short {
        return buf.short
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int16ValueSer::class).toString()
    }
}

object Int32ValueSer : ValueSer<Int> {
    override fun calcSizeBytes(value: Int): Int {
        return Int.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Int) {
        buf.putInt(value)
    }

    override fun deser(buf: ByteBuffer): Int {
        return buf.int
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int32ValueSer::class).toString()
    }
}

object Int64ValueSer : ValueSer<Long> {
    override fun calcSizeBytes(value: Long): Int {
        return Long.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Long) {
        buf.putLong(value)
    }

    override fun deser(buf: ByteBuffer): Long {
        return buf.long
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Int64ValueSer::class).toString()
    }
}

object Float32ValueSer : ValueSer<Float> {
    override fun calcSizeBytes(value: Float): Int {
        return Int.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Float) {
        buf.putFloat(value)
    }

    override fun deser(buf: ByteBuffer): Float {
        return buf.float
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Float32ValueSer::class).toString()
    }
}

object Float64ValueSer : ValueSer<Double> {
    override fun calcSizeBytes(value: Double): Int {
        return Long.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Double) {
        buf.putDouble(value)
    }

    override fun deser(buf: ByteBuffer): Double {
        return buf.double
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", Float64ValueSer::class).toString()
    }
}

object BoolValueSer : ValueSer<Boolean> {
    override fun calcSizeBytes(value: Boolean): Int {
        return Byte.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: Boolean) {
        if (value)
            buf.put(1)
        else
            buf.put(0)
    }

    override fun deser(buf: ByteBuffer): Boolean {
        return when (val byte = buf.get()) {
            1.toByte() -> true
            0.toByte() -> false
            else -> throw RuntimeException("Invalid boolean value $byte met during deserialization")
        }
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", BoolValueSer::class).toString()
    }
}

object TextValueSer : ValueSer<String> {
    override fun calcSizeBytes(value: String): Int {
        val strBytes = value.toByteArray(StandardCharsets.UTF_8)

        return Leb128.sizeSigned(strBytes.size) + strBytes.size
    }

    override fun ser(buf: ByteBuffer, value: String) {
        val strBytes = value.toByteArray(StandardCharsets.UTF_8)
        Leb128.writeUnsigned(buf, strBytes.size)
        buf.put(strBytes)
    }

    override fun deser(buf: ByteBuffer): String {
        val strLength = Leb128.readUnsigned(buf)
        val strBytes = ByteArray(strLength)
        buf.get(strBytes)

        return strBytes.toString(StandardCharsets.UTF_8)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", TextValueSer::class).toString()
    }
}

object NullValueSer : ValueSer<Null> {
    override fun calcSizeBytes(value: Null): Int {
        return 0
    }

    override fun ser(buf: ByteBuffer, value: Null) {
    }

    override fun deser(buf: ByteBuffer): Null {
        return Null
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", NullValueSer::class).toString()
    }
}

object ReservedValueSer : ValueSer<Reserved> {
    override fun calcSizeBytes(value: Reserved): Int {
        return 0
    }

    override fun ser(buf: ByteBuffer, value: Reserved) {
    }

    override fun deser(buf: ByteBuffer): Reserved {
        return Reserved
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", ReservedValueSer::class).toString()
    }
}

object EmptyValueSer : ValueSer<Empty> {
    override fun calcSizeBytes(value: Empty): Int {
        return 0
    }

    override fun ser(buf: ByteBuffer, value: Empty) {
        throw RuntimeException("Unable to serialize empty IDL value")
    }

    override fun deser(buf: ByteBuffer): Empty {
        return Empty
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", EmptyValueSer::class).toString()
    }
}

class OptValueSer<T>(val innerSer: ValueSer<T>) : ValueSer<T?> {
    override fun calcSizeBytes(value: T?): Int {
        return Byte.SIZE_BYTES + if (value != null) innerSer.calcSizeBytes(value) else 0
    }

    override fun ser(buf: ByteBuffer, value: T?) {
        if (value != null) {
            buf.put(1)
            innerSer.ser(buf, value)
        } else
            buf.put(0)
    }

    override fun deser(buf: ByteBuffer): T? {
        return when (buf.get()) {
            1.toByte() -> innerSer.deser(buf)
            0.toByte() -> null
            else -> throw RuntimeException("Invalid present flag met during optional value deserialization")
        }
    }

    override fun poetize(): String {
        return CodeBlock.of("%T(${innerSer.poetize()})", OptValueSer::class).toString()
    }
}

class VecValueSer<T>(val innerSer: ValueSer<T>) : ValueSer<List<T>> {
    override fun calcSizeBytes(value: List<T>): Int {
        return Leb128.sizeUnsigned(value.size) + value.map { innerSer.calcSizeBytes(it) }.sum()
    }

    override fun ser(buf: ByteBuffer, value: List<T>) {
        Leb128.writeUnsigned(buf, value.size)

        for (entry in value) {
            innerSer.ser(buf, entry)
        }
    }

    override fun deser(buf: ByteBuffer): List<T> {
        val size = Leb128.readUnsigned(buf)
        return (0..size).map { innerSer.deser(buf) }
    }

    override fun poetize(): String {
        return CodeBlock.of("%T(${innerSer.poetize()})", VecValueSer::class).toString()
    }
}

object BlobValueSer : ValueSer<ByteArray> {
    override fun calcSizeBytes(value: ByteArray): Int {
        return Leb128.sizeUnsigned(value.size) + value.size
    }

    override fun ser(buf: ByteBuffer, value: ByteArray) {
        Leb128.writeUnsigned(buf, value.size)

        buf.put(value)
    }

    override fun deser(buf: ByteBuffer): ByteArray {
        val size = Leb128.readUnsigned(buf)
        val bytes = ByteArray(size)
        buf.get(bytes)

        return bytes
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", BlobValueSer::class).toString()
    }
}

typealias RecordSerIntermediate = Map<Int, Any>

val RecordSerIntermediate_typeName =
    Map::class.asClassName().parameterizedBy(Int::class.asTypeName(), Any::class.asTypeName())

class RecordValueSer(val innerSers: Map<Int, ValueSer<Any>>) : ValueSer<RecordSerIntermediate> {
    override fun calcSizeBytes(value: RecordSerIntermediate): Int {
        return innerSers.entries
            .map { (idx, ser) -> ser.calcSizeBytes(value[idx]!!) }
            .sum()
    }

    override fun ser(buf: ByteBuffer, value: RecordSerIntermediate) {
        innerSers.entries.forEach { (idx, ser) ->
            ser.ser(buf, value[idx]!!)
        }
    }

    override fun deser(buf: ByteBuffer): RecordSerIntermediate {
        return innerSers.entries.associate { (idx, ser) ->
            idx to ser.deser(buf)
        }
    }

    override fun poetize(): String {
        val poetizedInnerSers = innerSers.entries.joinToString(prefix = "mapOf(", postfix = ")") { (key, ser) ->
            "$key to ${ser.poetize()}"
        }
        return CodeBlock.of("%T(${poetizedInnerSers})", RecordValueSer::class).toString()
    }
}

typealias VariantSerIntermediate = Pair<Int, Any>

val VariantSerIntermediate_typeName =
    Pair::class.asTypeName().parameterizedBy(Int::class.asTypeName(), Any::class.asTypeName())

class VariantValueSer(val innerSers: Map<Int, ValueSer<Any>>) : ValueSer<VariantSerIntermediate> {
    override fun calcSizeBytes(value: VariantSerIntermediate): Int {
        val (idx, v) = value

        return Leb128.sizeUnsigned(idx) + innerSers[idx]!!.calcSizeBytes(v)
    }

    override fun ser(buf: ByteBuffer, value: VariantSerIntermediate) {
        val (idx, v) = value
        Leb128.writeUnsigned(buf, idx)

        val ser = innerSers[idx]
            ?: throw RuntimeException("Unable to serialize variant: no serializer for idx \"$idx\"")
        ser.ser(buf, v)
    }

    override fun deser(buf: ByteBuffer): VariantSerIntermediate {
        val idx = Leb128.readUnsigned(buf)
        val ser = innerSers[idx]
            ?: throw RuntimeException("Unable to deserialize variant: no serializer for idx \"$idx\"")

        return idx to ser.deser(buf)
    }

    override fun poetize(): String {
        val poetizedInnerSers = innerSers.entries.joinToString(prefix = "mapOf(", postfix = ")") { (idx, ser) ->
            "$idx to ${ser.poetize()}"
        }

        return CodeBlock.of("%T(${poetizedInnerSers})", VariantValueSer::class).toString()
    }
}

object FuncValueSer : ValueSer<SimpleIDLFunc> {
    override fun calcSizeBytes(value: SimpleIDLFunc): Int {
        return if (value.funcName != null && value.service != null)
            Byte.SIZE_BYTES + ServiceValueSer.calcSizeBytes(value.service) + TextValueSer.calcSizeBytes(value.funcName)
        else
            Byte.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: SimpleIDLFunc) {
        if (value.funcName != null && value.service != null) {
            buf.put(1)
            ServiceValueSer.ser(buf, value.service)
            TextValueSer.ser(buf, value.funcName)
        } else
            buf.put(0)
    }

    override fun deser(buf: ByteBuffer): SimpleIDLFunc {
        val (service, funcName) = when (buf.get()) {
            1.toByte() -> Pair(ServiceValueSer.deser(buf), TextValueSer.deser(buf))
            0.toByte() -> Pair(null, null)
            else -> throw RuntimeException("Invalid notOpaque flag met during function deserialization")
        }

        return SimpleIDLFunc(funcName, service)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", FuncValueSer::class).toString()
    }
}

object ServiceValueSer : ValueSer<SimpleIDLService> {
    override fun calcSizeBytes(value: SimpleIDLService): Int {
        return if (value.id != null)
            Byte.SIZE_BYTES + BlobValueSer.calcSizeBytes(value.id)
        else
            Byte.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: SimpleIDLService) {
        if (value.id != null) {
            buf.put(1)
            BlobValueSer.ser(buf, value.id)
        } else
            buf.put(0)
    }

    override fun deser(buf: ByteBuffer): SimpleIDLService {
        val id = when (buf.get()) {
            1.toByte() -> BlobValueSer.deser(buf)
            0.toByte() -> null
            else -> throw RuntimeException("Invalid notOpaque flag met during service deserialization")
        }

        return SimpleIDLService(id)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", ServiceValueSer::class).toString()
    }
}

object PrincipalValueSer : ValueSer<SimpleIDLPrincipal> {
    override fun calcSizeBytes(value: SimpleIDLPrincipal): Int {
        return if (value.id != null)
            Byte.SIZE_BYTES + BlobValueSer.calcSizeBytes(value.id)
        else
            Byte.SIZE_BYTES
    }

    override fun ser(buf: ByteBuffer, value: SimpleIDLPrincipal) {
        if (value.id != null) {
            buf.put(1)
            BlobValueSer.ser(buf, value.id)
        } else
            buf.put(0)
    }

    override fun deser(buf: ByteBuffer): SimpleIDLPrincipal {
        val id = when (buf.get()) {
            1.toByte() -> BlobValueSer.deser(buf)
            0.toByte() -> null
            else -> throw RuntimeException("Invalid notOpaque flag met during principal deserialization")
        }

        return SimpleIDLPrincipal(id)
    }

    override fun poetize(): String {
        return CodeBlock.of("%T", PrincipalValueSer::class).toString()
    }
}

fun getValueSerForType(type: IDLType, typeTable: TypeTable): ValueSer<Any> {
    return when (type) {
        is IDLType.Id -> {
            val labeledType = typeTable.getTypeByLabel(type)
            getValueSerForType(labeledType, typeTable)
        }

        is IDLType.Primitive.Natural -> NatValueSer as ValueSer<Any>
        is IDLType.Primitive.Nat8 -> Nat8ValueSer as ValueSer<Any>
        is IDLType.Primitive.Nat16 -> Nat16ValueSer as ValueSer<Any>
        is IDLType.Primitive.Nat32 -> Nat32ValueSer as ValueSer<Any>
        is IDLType.Primitive.Nat64 -> Nat64ValueSer as ValueSer<Any>

        is IDLType.Primitive.Integer -> IntValueSer as ValueSer<Any>
        is IDLType.Primitive.Int8 -> Int8ValueSer as ValueSer<Any>
        is IDLType.Primitive.Int16 -> Int16ValueSer as ValueSer<Any>
        is IDLType.Primitive.Int32 -> Int32ValueSer as ValueSer<Any>
        is IDLType.Primitive.Int64 -> Int64ValueSer as ValueSer<Any>

        is IDLType.Primitive.Float32 -> Float32ValueSer as ValueSer<Any>
        is IDLType.Primitive.Float64 -> Float64ValueSer as ValueSer<Any>

        is IDLType.Primitive.Bool -> BoolValueSer as ValueSer<Any>
        is IDLType.Primitive.Text -> TextValueSer as ValueSer<Any>
        is IDLType.Primitive.Null -> NullValueSer as ValueSer<Any>
        is IDLType.Primitive.Reserved -> ReservedValueSer as ValueSer<Any>
        is IDLType.Primitive.Empty -> EmptyValueSer as ValueSer<Any>

        is IDLType.Constructive.Opt -> {
            val innerSer = getValueSerForType(type.type, typeTable)
            OptValueSer(innerSer) as ValueSer<Any>
        }
        is IDLType.Constructive.Vec -> {
            val innerSer = getValueSerForType(type.type, typeTable)
            VecValueSer(innerSer) as ValueSer<Any>
        }
        is IDLType.Constructive.Blob -> BlobValueSer as ValueSer<Any>
        is IDLType.Constructive.Record -> {
            val innerSers = type.fields.associate { it.idx to getValueSerForType(it.type, typeTable) }
            RecordValueSer(innerSers) as ValueSer<Any>
        }
        is IDLType.Constructive.Variant -> {
            val innerSers = type.fields.associate { it.idx to getValueSerForType(it.type, typeTable) }
            VariantValueSer(innerSers) as ValueSer<Any>
        }

        is IDLType.Reference.Func -> FuncValueSer as ValueSer<Any>
        is IDLType.Reference.Service -> ServiceValueSer as ValueSer<Any>
        is IDLType.Reference.Principal -> PrincipalValueSer as ValueSer<Any>

        else -> throw java.lang.RuntimeException("Unable to get serializer for type $type")
    }
}