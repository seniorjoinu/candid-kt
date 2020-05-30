package senior.joinu.candid

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

sealed class Opcode {
    data class Integer(val value: Int, val encoding: OpcodeEncoding = OpcodeEncoding.SLEB) : Opcode() {
        companion object {
            fun listFrom(value: Int, encoding: OpcodeEncoding = OpcodeEncoding.SLEB) = listOf(
                Integer(value, encoding)
            )
            fun listFrom(opcode: IDLOpcode, encoding: OpcodeEncoding = OpcodeEncoding.SLEB) =
                listOf(Integer(opcode.value, encoding))
        }
    }

    data class Utf8(val value: String, val encoding: OpcodeEncoding) : Opcode() {
        companion object {
            fun listFrom(value: String, encoding: OpcodeEncoding = OpcodeEncoding.LEB) = listOf(
                Utf8(value, encoding)
            )
        }
    }
}

enum class OpcodeEncoding {
    LEB, SLEB, BYTE
}