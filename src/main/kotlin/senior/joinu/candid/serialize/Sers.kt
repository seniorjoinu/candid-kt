package senior.joinu.candid.serialize

import senior.joinu.candid.idl.TypeTable
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
    fun poetize(): String
}

interface IDLVariant {
    val superCompanion: IDLVariantSuperCompanion
    val idx: Int?
    val value: Any?
}

interface IDLVariantSuperCompanion {
    val variants: Map<Int, IDLVariantCompanion>
    fun poetize(): String
}

interface IDLVariantCompanion {
    fun from(value: Any?): IDLVariant
}
