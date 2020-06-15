package senior.joinu.candid.serialize

open class Record : IDLRecord {
    final override lateinit var fields: RecordSerIntermediate
    override val companion = Companion

    constructor(fields: RecordSerIntermediate) {
        this.fields = fields
    }

    constructor(field1: Int, field2: String) {
        this.fields = mapOf(0 to field1, 1 to field2)
    }

    val field1 by lazy { fields[0] as Int }

    val field2 by lazy { fields[1] as String }

    companion object : IDLRecordCompanion {
        override fun from(fields: RecordSerIntermediate): IDLRecord {
            return Record(fields)
        }
    }
}


sealed class VariantSuper : IDLVariantSuper {
    class VariantA(override val value: Int) : VariantSuper(), IDLVariant<Int> {
        override val idx = 0
        override val companion = Companion

        companion object : IDLVariantCompanion<Int, VariantA> {
            override fun from(value: Int): VariantA {
                return VariantA(value)
            }
        }
    }

    class VariantB(override val value: Record) : VariantSuper(), IDLVariant<Record> {
        override val idx = 1
        override val companion = Companion

        companion object : IDLVariantCompanion<Record, VariantB> {
            override fun from(value: Record): VariantB {
                return VariantB(value)
            }
        }
    }

    override val superCompanion: IDLVariantSuperCompanion = Companion

    companion object : IDLVariantSuperCompanion {
        override val variants = mapOf(0 to VariantA.Companion, 1 to VariantB.Companion)
    }
}

// TODO: design func, service and principal then implement transpilers for them