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

// [senior.joinu] - Everything below is from https://github.com/square/kotlinpoet/blob/master/kotlinpoet/src/main/java/com/squareup/kotlinpoet/Util.kt
// for some reason these functions are internal there :c

// https://github.com/JetBrains/kotlin/blob/master/core/descriptors/src/org/jetbrains/kotlin/renderer/KeywordStringsGenerated.java
private val KEYWORDS = setOf(
    "package",
    "as",
    "typealias",
    "class",
    "this",
    "super",
    "val",
    "var",
    "fun",
    "for",
    "null",
    "true",
    "false",
    "is",
    "in",
    "throw",
    "return",
    "break",
    "continue",
    "object",
    "if",
    "try",
    "else",
    "while",
    "do",
    "when",
    "interface",
    "typeof"
)

private const val ALLOWED_CHARACTER = '$'

internal val String.isKeyword get() = this in KEYWORDS

internal val String.hasAllowedCharacters get() = this.any { it == ALLOWED_CHARACTER }

// https://github.com/JetBrains/kotlin/blob/master/compiler/frontend.java/src/org/jetbrains/kotlin/resolve/jvm/checkers/JvmSimpleNameBacktickChecker.kt
private val ILLEGAL_CHARACTERS_TO_ESCAPE = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

private fun String.failIfEscapeInvalid() {
    require(!any { it in ILLEGAL_CHARACTERS_TO_ESCAPE }) {
        "Can't escape identifier $this because it contains illegal characters: " +
                ILLEGAL_CHARACTERS_TO_ESCAPE.intersect(this.toSet()).joinToString("") }
}

internal fun String.escapeIfNecessary(validate: Boolean = true): String {
    val escapedString = escapeIfNotJavaIdentifier().escapeIfKeyword().escapeIfHasAllowedCharacters()
    if (validate) {
        escapedString.failIfEscapeInvalid()
    }
    return escapedString
}

private fun String.alreadyEscaped() = startsWith("`") && endsWith("`")

private fun String.escapeIfKeyword() = if (isKeyword && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfHasAllowedCharacters() = if (hasAllowedCharacters && !alreadyEscaped()) "`$this`" else this

private fun String.escapeIfNotJavaIdentifier(): String {
    return if (!Character.isJavaIdentifierStart(first()) ||
        drop(1).any { !Character.isJavaIdentifierPart(it) } && !alreadyEscaped()) {
        "`$this`"
    } else {
        this
    }
}

internal fun String.escapeSegmentsIfNecessary(delimiter: Char = '.') = split(delimiter)
    .filter { it.isNotEmpty() }
    .joinToString(delimiter.toString()) { it.escapeIfNecessary() }