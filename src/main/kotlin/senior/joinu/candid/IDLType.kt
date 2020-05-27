package senior.joinu.candid


sealed class IDLType {
    companion object {
        val typeTable = mutableListOf<IDLType>()
        val labels = mutableMapOf<Id, Int>()

        fun addTypeDefinitionToTable(type: IDLType): Int {
            return if (typeTable.contains(type)) {
                typeTable.indexOf(type)
            } else {
                typeTable.add(type)
                typeTable.size - 1
            }
        }

        fun addLabeledTypeDefinitionToTable(label: Id, type: IDLType): Int {
            val id = addTypeDefinitionToTable(type)
            labels[label] = id

            return id
        }

        fun getTypeDefinitionId(type: IDLType): Int {
            val id = typeTable.indexOf(type)

            if (id == -1) throw RuntimeException("Type $type is not in type table!")
            return id
        }

        fun getTypeDefinitionIdByLabel(label: Id): Int {
            return labels[label] ?: throw RuntimeException("Type $label is not in type table!")
        }

        fun getTypeDefinitionById(id: Int): IDLType {
            return typeTable.getOrNull(id) ?: throw RuntimeException("Type with id $id is no in type table!")
        }

        fun getTypeLabelById(id: Int): Id {
            return labels.entries.find { it.value == id }?.key
                ?: throw RuntimeException("Label for type with id $id does not exist!")
        }
    }

    abstract val opcodeList: List<Opcode>

    data class Id(override val value: String) : IDLType(), IDLMethodType,
        IDLTextToken, IDLActorType {
        override val opcodeList: List<Opcode> by lazy { Opcode.Integer.listFrom(getTypeDefinitionIdByLabel(this)) }

        override fun computeOpcodeList() = opcodeList
    }

    sealed class Reference : IDLType() {
        data class Func(
            val arguments: List<IDLArgType>,
            val results: List<IDLArgType>,
            val annotations: List<IDLFuncAnn>
        ) : Reference(), IDLMethodType {
            override val opcodeList: List<Opcode>
                get() {
                    val funcOpcode = Opcode.Integer.listFrom(-22)
                    val argsOpcode = Opcode.Integer.listFrom(arguments.size, OpcodeEncoding.LEB) +
                            arguments
                                .map { arg -> arg.getOpcodeList() }
                                .flatten()
                    val resultsOpcode = Opcode.Integer.listFrom(results.size, OpcodeEncoding.LEB) +
                            results
                                .map { arg -> arg.getOpcodeList() }
                                .flatten()
                    val annotationsOpcode = Opcode.Integer.listFrom(annotations.size, OpcodeEncoding.LEB) +
                            annotations
                                .map {
                                    when (it) {
                                        IDLFuncAnn.Query -> Opcode.Integer.listFrom(1, OpcodeEncoding.BYTE)
                                        IDLFuncAnn.Oneway -> Opcode.Integer.listFrom(2, OpcodeEncoding.BYTE)
                                    }
                                }
                                .flatten()

                    return funcOpcode + argsOpcode + resultsOpcode + annotationsOpcode
                }

            override fun computeOpcodeList() = opcodeList
        }

        data class Service(val methods: List<IDLMethod>) : Reference(), IDLActorType {
            override val opcodeList = Opcode.Integer.listFrom(-23) +
                    Opcode.Integer.listFrom(methods.size, OpcodeEncoding.LEB) +
                    methods
                        .map { method ->
                            Opcode.Integer.listFrom(method.name.length, OpcodeEncoding.LEB) +
                                    Opcode.Utf8.listFrom(method.name) +
                                    method.type.computeOpcodeList()
                        }
                        .flatten()
        }

        object Principal : Reference() {
            override val opcodeList = Opcode.Integer.listFrom(-24)

            override fun toString() = "Principal"
        }
    }

    sealed class Constructive : IDLType() {
        data class Opt(val type: IDLType) : Constructive() {
            override val opcodeList: List<Opcode>
                get() {
                    val id = addTypeDefinitionToTable(type)
                    return Opcode.Integer.listFrom(-18) + Opcode.Integer.listFrom(id)
                }
        }

