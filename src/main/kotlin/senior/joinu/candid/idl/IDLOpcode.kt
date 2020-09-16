package senior.joinu.candid.idl

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
    PRINCIPAL(-24);

    companion object {
        fun fromInt(value: Int) = IDLOpcode.values().find { it.value == value }
    }
}

val MAGIC_PREFIX = byteArrayOf('D'.toByte(), 'I'.toByte(), 'D'.toByte(), 'L'.toByte())
