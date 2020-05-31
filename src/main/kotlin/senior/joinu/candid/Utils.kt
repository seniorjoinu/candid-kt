package senior.joinu.candid

import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*


val twoPowThirtyTwo: BigInteger = BigInteger.valueOf(2).pow(32)

fun idlHash(id: String): Int {
    val result = id.foldIndexed(BigInteger.ZERO) { idx, acc, char ->
        acc + char.toLong().toBigInteger() * BigInteger.valueOf(223).pow((id.length - 1) - idx)
    }

    return result.mod(twoPowThirtyTwo).intValueExact()
}

/*
    ByteBuffer should be set to little endian
    buf.order(ByteOrder.LITTLE_ENDIAN)
 */
object IDLSerialize {
    fun magicToken(buf: ByteBuffer) {
        buf
            .putChar('D')
            .putChar('I')
            .putChar('D')
            .putChar('L')
    }
}


fun BigInteger.reverseOrder() = BigInteger(this.toBytesLE())

fun BigInteger.toBytesLE(): ByteArray {
    return this.toByteArray().reversedArray()
}

fun BigInteger.toUBytesLE(): ByteArray {
    val extractedBytes = this.toUBytes()
    return extractedBytes.reversedArray()
}

fun BigInteger.toUBytes(): ByteArray {
    var extractedBytes = this.toByteArray()
    var skipped = 0
    var skip = true
    for (b in extractedBytes) {
        val signByte = b == 0x00.toByte()
        if (skip && signByte) {
            skipped++
            continue
        } else if (skip) {
            skip = false
        }
    }
    extractedBytes = Arrays.copyOfRange(
        extractedBytes, skipped,
        extractedBytes.size
    )
    return extractedBytes
}