package senior.joinu.candid

import senior.joinu.leb128.Leb128
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

enum class IDLOpcode(val value: Int) {
    NULL(-1),
    BOOL(-2),
    NAT(-3),
    INT(-4),
    NAT8(-5),
    NAT16(-6),
    NAT32(-7),
    NAT64(-8),
    INT8(-9),
    INT16(-10),
    INT32(-11),
    INT64(-12),
    FLOAT32(-13),
    FLOAT64(-14),
    TEXT(-15),
    RESERVED(-16),
    EMPTY(-17),
    OPT(-18),
    VEC(-19),
    RECORD(-20),
    VARIANT(-21),
    FUNC(-22),
    SERVICE(-23),
    PRINCIPAL(-24)
}

// encode all opcodes to bytes

sealed class Opcode {
    abstract fun serialize(buf: ByteBuffer)
    abstract fun sizeBytes(): Int

    data class Integer(val value: Int, val encoding: OpcodeEncoding = OpcodeEncoding.SLEB) : Opcode() {
        companion object {
            fun listFrom(value: Int, encoding: OpcodeEncoding = OpcodeEncoding.SLEB) = listOf(
                Integer(value, encoding)
            )
            fun listFrom(opcode: IDLOpcode, encoding: OpcodeEncoding = OpcodeEncoding.SLEB) =
                listOf(Integer(opcode.value, encoding))
        }

        override fun serialize(buf: ByteBuffer) {
            when (encoding) {
                OpcodeEncoding.LEB -> Leb128.writeUnsigned(buf, value.toUInt())
                OpcodeEncoding.SLEB -> Leb128.writeSigned(buf, value)
                OpcodeEncoding.BYTE -> buf.put(value.toByte())
            }
        }

        override fun sizeBytes(): Int {
            return when (encoding) {
                OpcodeEncoding.LEB -> Leb128.sizeUnsigned(value)
                OpcodeEncoding.SLEB -> Leb128.sizeSigned(value)
                OpcodeEncoding.BYTE -> Byte.SIZE_BYTES
            }
        }
    }

    data class Utf8(val value: String) : Opcode() {
        companion object {
            fun listFrom(value: String) = listOf(
                Utf8(value)
            )
        }

        override fun serialize(buf: ByteBuffer) {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            Leb128.writeUnsigned(buf, bytes.size.toUInt())
            buf.put(bytes)
        }

        override fun sizeBytes(): Int {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)

            return Leb128.sizeUnsigned(bytes.size) + bytes.size
        }
    }
}

enum class OpcodeEncoding {
    LEB, SLEB, BYTE
}

val MAGIC_PREFIX = byteArrayOf('D'.toByte(), 'I'.toByte(), 'D'.toByte(), 'L'.toByte())