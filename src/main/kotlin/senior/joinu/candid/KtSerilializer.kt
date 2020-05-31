package senior.joinu.candid

import java.math.BigInteger
import java.nio.ByteBuffer

interface Serializer {
    fun serialize(buf: ByteBuffer)
}

class NaturalSer(val value: BigInteger): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}
class IntegerSer(val value: BigInteger): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}
class Nat8Ser(val value: UByte): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Nat16Ser(val value: UShort): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Nat32Ser(val value: UInt): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Nat64Ser(val value: ULong): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Int8Ser(val value: Byte): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Int16Ser(val value: Short): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Int32Ser(val value: Int): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Int64Ser(val value: Long): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Float32Ser(val value: Float): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class Float64Ser(val value: Double): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class TextSer(val value: String): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class NullSer(val value: Null): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class ReservedSer(val value: Reserved): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class EmptySer(val value: Empty): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class OptSer(val inner: Serializer?): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

class VecSer(val inner: List<Serializer>): Serializer {
    override fun serialize(buf: ByteBuffer) {
        TODO("Not yet implemented")
    }
}

// TODO make any generated RECORD, VARIANT, FUNCTION and SERVICE - Serializer
