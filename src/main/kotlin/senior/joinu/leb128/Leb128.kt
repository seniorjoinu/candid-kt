/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package senior.joinu.leb128

import java.lang.RuntimeException
import java.nio.ByteBuffer


/**
 * Reads and writes DWARFv3 LEB 128 signed and unsigned integers. See DWARF v3
 * section 7.6.
 */
object Leb128 {
    /**
     * Gets the number of bytes in the unsigned LEB128 encoding of the
     * given value.
     *
     * @param value the value in question
     * @return its write size, in bytes
     */
    fun unsignedLeb128Size(value: Int): Int {
        // TODO: This could be much cleverer.
        var remaining = value shr 7
        var count = 0
        while (remaining != 0) {
            remaining = remaining shr 7
            count++
        }
        return count + 1
    }

    /**
     * Gets the number of bytes in the signed LEB128 encoding of the
     * given value.
     *
     * @param value the value in question
     * @return its write size, in bytes
     */
    fun signedLeb128Size(value: Int): Int {
        // TODO: This could be much cleverer.
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

    /**
     * Reads an signed integer from `in`.
     */
    fun readSignedLeb128(input: ByteBuffer): Int {
        var result = 0
        var cur: Int
        var count = 0
        var signBits = -1
        do {
            cur = input.int and 0xff
            result = result or (cur and 0x7f shl count * 7)
            signBits = signBits shl 7
            count++
        } while (cur and 0x80 == 0x80 && count < 5)
        if (cur and 0x80 == 0x80) {
            throw Leb128Exception("invalid LEB128 sequence")
        }

        // Sign extend if appropriate
        if (signBits shr 1 and result != 0) {
            result = result or signBits
        }
        return result
    }

    /**
     * Reads an unsigned integer from `in`.
     */
    fun readUnsignedLeb128(input: ByteBuffer): Int {
        var result = 0
        var cur: Int
        var count = 0
        do {
            cur = input.int and 0xff
            result = result or (cur and 0x7f shl count * 7)
            count++
        } while (cur and 0x80 == 0x80 && count < 5)
        if (cur and 0x80 == 0x80) {
            throw Leb128Exception("invalid LEB128 sequence")
        }
        return result
    }

    /**
     * Writes `value` as an unsigned integer to `out`, starting at
     * `offset`. Returns the number of bytes written.
     */
    fun writeUnsignedLeb128(out: ByteBuffer, value: Int) {
        var value = value
        var remaining = value ushr 7
        while (remaining != 0) {
            out.put((value and 0x7f or 0x80).toByte())
            value = remaining
            remaining = remaining ushr 7
        }
        out.put((value and 0x7f).toByte())
    }

    /**
     * Writes `value` as a signed integer to `out`, starting at
     * `offset`. Returns the number of bytes written.
     */
    fun writeSignedLeb128(out: ByteBuffer, value: Int) {
        var value = value
        var remaining = value shr 7
        var hasMore = true
        val end = if (value and Int.MIN_VALUE == 0) 0 else -1
        while (hasMore) {
            hasMore = (remaining != end
                    || remaining and 1 != value shr 6 and 1)
            out.put((value and 0x7f or if (hasMore) 0x80 else 0).toByte())
            value = remaining
            remaining = remaining shr 7
        }
    }
}

class Leb128Exception(m: String) : RuntimeException(m)