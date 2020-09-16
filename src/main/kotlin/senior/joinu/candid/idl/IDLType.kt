package senior.joinu.candid.idl

import com.squareup.kotlinpoet.asTypeName
import senior.joinu.candid.serialize.getTypeSerForType
import senior.joinu.candid.transpile.prettyString
import senior.joinu.candid.utils.CodeBlock
import senior.joinu.candid.utils.Leb128
import java.nio.ByteBuffer

class TypeTable(
    // list of types in type table
    val registry: MutableList<IDLType> = mutableListOf(),
    // some types have labels that are used in code
    val labels: MutableMap<IDLType.Id, Int> = mutableMapOf()
) {
    fun serialize(buf: ByteBuffer) {
        Leb128.writeUnsigned(buf, registry.size)
        registry.forEach { type ->
            val typeSer = getTypeSerForType(type, this)
            typeSer.serType(buf)
        }
    }

    fun sizeBytes(): Int {
        return Leb128.sizeUnsigned(registry.size) + registry
            .map { type ->
                val typeSer = getTypeSerForType(type, this)
                typeSer.calcTypeSizeBytes()
            }
            .sum()
    }

    fun exists(custom: IDLType.Other.Custom): Boolean {
        return registry.size > custom.opcode
    }

    fun poetize(): String {
        val poetizedRegistry = registry.joinToString { it.poetize() }
        val poetizedLabels = labels.entries.joinToString { (key, value) -> "${key.poetize()} to $value" }

        return CodeBlock
            .of(
                "%T(registry = mutableListOf($poetizedRegistry), labels = mutableMapOf($poetizedLabels))",
                TypeTable::class
            )
            .toString()
    }

    // copies labels from this TT into result TT, if these labels are used by type
    fun copyLabelsForType(type: IDLType, result: TypeTable) {
        when (type) {
            is IDLType.Id -> {
                if (!result.labels.containsKey(type)) {
                    val idx = labels[type]!!

                    val t = if (idx < 0) IDLType.Primitive.primitiveByOpcode(IDLOpcode.fromInt(idx)!!)
                    else registry[idx]

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

    fun registerIfNotPrimitiveOrId(type: IDLType) {
        when (type) {
            is IDLType.Id, is IDLType.Primitive, is IDLType.Other -> {
            }
            else -> registerType(type)
        }
    }

    fun registerInnerType(type: IDLType) {
        when (type) {
            is IDLType.Constructive.Opt -> registerIfNotPrimitiveOrId(type.type)
            is IDLType.Constructive.Vec -> registerIfNotPrimitiveOrId(type.type)
            is IDLType.Constructive.Record -> type.fields.forEach { registerIfNotPrimitiveOrId(it.type) }
            is IDLType.Constructive.Variant -> type.fields.forEach { registerIfNotPrimitiveOrId(it.type) }
            is IDLType.Reference.Func -> {
                type.arguments.forEach { registerIfNotPrimitiveOrId(it.type) }
                type.results.forEach { registerIfNotPrimitiveOrId(it.type) }
            }
            is IDLType.Reference.Service -> type.methods.forEach { registerIfNotPrimitiveOrId(it.type as IDLType) }
            else -> {
            }
        }
    }

    // registers type in TT if it wasn't registered before
    // returns registry index
    fun registerType(type: IDLType): Int {
        val idx = registry.indexOf(type)

        return if (idx == -1) {
            registry.add(type)
            registerInnerType(type)
            registry.lastIndex
        } else {
            idx
        }
    }

    // registers type and puts label on it
    // returns registry index
    // ! thread unsafe !
    fun registerTypeWithLabel(label: IDLType.Id, type: IDLType): Int {
        if (labels.containsKey(label)) {
            return labels[label]!!
        }

        val oldIdx = registry.indexOf(type)
        if (oldIdx != -1) {
            labels[label] = oldIdx

            return oldIdx
        }

        if (type is IDLType.Primitive) {
            labels[label] = type.opcode.value

            return type.opcode.value
        }

        val idx = registry.lastIndex + 1
        labels[label] = idx
        registry.add(type)
        registerInnerType(type)

        return idx
    }

    // returns IDLType by label
    fun getTypeByLabel(label: IDLType.Id): IDLType {
        val id = labels[label]!!
        return registry[id]
    }

    fun getIdxByLabel(label: IDLType.Id): Int {
        return labels[label]!!
    }
}

sealed class IDLType {
    abstract fun poetize(): String

    data class Id(override val value: String) : IDLType(), IDLMethodType, IDLName, IDLActorType {
        companion object { const val pattern: String = "[A-Za-z_][A-Za-z0-9_]*" }
        override fun toString() = value
        override fun poetize() = CodeBlock.of("%T(\"$value\")", Id::class).toString()
    }

    sealed class Reference : IDLType() {
        data class Func(
            val arguments: List<IDLArgType> = emptyList(),
            val results: List<IDLArgType> = emptyList(),
            val annotations: List<IDLFuncAnn> = emptyList()
        ) : Reference(), IDLMethodType {
            companion object { const val text = "func" }
            override fun toString() = "$text${arguments.joinToString(", ", "(", ")")} -> ${results.joinToString(", ", "(", ")")}${if (annotations.isEmpty()) "" else annotations.joinToString(", ", " ")}"
            override fun poetize(): String {
                val poetizedArgs = arguments.joinToString { it.poetize() }
                val poetizedRess = results.joinToString { it.poetize() }
                val poetizedAnns = annotations.joinToString { it.poetize() }

                return CodeBlock
                    .of(
                        "%T(arguments = listOf($poetizedArgs), results = listOf($poetizedRess), annotations = listOf($poetizedAnns))",
                        Func::class
                    )
                    .toString()
            }
        }

        data class Service(val methods: List<IDLMethod> = emptyList()) : Reference(), IDLActorType {
            companion object { const val text = "service" }
            override fun toString() = "$text ${prettyString(methods, "")}"
            override fun poetize(): String {
                val poetizedMethods = methods.joinToString { it.poetize() }
                return CodeBlock.of("%T(methods = listOf($poetizedMethods))", Service::class).toString()
            }
        }

        object Principal : Reference() {
            const val text = "principal"
            override fun toString() = text
            override fun poetize() = CodeBlock.of("%T", Principal::class).toString()
        }
    }

    sealed class Constructive : IDLType() {
        data class Opt(val type: IDLType) : Constructive() {
            companion object { const val text = "opt" }
            override fun toString() = "$text $type"
            override fun poetize() = CodeBlock.of("%T(${type.poetize()})", Opt::class).toString()
        }

        data class Vec(val type: IDLType) : Constructive() {
            companion object { const val text = "vec" }
            override fun toString() = "$text $type"
            override fun poetize() = CodeBlock.of("%T(${type.poetize()})", Vec::class).toString()
        }

        object Blob : Constructive() {
            const val text = "blob"
            override fun toString() = text
            override fun poetize() = CodeBlock.of("%T", Blob::class).toString()
        }

        data class Record(val fields: List<IDLFieldType> = emptyList()) : Constructive() {
            companion object { const val text = "record" }
            override fun toString() = "$text ${prettyString(fields, ";")}"
            override fun poetize(): String {
                val poetizedFields = fields.joinToString { it.poetize() }
                return CodeBlock.of("%T(fields = listOf($poetizedFields))", Record::class).toString()
            }
        }

        data class Variant(val fields: List<IDLFieldType> = emptyList()) : Constructive() {
            companion object { const val text = "variant" }
            override fun toString() = "$text ${prettyString(fields, ";")}"
            override fun poetize(): String {
                val poetizedFields = fields.joinToString { it.poetize() }
                return CodeBlock.of("%T(fields = listOf($poetizedFields))", Variant::class).toString()
            }
        }
    }

    sealed class Primitive : IDLType() {
        abstract val opcode: IDLOpcode
        abstract var text: String
        override fun toString() = text
        override fun poetize() = CodeBlock.of("%T", this::class).toString()

        companion object {
            fun primitiveByOpcode(opcode: IDLOpcode): Primitive {
                return when (opcode) {
                    IDLOpcode.EMPTY -> Empty
                    IDLOpcode.RESERVED -> Reserved
                    IDLOpcode.FLOAT64 -> Float64
                    IDLOpcode.FLOAT32 -> Float32
                    IDLOpcode.BOOL -> Bool
                    IDLOpcode.NULL -> Null
                    IDLOpcode.TEXT -> Text
                    IDLOpcode.NAT -> Natural
                    IDLOpcode.NAT8 -> Nat8
                    IDLOpcode.NAT16 -> Nat16
                    IDLOpcode.NAT32 -> Nat32
                    IDLOpcode.NAT64 -> Nat64
                    IDLOpcode.INT -> Integer
                    IDLOpcode.INT8 -> Int8
                    IDLOpcode.INT16 -> Int16
                    IDLOpcode.INT32 -> Int32
                    IDLOpcode.INT64 -> Int64
                    else -> error("Not primitive opcode")
                }
            }
        }

        object Natural : Primitive() {
            override var text = "nat"
            override val opcode = IDLOpcode.NAT
        }

        object Nat8 : Primitive() {
            override var text = "nat8"
            override val opcode = IDLOpcode.NAT8
        }

        object Nat16 : Primitive() {
            override var text = "nat16"
            override val opcode = IDLOpcode.NAT16
        }

        object Nat32 : Primitive() {
            override var text = "nat32"
            override val opcode = IDLOpcode.NAT32
        }

        object Nat64 : Primitive() {
            override var text = "nat64"
            override val opcode = IDLOpcode.NAT64
        }

        object Integer : Primitive() {
            override var text = "int"
            override val opcode = IDLOpcode.INT
        }

        object Int8 : Primitive() {
            override var text = "int8"
            override val opcode = IDLOpcode.INT8
        }

        object Int16 : Primitive() {
            override var text = "int16"
            override val opcode = IDLOpcode.INT16
        }

        object Int32 : Primitive() {
            override var text = "int32"
            override val opcode = IDLOpcode.INT32
        }

        object Int64 : Primitive() {
            override var text = "int64"
            override val opcode = IDLOpcode.INT64
        }

        object Float32 : Primitive() {
            override var text = "float32"
            override val opcode = IDLOpcode.FLOAT32
        }

        object Float64 : Primitive() {
            override var text = "float64"
            override val opcode = IDLOpcode.FLOAT64
        }

        object Bool : Primitive() {
            override var text = "bool"
            override val opcode = IDLOpcode.BOOL
        }

        object Text : Primitive() {
            override var text = "text"
            override val opcode = IDLOpcode.TEXT
        }

        object Null : Primitive() {
            override var text = "null"
            override val opcode = IDLOpcode.NULL
        }

        object Reserved : Primitive() {
            override var text = "reserved"
            override val opcode = IDLOpcode.RESERVED
        }

        object Empty : Primitive() {
            override var text = "empty"
            override val opcode = IDLOpcode.EMPTY
        }
    }

    sealed class Other : IDLType() {
        data class Custom(val opcode: Int) : Other() {
            val text: String = "<CUSTOM>"
            override fun toString() = text
            override fun poetize() = CodeBlock.of("%T($opcode)", Custom::class).toString()
        }

        data class Future(val opcode: Int) : Other() {
            val text: String = "<FUTURE>"
            override fun toString() = text
            override fun poetize() = CodeBlock.of("%T($opcode)", Future::class).toString()
        }
    }
}

data class IDLFieldType(val name: String?, val type: IDLType, var idx: Int) {
    override fun toString(): String {
        // for anonymous fields
        if (name == null) return "$type"

        // for variant fields
        if (type == IDLType.Primitive.Null) return "$name"

        // for other fields
        return "$name: $type"
    }

    fun poetize() = CodeBlock.of("%T(\"$name\", ${type.poetize()}, $idx)", IDLFieldType::class.asTypeName()).toString()
}

data class IDLArgType(val name: String?, val type: IDLType) {
    override fun toString() = "${if (name == null) "" else "$name: "}$type"
    fun poetize() = CodeBlock.of("%T(\"$name\", ${type.poetize()})", IDLArgType::class.asTypeName()).toString()
}

enum class IDLFuncAnn {
    Oneway {
        override var text = "oneway"
        override fun poetize() = CodeBlock.of("%T", Oneway::class.asTypeName()).toString()
    },
    Query {
        override var text = "query"
        override fun poetize() = CodeBlock.of("%T", Query::class.asTypeName()).toString()
    };
    abstract val text: String
    override fun toString() = text
    abstract fun poetize(): String
}

data class IDLMethod(val name: IDLName, val type: IDLMethodType) {
    override fun toString() = "$name: ${type.toString().replace(IDLType.Reference.Func.text, "")};"
    fun poetize() = CodeBlock.of("%T($name, ${(type as IDLType).poetize()})", IDLMethod::class.asTypeName()).toString()
}

sealed class IDLDef {
    data class Type(val name: String, val type: IDLType) : IDLDef() {
        companion object { const val text = "type" }
        override fun toString() = "$text $name = $type;"
    }
    data class Import(val filePath: IDLToken.TextVal) : IDLDef() {
        companion object { const val text = "import" }

        override fun toString() = "$text \"$filePath\";"
    }
}

data class IDLActor(val name: String?, val type: IDLActorType) {
    override fun toString() = "${IDLType.Reference.Service.text} ${if (name == null) "" else "$name"}:${
        type.toString().replace(
            IDLType.Reference.Service.text, ""
        )
    };"
}

data class IDLProgram(val imports: List<IDLDef.Import>, val types: List<IDLDef.Type>, val actor: IDLActor?) {
    override fun toString() = "${prettyString(imports)}${prettyString(types)}$actor"
    private fun <E> prettyString(items: List<E>): String {
        return if (items.isEmpty()) "" else items.joinToString(System.lineSeparator(), "", System.lineSeparator())
    }
}

interface IDLMethodType

interface IDLActorType
