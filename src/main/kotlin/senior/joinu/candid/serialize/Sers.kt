package senior.joinu.candid.serialize

import java.nio.ByteBuffer

interface Ser {
    fun poetize(): String
}

interface IDLSerializable {
    fun serialize(buf: ByteBuffer)
    fun sizeBytes(): Int
}

interface IDLDeserializable<T> {
    fun deserialize(buf: ByteBuffer): T
}

// TODO: remove current (de)serializing code from transpiler
// TODO: generate sers for function args and results
