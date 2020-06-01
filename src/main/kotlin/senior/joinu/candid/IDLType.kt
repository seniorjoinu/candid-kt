package senior.joinu.candid

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName

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
    abstract fun poetize(): String

    data class Id(override val value: String) : IDLType(), IDLMethodType, IDLTextToken, IDLActorType {
        override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
            return typeTable.getRegistryIndexByLabel(this)
        }

        override fun poetize() = CodeBlock.of("%T(\"$value\")", Id::class.asTypeName()).toString()
    }

    sealed class Reference : IDLType() {
        data class Func(
            val arguments: List<IDLArgType> = emptyList(),
            val results: List<IDLArgType> = emptyList(),
            val annotations: List<IDLFuncAnn> = emptyList()
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

            override fun poetize(): String {
                val poetizedArgs = arguments.joinToString { it.poetize() }
                val poetizedRess = results.joinToString { it.poetize() }
                val poetizedAnns = annotations.joinToString { it.poetize() }

                return CodeBlock
                    .of(
                        "%T(arguments = listOf($poetizedArgs), results = listOf($poetizedRess), annotations = listOf($poetizedAnns))",
                        Func::class.asTypeName()
                    )
                    .toString()
            }
        }

        data class Service(val methods: List<IDLMethod> = emptyList()) : Reference(), IDLActorType {
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

            override fun poetize(): String {
                val poetizedMethods = methods.joinToString { it.poetize() }
                return CodeBlock.of("%T(methods = listOf($poetizedMethods))", Service::class.asTypeName()).toString()
            }
        }

        object Principal : Reference() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.PRINCIPAL)
            }

            override fun toString() = "Principal"

            override fun poetize() = CodeBlock.of("%T", Principal::class.asTypeName()).toString()
        }
    }

    sealed class Constructive : IDLType() {
        data class Opt(val type: IDLType) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.OPT) + typeTable.getNestedTypeRegistryIndex(type)
            }

            override fun poetize() = CodeBlock.of("%T(${type.poetize()})", Opt::class.asTypeName()).toString()
        }

        data class Vec(val type: IDLType) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.VEC) + typeTable.getNestedTypeRegistryIndex(type)
            }

            override fun poetize() = CodeBlock.of("%T(${type.poetize()})", Vec::class.asTypeName()).toString()
        }

        object Blob : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.VEC) +
                    Primitive.Nat8.getTOpcodeList(typeTable)

            override fun toString() = "Blob"

            override fun poetize() = CodeBlock.of("%T", Blob::class.asTypeName()).toString()
        }

        data class Record(val fields: List<IDLFieldType> = emptyList()) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.RECORD) +
                        Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                        fields
                            .mapIndexed { idx, field -> if (field.name != null) idlHash(field.name) to field else idx to field }
                            .sortedBy { it.first }
                            .map { (id, field) -> field.getOpcodeList(id, typeTable) }
                            .flatten()
            }

            override fun poetize(): String {
                val poetizedFields = fields.joinToString { it.poetize() }
                return CodeBlock.of("%T(fields = listOf($poetizedFields))", Record::class.asTypeName()).toString()
            }
        }

        data class Variant(val fields: List<IDLFieldType> = emptyList()) : Constructive() {
            override fun getTOpcodeList(typeTable: TypeTable): List<Opcode> {
                return Opcode.Integer.listFrom(IDLOpcode.VARIANT) +
                        Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                        fields
                            .mapIndexed { idx, field -> if (field.name != null) idlHash(field.name) to field else idx to field }
                            .sortedBy { it.first }
                            .map { (id, field) -> field.getOpcodeList(id, typeTable) }
                            .flatten()
            }

            override fun poetize(): String {
                val poetizedFields = fields.joinToString { it.poetize() }
                return CodeBlock.of("%T(fields = listOf($poetizedFields))", Variant::class.asTypeName()).toString()
            }
        }
    }

    sealed class Primitive : IDLType() {
        object Natural : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT)

            override fun toString() = "Nat"

            override fun poetize() = CodeBlock.of("%T", Natural::class.asTypeName()).toString()
        }

        object Nat8 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT8)

            override fun toString() = "Nat8"

            override fun poetize() = CodeBlock.of("%T", Nat8::class.asTypeName()).toString()
        }

        object Nat16 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT16)

            override fun toString() = "Nat16"

            override fun poetize() = CodeBlock.of("%T", Nat16::class.asTypeName()).toString()
        }

        object Nat32 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT32)

            override fun toString() = "Nat32"

            override fun poetize() = CodeBlock.of("%T", Nat32::class.asTypeName()).toString()
        }

        object Nat64 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NAT64)

            override fun toString() = "Nat64"

            override fun poetize() = CodeBlock.of("%T", Nat64::class.asTypeName()).toString()
        }

        object Integer : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT)

            override fun toString() = "Int"

            override fun poetize() = CodeBlock.of("%T", Integer::class.asTypeName()).toString()
        }

        object Int8 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT8)

            override fun toString() = "Int8"

            override fun poetize() = CodeBlock.of("%T", Int8::class.asTypeName()).toString()
        }

        object Int16 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT16)

            override fun toString() = "Int16"

            override fun poetize() = CodeBlock.of("%T", Int16::class.asTypeName()).toString()
        }

        object Int32 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT32)

            override fun toString() = "Int32"

            override fun poetize() = CodeBlock.of("%T", Int32::class.asTypeName()).toString()
        }

        object Int64 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.INT64)

            override fun toString() = "Int64"

            override fun poetize() = CodeBlock.of("%T", Int64::class.asTypeName()).toString()
        }

        object Float32 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.FLOAT32)

            override fun toString() = "Float32"

            override fun poetize() = CodeBlock.of("%T", Float32::class.asTypeName()).toString()
        }

        object Float64 : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.FLOAT64)

            override fun toString() = "Float64"

            override fun poetize() = CodeBlock.of("%T", Float64::class.asTypeName()).toString()
        }

        object Bool : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.BOOL)

            override fun toString() = "Bool"

            override fun poetize() = CodeBlock.of("%T", Bool::class.asTypeName()).toString()
        }

        object Text : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.TEXT)

            override fun toString() = "Text"

            override fun poetize() = CodeBlock.of("%T", Text::class.asTypeName()).toString()
        }

        object Null : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.NULL)

            override fun toString() = "Null"

            override fun poetize() = CodeBlock.of("%T", Null::class.asTypeName()).toString()
        }

        object Reserved : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.RESERVED)

            override fun toString() = "Reserved"

            override fun poetize() = CodeBlock.of("%T", Reserved::class.asTypeName()).toString()
        }

        object Empty : Primitive() {
            override fun getTOpcodeList(typeTable: TypeTable) = Opcode.Integer.listFrom(IDLOpcode.EMPTY)

            override fun toString() = "Empty"

            override fun poetize() = CodeBlock.of("%T", Empty::class.asTypeName()).toString()
        }
    }
}

