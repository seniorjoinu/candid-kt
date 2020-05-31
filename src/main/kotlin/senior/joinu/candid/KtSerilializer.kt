package senior.joinu.candid

import java.nio.ByteBuffer

interface Serializer {
    fun serialize(buf: ByteBuffer)
}

fun serializeIDLValue(type: IDLType, value: Any?, buf: ByteBuffer) {
    
}

// TODO make any generated RECORD, VARIANT, FUNCTION and SERVICE - Serializer
