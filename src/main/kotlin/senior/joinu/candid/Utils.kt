package senior.joinu.candid

import senior.joinu.leb128.Leb128
import java.math.BigInteger
import java.nio.ByteBuffer

val twoPowThirtyTwo: BigInteger = BigInteger.valueOf(2).pow(32)

fun idlHash(id: String): BigInteger {
    val result = id.foldIndexed(BigInteger.ZERO) { idx, acc, char ->
        acc + char.toLong().toBigInteger() * BigInteger.valueOf(223).pow((id.length - 1) - idx)
    }

    return result.mod(twoPowThirtyTwo)
}

enum class EIDLType(val value: Int) {
    Null(-1),
    Bool(-2),
    Natural(-3),
    Integer(-4),
    Nat8(-5),
    Nat16(-6),
    Nat32(-7),
    Nat64(-8),
    Int8(-9),
    Int16(-10),
    Int32(-11),
    Int64(-12),
    Float32(-13),
    Float64(-14),
    Text(-15),
    Reserved(-16),
    Empty(-17),
    Opt(-18),
    Vec(-19),
    Record(-20),
    Variant(-21),
    Func(-22),
    Service(-23),
    Principal(-24)
}

fun idlSerializeTypeTag(buf: ByteBuffer, value: EIDLType) {
    Leb128.writeSignedLeb128(buf, value.value)
}

fun idlSerializeNat(buf: ByteBuffer, value: BigInteger) {

}
