package senior.joinu.candid

class TypeTable {
    // list of types in type table
    val registry = mutableListOf<IDLType>()
    // some types have labels that are used in code
    val labels = mutableMapOf<IDLType.Id, Int>()

    // copies labels from this TT into result TT, if these labels are used by type
    fun copyLabelsForType(type: IDLType, result: TypeTable) {
        when (type) {
            is IDLType.Id -> {
                if (!result.labels.containsKey(type)) {
                    val idx = labels[type]!!
                    val t = registry[idx]
                    result.registerTypeWithLabel(type, t)
                    copyLabelsForType(t, result)
                }
            }
            is IDLType.Constructive -> {
                when (type) {
                    is IDLType.Constructive.Opt -> copyLabelsForType(type.type, result)
                    is IDLType.Constructive.Vec -> copyLabelsForType(type.type, result)
                    is IDLType.Constructive.Record -> {
                        for (field in type.fields) {
                            copyLabelsForType(field.type, result)
                        }
                    }
                    is IDLType.Constructive.Variant -> {
                        for (field in type.fields) {
                            copyLabelsForType(field.type, result)
                        }
                    }
                }
            }
            is IDLType.Reference -> {
                when (type) {
                    is IDLType.Reference.Func -> {
                        for (arg in type.arguments) {
                            copyLabelsForType(arg.type, result)
                        }
                        for (res in type.results) {
                            copyLabelsForType(res.type, result)
                        }
                    }
                    is IDLType.Reference.Service -> {
                        for (method in type.methods) {
                            copyLabelsForType(method.type as IDLType, result)
                        }
                    }
                }
            }
        }
    }

    // registers type in TT if it wasn't registered before
    // returns registry index
    fun registerType(type: IDLType): Int {
        val idx = registry.indexOf(type)

        return if (idx == -1) {
            registry.add(type)
            val newIdx = registry.lastIndex
            type.getTOpcodeList(this)
            newIdx
        } else {
            idx
        }
    }

    // registers type and puts label on it
    // returns registry index
    fun registerTypeWithLabel(label: IDLType.Id, type: IDLType): Int {
        if (labels.containsKey(label)) {
            return labels[label]!!
        }

        val oldIdx = registry.indexOf(type)
        if (oldIdx != -1) {
            labels[label] = oldIdx

            return oldIdx
        }

        val idx = registry.lastIndex + 1
        labels[label] = idx
        registry.add(type)
        type.getTOpcodeList(this)

        return idx
    }

    // return valid opcodes for nested type
    // if primitive - returns standard values (negative)
    // if id - returns registry index for label
    // else - tries to register the type and returns it's index
    fun getNestedTypeRegistryIndex(type: IDLType): List<Opcode> = when (type) {
        is IDLType.Primitive -> type.getTOpcodeList(this)
        is IDLType.Id -> getRegistryIndexByLabel(type)
        else -> Opcode.Integer.listFrom(registerType(type))
    }

    // returns an index which the label points on
    fun getRegistryIndexByLabel(label: IDLType.Id): List<Opcode> {
        return Opcode.Integer.listFrom(labels[label]!!)
    }

    // returns IDLType by label
    fun getTypeByLabel(label: IDLType.Id): IDLType {
        val id = labels[label]!!
        return registry[id]
    }
}

sealed class IDLType {
    abstract fun getTOpcodeList(typeTable: TypeTable): List<Opcode>

