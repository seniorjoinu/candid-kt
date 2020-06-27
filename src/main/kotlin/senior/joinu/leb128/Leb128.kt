package senior.joinu.leb128

import senior.joinu.candid.reverseOrder
import senior.joinu.candid.toBytesLE
import senior.joinu.candid.toUBytesLE
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or


object Leb128 {
    private const val continuationBit = (1 shl 7).toByte()
    private const val signBit = (1 shl 6).toByte()

    private fun lowBitsOfByte(byte: Byte): Byte {
        return byte and continuationBit.unaryMinus().toByte()
    }

    private fun lowBitsOfLong(long: Long): Byte {
        val byte = (long and Byte.MAX_VALUE.toLong()).toByte()
        return lowBitsOfByte(byte)
    }

    fun readUnsigned(buf: ByteBuffer): Long {
        var result = 0L
        var shift = 0

        while (true) {
            val byte = buf.get()

            if (shift == 63 && byte != 0x00.toByte() && byte != 0x01.toByte()) {
                throw RuntimeException("Leb128 overflow")
            }

            val lowBits = lowBitsOfByte(byte).toLong()
            result = result or (lowBits shl shift)

            if (byte and continuationBit == 0.toByte()) {
                return result
            }

            shift += 7
        }
    }

    fun readSigned(buf: ByteBuffer): Long {
        var result = 0L
        var shift = 0
        val size = 64
        var byte: Byte

        while (true) {
            byte = buf.get()

            if (shift == 63 && byte != 0x00.toByte() && byte != 0x7f.toByte()) {
                throw RuntimeException("Leb128 overflow")
            }

            val lowBits = lowBitsOfByte(byte).toLong()
            result = result or (lowBits shl shift)
            shift += 7

            if (byte and continuationBit == 0.toByte()) {
                break
            }
        }

        if (shift < size && (signBit and byte) == signBit) {
            result = result or (0.toByte().unaryMinus() shl shift).toLong()
        }

        return result
    }

    fun writeUnsigned(buf: ByteBuffer, _value: Long) {
        var value = _value

        while (true) {
            var byte = lowBitsOfLong(value)
            value = value shr 7

            if (value != 0L) {
                byte = byte or continuationBit
            }

            buf.put(byte)

            if (value == 0L) {
                break
            }
        }
    }

    fun sizeUnsigned(_value: Long): Int {
        var value = _value
        var size = 0

        while (true) {
            value = value shr 7

            size++

            if (value == 0L) {
                return size
            }
        }
    }

    fun writeSigned(buf: ByteBuffer, _value: Long) {
        var value = _value

        while (true) {
            var byte = value.toByte()

            value = value shr 6
            val done = value == 0L || value == -1L
            if (done) {
                byte = byte and continuationBit.unaryMinus().toByte()
            } else {
                value = value shr 1
                byte = byte or continuationBit
            }

            buf.put(byte)

            if (done) {
                break
            }
        }
    }

    fun sizeSigned(_value: Long): Int {
        var value = _value
        var size = 0

        while (true) {
            value = value shr 6
            val done = value == 0L || value == -1L
            if (!done) {
                value = value shr 1
            }

            size++

            if (done) {
                return size
            }
        }
    }
}

const val nineBits = 0xff
const val eightBits = 0x80
const val sevenBits = 0x7f

object Leb128BI {
    fun writeNat(buf: ByteBuffer, nat: BigInteger) {
        var value = if (nat < BigInteger.ZERO) nat.negate() else nat

        while (true) {
            val bigByte = value and BigInteger.valueOf(sevenBits.toLong())
            var byte = bigByte.toUBytesLE().first()

            value = value shr 7

            if (value != BigInteger.ZERO) {
                byte = byte or eightBits.toByte()
            }

            buf.put(byte)

            if (value == BigInteger.ZERO) {
                break
            }
        }
    }

    fun sizeNat(nat: BigInteger): Int {
        var value = if (nat < BigInteger.ZERO) nat.negate() else nat
        var result = 0

        while (true) {
            val bigByte = value and BigInteger.valueOf(sevenBits.toLong())
            var byte = bigByte.toUBytesLE().first()

            value = value shr 7

            if (value != BigInteger.ZERO) {
                byte = byte or eightBits.toByte()
            }

            result++

            if (value == BigInteger.ZERO) {
                break
            }
        }

        return result
    }

    fun readNat(buf: ByteBuffer): BigInteger {
        var result = BigInteger.ZERO
        var shift = 0

        while (true) {
            var byte = buf.get()
            byte = byte and sevenBits.toByte()

            val lowBits = BigInteger(1, ByteArray(1) { byte })

            result = result or (lowBits shl shift)

            if (byte and eightBits.toByte() == 0.toByte()) {
                return result.reverseOrder()
            }

            shift += 7
        }
    }

    fun writeInt(buf: ByteBuffer, int: BigInteger) {
        var value = int

        while (true) {
            val bigByte = int and BigInteger.valueOf(nineBits.toLong())
            var byte = bigByte.toBytesLE().first()

            value = value shr 6

            val done = value == BigInteger.ZERO || value == BigInteger.valueOf(-1)
            if (done) {
                byte = byte and sevenBits.toByte()
                buf.put(byte)

                break
            }

            value = value shr 1
            byte = byte or eightBits.toByte()
            buf.put(byte)
        }
    }

    fun sizeInt(int: BigInteger): Int {
        var value = int
        var result = 0

        while (true) {
            val bigByte = int and BigInteger.valueOf(nineBits.toLong())
            var byte = bigByte.toBytesLE().first()

            value = value shr 6

            val done = value == BigInteger.ZERO || value == BigInteger.valueOf(-1)
            if (done) {
                byte = byte and sevenBits.toByte()
                result++

                break
            }

            value = value shr 1
            byte = byte or eightBits.toByte()
            result++
        }

        return result
    }

    fun readInt(buf: ByteBuffer): BigInteger {
        var result = BigInteger.ZERO
        var shift = 0
        var byte: Byte

        while (true) {
            byte = buf.get()
            byte = byte and sevenBits.toByte()

            val lowBits = BigInteger(ByteArray(1) { byte })

            result = result or (lowBits shl shift)
            shift += 7

            if (byte and eightBits.toByte() == 0.toByte()) {
                break
            }
        }

        if (shift % 8 != 0 && ((0x40).toByte() and byte) == (0x40).toByte()) {
            result = result or BigInteger.valueOf(-1) shl shift
        }

        return result.reverseOrder()
    }
}