data class IDLFieldType(val name: String?, val type: IDLType) {
    fun getOpcodeList(idx: Int, typeTable: TypeTable) = Opcode.Integer.listFrom(idx, OpcodeEncoding.LEB) +
            typeTable.getNestedTypeRegistryIndex(type)

    fun poetize() = CodeBlock.of("%T(\"$name\", ${type.poetize()})", IDLFieldType::class.asTypeName()).toString()
}

data class IDLArgType(val name: String?, val type: IDLType) {
    fun getOpcodeList(typeTable: TypeTable) = typeTable.getNestedTypeRegistryIndex(type)
    fun poetize() = CodeBlock.of("%T(\"$name\", ${type.poetize()})", IDLArgType::class.asTypeName()).toString()
}

enum class IDLFuncAnn {
    Oneway {
        override fun poetize() = CodeBlock.of("%T", Oneway::class.asTypeName()).toString()
    },
    Query {
        override fun poetize() = CodeBlock.of("%T", Query::class.asTypeName()).toString()
    };

    abstract fun poetize(): String
}

data class IDLMethod(val name: String, val type: IDLMethodType) {
    fun poetize() = CodeBlock.of("%T($name, ${(type as IDLType).poetize()})", IDLMethod::class.asTypeName()).toString()
}
sealed class IDLDef {
    data class Type(val name: String, val type: IDLType) : IDLDef()
    data class Import(val filePath: String) : IDLDef()
}

data class IDLActorDef(val name: String?, val type: IDLActorType)
data class IDLProgram(val imports: List<IDLDef.Import>, val types: List<IDLDef.Type>, val actor: IDLActorDef?)

interface IDLMethodType

interface IDLActorType