    data class Id(override val value: String) : IDLType(), IDLMethodType, IDLTextToken, IDLActorType {
        override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
            return typeTable.getRegistryIndexByLabel(this)
        }
    }

    sealed class Reference : IDLType() {
        data class Func(
            val arguments: List<IDLArgType>,
            val results: List<IDLArgType>,
            val annotations: List<IDLFuncAnn>
        ) : Reference(), IDLMethodType {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                val funcOpcode = Opcode.Integer.listFrom(IDLOpcode.FUNC)
                val argsOpcode = Opcode.Integer.listFrom(arguments.size, OpcodeEncoding.LEB) +
                        arguments
                            .map { arg -> arg.getOpcodeList(typeTable) }
                            .flatten()
                val resultsOpcode = Opcode.Integer.listFrom(results.size, OpcodeEncoding.LEB) +
                        results
                            .map { arg -> arg.getOpcodeList(typeTable) }
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
        }

        data class Service(val methods: List<IDLMethod>) : Reference(), IDLActorType {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.SERVICE) +
                        Opcode.Integer.listFrom(methods.size, OpcodeEncoding.LEB) +
                        methods
                            .map { method ->
                                Opcode.Integer.listFrom(method.name.length, OpcodeEncoding.LEB) +
                                        Opcode.Utf8.listFrom(method.name) +
                                        (method.type as IDLType).getTOpcodeList(typeTable)
                            }
                            .flatten()
            }
        }

        object Principal : Reference() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.PRINCIPAL)
            }

            override fun toString() = "Principal"
        }
    }

    sealed class Constructive : IDLType() {
        data class Opt(val type: IDLType) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.OPT) + typeTable.getNestedTypeRegistryIndex(type)
            }
        }

        data class Vec(val type: IDLType) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.VEC) + typeTable.getNestedTypeRegistryIndex(type)
            }
        }

        object Blob : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.VEC) +
                    Primitive.Nat8.getTOpcodeList(typeTable)

            override fun toString() = "Blob"
        }

        data class Record(val fields: List<IDLFieldType>) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.RECORD) +
                        Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                        fields
                            .mapIndexed { idx, field -> if (field.name != null) idlHash(field.name) to field else idx to field }
                            .sortedBy { it.first }
                            .map { (id, field) -> field.getOpcodeList(id, typeTable) }
                            .flatten()
            }
        }

        data class Variant(val fields: List<IDLFieldType>) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.VARIANT) +
                        Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                        fields
                            .mapIndexed { idx, field -> if (field.name != null) idlHash(field.name) to field else idx to field }
                            .sortedBy { it.first }
                            .map { (id, field) -> field.getOpcodeList(id, typeTable) }
                            .flatten()
            }
        }
    }

    sealed class Primitive : IDLType() {
        object Natural : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT)

            override fun toString() = "Nat"
        }

        object Nat8 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT8)

            override fun toString() = "Nat8"
        }

        object Nat16 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT16)

            override fun toString() = "Nat16"
        }

        object Nat32 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT32)

            override fun toString() = "Nat32"
        }

        object Nat64 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT64)

            override fun toString() = "Nat64"
        }

        object Integer : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT)

            override fun toString() = "Int"
        }

        object Int8 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT8)

            override fun toString() = "Int8"
        }

        object Int16 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT16)

            override fun toString() = "Int16"
        }

        object Int32 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT32)

            override fun toString() = "Int32"
        }

        object Int64 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT64)

            override fun toString() = "Int64"
        }

        object Float32 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.FLOAT32)

            override fun toString() = "Float32"
        }

        object Float64 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.FLOAT64)

            override fun toString() = "Float64"
        }

        object Bool : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.BOOL)

            override fun toString() = "Bool"
        }

        object Text : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.TEXT)

            override fun toString() = "Text"
        }

        object Null : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NULL)

            override fun toString() = "Null"
        }

        object Reserved : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.RESERVED)

            override fun toString() = "Reserved"
        }

        object Empty : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.EMPTY)
            override fun toString() = "Empty"
        }
    }
}

data class IDLFieldType(val name: String?, val type: IDLType) {
    fun getOpcodeList(idx: Int, typeTable: TypeTable) = Opcode.Integer.listFrom(idx, OpcodeEncoding.LEB) +
            typeTable.getNestedTypeRegistryIndex(type)
}

data class IDLArgType(val name: String?, val type: IDLType) {
    fun getOpcodeList(typeTable: TypeTable) = typeTable.getNestedTypeRegistryIndex(type)
}

enum class IDLFuncAnn {
    Oneway, Query
}

data class IDLMethod(val name: String, val type: IDLMethodType)
sealed class IDLDef {
    data class Type(val name: String, val type: IDLType) : IDLDef()
    data class Import(val filePath: String) : IDLDef()
}

data class IDLActorDef(val name: String?, val type: IDLActorType)
data class IDLProgram(val imports: List<IDLDef.Import>, val types: List<IDLDef.Type>, val actor: IDLActorDef?)

interface IDLMethodType

interface IDLActorType