        data class Vec(val type: IDLType) : Constructive() {
            override val opcodeList: List<Opcode>
                get() {
                    val id = addTypeDefinitionToTable(type)
                    return Opcode.Integer.listFrom(-19) + Opcode.Integer.listFrom(id)
                }
        }

        data class Record(val fields: List<IDLFieldType>) : Constructive() {
            override val opcodeList = Opcode.Integer.listFrom(-20) +
                    Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                    fields
                        .mapIndexed { idx, field -> field.getOpcodeList(idx) }
                        .flatten()
        }

        data class Variant(val fields: List<IDLFieldType>) : Constructive() {
            override val opcodeList = Opcode.Integer.listFrom(-21) +
                    Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                    fields
                        .mapIndexed { idx, field -> field.getOpcodeList(idx) }
                        .flatten()
        }
    }

    sealed class Primitive : IDLType() {
        object Natural : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-3)

            override fun toString() = "Nat"
        }

        object Nat8 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-5)

            override fun toString() = "Nat8"
        }

        object Nat16 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-6)

            override fun toString() = "Nat16"
        }

        object Nat32 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-7)

            override fun toString() = "Nat32"
        }

        object Nat64 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-8)

            override fun toString() = "Nat64"
        }

        object Integer : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-4)

            override fun toString() = "Int"
        }

        object Int8 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-9)

            override fun toString() = "Int8"
        }

        object Int16 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-10)

            override fun toString() = "Int16"
        }

        object Int32 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-11)

            override fun toString() = "Int32"
        }

        object Int64 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-12)

            override fun toString() = "Int64"
        }

        object Float32 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-13)

            override fun toString() = "Float32"
        }

        object Float64 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-14)

            override fun toString() = "Float64"
        }

        object Bool : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-2)

            override fun toString() = "Bool"
        }

        object Text : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-15)

            override fun toString() = "Text"
        }

        object Null : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-1)

            override fun toString() = "Null"
        }

        object Reserved : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-16)

            override fun toString() = "Reserved"
        }

        object Empty : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-17)

            override fun toString() = "Empty"
        }

        object Blob : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(-19) + Nat8.opcodeList

            override fun toString() = "Blob"
        }
    }
}

data class IDLFieldType(val name: String?, val type: IDLType) {
    fun getOpcodeList(idx: Int): List<Opcode> {
        val id = IDLType.addTypeDefinitionToTable(type)
        return Opcode.Integer.listFrom(idx, OpcodeEncoding.LEB) + Opcode.Integer.listFrom(id)
    }
}

data class IDLArgType(val name: String?, val type: IDLType) {
    fun getOpcodeList(): List<Opcode> {
        val id = IDLType.addTypeDefinitionToTable(type)
        return Opcode.Integer.listFrom(id)
    }
}

enum class IDLFuncAnn {
    Oneway, Query
}

data class IDLMethod(val name: String, val type: IDLMethodType)
sealed class IDLDef {
    class Type(val name: String, val type: IDLType) : IDLDef()
    class Import(val filePath: String) : IDLDef()
}

data class IDLActorDef(val name: String?, val type: IDLActorType)
data class IDLProgram(val imports: List<IDLDef.Import>, val types: List<IDLDef.Type>, val actor: IDLActorDef?)

interface IDLMethodType {
    fun computeOpcodeList(): List<Opcode>
}

interface IDLActorType

sealed class Opcode {
    data class Integer(val value: Int, val encoding: OpcodeEncoding) : Opcode() {
        companion object {
            fun listFrom(value: Int, encoding: OpcodeEncoding = OpcodeEncoding.SLEB) = listOf(Integer(value, encoding))
        }
    }

    data class Utf8(val value: String, val encoding: OpcodeEncoding) : Opcode() {
        companion object {
            fun listFrom(value: String, encoding: OpcodeEncoding = OpcodeEncoding.LEB) = listOf(Utf8(value, encoding))
        }
    }
}

enum class OpcodeEncoding {
    LEB, SLEB, BYTE
}