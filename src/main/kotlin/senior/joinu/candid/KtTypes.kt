package senior.joinu.candid

data class Principal(override val id: ByteArray?): IDLPrincipal() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Principal

        if (id != null) {
            if (other.id == null) return false
            if (!id.contentEquals(other.id)) return false
        } else if (other.id != null) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.contentHashCode() ?: 0
    }
}

object Null
object Reserved
object Empty