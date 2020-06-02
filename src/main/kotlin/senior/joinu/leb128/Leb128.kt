package senior.joinu.leb128

import senior.joinu.candid.reverseOrder
import senior.joinu.candid.toBytesLE
import senior.joinu.candid.toUBytesLE
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or


const val nineBits = 0xff
const val eightBits = 0x80
const val sevenBits = 0x7f

object Leb128 {
    fun sizeUnsigned(value: Int): Int {
        var remaining = value shr 7
        var count = 0
        while (remaining != 0) {
            remaining = remaining shr 7
            count++
        }
        return count + 1
    }

    fun sizeSigned(value: Int): Int {
        var value = value
        var remaining = value shr 7
        var count = 0
        var hasMore = true
        val end = if (value and Int.MIN_VALUE == 0) 0 else -1
        while (hasMore) {
            hasMore = (remaining != end
                    || remaining and 1 != value shr 6 and 1)
            value = remaining
            remaining = remaining shr 7
            count++
        }
        return count
    }

    fun readSigned(buf: ByteBuffer): Int {
        var result = 0
        var cur: Int
        var count = 0
        var signBits = -1
        do {
            cur = buf.int and nineBits
            result = result or (cur and sevenBits shl count * 7)
            signBits = signBits shl 7
            count++
        } while (cur and eightBits == eightBits && count < 5)
        if (cur and eightBits == eightBits) {
            throw Leb128Exception("invalid LEB128 sequence")
        }

        // Sign extend if appropriate
        if (signBits shr 1 and result != 0) {
            result = result or signBits
        }
        return result
    }

    fun readUnsigned(buf: ByteBuffer): UInt {
        var result = 0u
        var cur: UInt
        var count = 0u
        do {
            cur = buf.int.toUInt() and nineBits.toUInt()
            result = result or (cur and sevenBits.toUInt() shl (count * 7u).toInt())
            count++
        } while (cur and eightBits.toUInt() == eightBits.toUInt() && count < 5u)
        if (cur and eightBits.toUInt() == eightBits.toUInt()) {
            throw Leb128Exception("invalid LEB128 sequence")
        }
        return result
    }

    fun writeUnsigned(buf: ByteBuffer, nat: UInt) {
        var value = nat

        var remaining = value shr 7
        while (remaining != 0u) {
            buf.put((value and sevenBits.toUInt() or eightBits.toUInt()).toByte())
            value = remaining
            remaining = remaining shr 7
        }
        buf.put((value and sevenBits.toUInt()).toByte())
    }

    fun writeSigned(buf: ByteBuffer, int: Int) {
        var value = int

        var remaining = value shr 7
        var hasMore = true
        val end = if (value and Int.MIN_VALUE == 0) 0 else -1
        while (hasMore) {
            hasMore = (remaining != end
                    || remaining and 1 != value shr 6 and 1)
            buf.put((value and 0x7f or if (hasMore) 0x80 else 0).toByte())
            value = remaining
            remaining = remaining shr 7
        }
    }
}

class Leb128Exception(m: String) : RuntimeException(m)


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