package senior.joinu.candid.serialize

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import senior.joinu.candid.TypeTable
import java.nio.ByteBuffer

interface Ser {
    fun poetize(): String
}

interface IDLSerializable<S> {
    fun serialize(buf: ByteBuffer, valueSer: ValueSer<S>, typeTable: TypeTable)
    fun sizeBytes(typeTable: TypeTable): Int
}

interface IDLDeserializable<S, T> {
    fun deserialize(buf: ByteBuffer, valueSer: ValueSer<S>): T
}

interface IDLRecord {
    var fields: RecordSerIntermediate
    val companion: IDLRecordCompanion
}

interface IDLRecordCompanion {
    fun from(fields: RecordSerIntermediate): IDLRecord
}

typealias IDLVariantCompanionType = IDLVariantCompanion<out Any, out IDLVariant<out Any>>

val IDLVariantCompanionType_typeName = IDLVariantCompanion::class.asTypeName().parameterizedBy(
    WildcardTypeName.producerOf(Any::class),
    WildcardTypeName.producerOf(IDLVariant::class.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Any::class)))
)

interface IDLVariantSuperCompanion {
    val variants: Map<Int, IDLVariantCompanionType>
}

interface IDLVariant<T> {
    val idx: Int
    val value: T
}

interface IDLVariantCompanion<T, V : IDLVariant<T>> {
    fun from(value: T): V
}