package senior.joinu.candid.utils


import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or


object Leb128 {
    fun sizeSigned(value: Int): Int {
        var size = 0
        var value = value
        var remaining = value shr 7
        var hasMore = true
        val end = if (value and Int.MIN_VALUE == 0) 0 else -1
        while (hasMore) {
            hasMore = (remaining != end
                    || remaining and 1 != value shr 6 and 1)
            size++
            value = remaining
            remaining = remaining shr 7
        }

        return size
    }

    fun sizeUnsigned(value: Int): Int {
        var size = 0
        var remaining = value ushr 7
        while (remaining != 0) {
            size++
            remaining = remaining ushr 7
        }
        return ++size
    }

    fun readSigned(buf: ByteBuffer): Int {
        var result = 0
        var cur: Byte
        var count = 0
        var signBits = -1
        do {
            cur = buf.get() and 0xff.toByte()
            result = result or ((cur and 0x7f).toInt() shl count * 7)
            signBits = signBits shl 7
            count++
        } while (cur and 0x80.toByte() == 0x80.toByte() && count < 5)
        if (cur and 0x80.toByte() == 0x80.toByte()) {
            throw Leb128Exception("invalid LEB128 sequence")
        }
        // Sign extend if appropriate
        if (signBits shr 1 and result != 0) {
            result = result or signBits
        }
        return result
    }

    fun readUnsigned(buf: ByteBuffer): Int {
        var result = 0
        var cur: Byte
        var count = 0
        do {
            cur = buf.get() and 0xff.toByte()
            result = result or ((cur and 0x7f).toInt() shl count * 7)
            count++
        } while (cur and 0x80.toByte() == 0x80.toByte() && count < 5)
        if (cur and 0x80.toByte() == 0x80.toByte()) {
            throw Leb128Exception("invalid LEB128 sequence")
        }
        return result
    }

    fun writeUnsigned(buf: ByteBuffer, value: Int) {
        var value = value
        var remaining = value ushr 7
        while (remaining != 0) {
            buf.put((value and 0x7f or 0x80).toByte())
            value = remaining
            remaining = remaining ushr 7
        }
        buf.put((value and 0x7f).toByte())
    }

    fun writeSigned(buf: ByteBuffer, value: Int) {
        var value = value
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
            val bigByte = value and BigInteger.valueOf(0x7f)
            var byte = bigByte.toByte()

            value = value shr 7

            if (value != BigInteger.ZERO) {
                byte = byte or 0x80.toByte()
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
            value = value shr 7

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
            byte = byte and 0x7f.toByte()

            val lowBits = BigInteger(byteArrayOf(byte))

            result = result or (lowBits shl shift)

            if (byte and 0x80.toByte() == 0.toByte()) {
                return result
            }

            shift += 7
        }
    }

    fun writeInt(buf: ByteBuffer, int: BigInteger) {
        var value = int

        while (true) {
            val bigByte = int and BigInteger.valueOf(0xff)
            var byte = bigByte.toByte()

            value = value shr 6

            val done = value == BigInteger.ZERO || value == BigInteger.valueOf(-1)
            if (done) {
                byte = byte and 0x7f.toByte()
                buf.put(byte)

                break
            }

            value = value shr 1
            byte = byte or 0x80.toByte()
            buf.put(byte)
        }
    }

    fun sizeInt(int: BigInteger): Int {
        var value = int
        var result = 0

        while (true) {
            value = value shr 6

            val done = value == BigInteger.ZERO || value == BigInteger.valueOf(-1)
            if (done) {
                result++

                break
            }

            value = value shr 1
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
            byte = byte and 0x7f.toByte()

            val lowBits = BigInteger(byteArrayOf(byte))

            result = result or (lowBits shl shift)
            shift += 7

            if (byte and 0x80.toByte() == 0.toByte()) {
                break
            }
        }

        if (0x40.toByte() and byte == 0x40.toByte()) {
            result = result or (BigInteger.valueOf(-1) shl shift)
        }

        return result
    }
}
