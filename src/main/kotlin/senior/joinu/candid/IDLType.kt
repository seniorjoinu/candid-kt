package senior.joinu.candid

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asTypeName
import senior.joinu.candid.serialize.getTypeSerForType
import senior.joinu.leb128.Leb128
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

    fun registerIfNotPrimitiveOrId(type: IDLType) {
        when (type) {
            is IDLType.Id, is IDLType.Primitive -> {
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

    data class Id(override val value: String) : IDLType(), IDLMethodType, IDLTextToken, IDLActorType {
        override fun poetize() = CodeBlock.of("%T(\"$value\")", Id::class).toString()
    }

    sealed class Reference : IDLType() {
        data class Func(
            val arguments: List<IDLArgType> = emptyList(),
            val results: List<IDLArgType> = emptyList(),
            val annotations: List<IDLFuncAnn> = emptyList()
        ) : Reference(), IDLMethodType {
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
            override fun poetize(): String {
                val poetizedMethods = methods.joinToString { it.poetize() }
                return CodeBlock.of("%T(methods = listOf($poetizedMethods))", Service::class).toString()
            }
        }

        object Principal : Reference() {
            override fun toString() = "Principal"

            override fun poetize() = CodeBlock.of("%T", Principal::class).toString()
        }
    }

    sealed class Constructive : IDLType() {
        data class Opt(val type: IDLType) : Constructive() {
            override fun poetize() = CodeBlock.of("%T(${type.poetize()})", Opt::class).toString()
        }

        data class Vec(val type: IDLType) : Constructive() {
            override fun poetize() = CodeBlock.of("%T(${type.poetize()})", Vec::class).toString()
        }

        object Blob : Constructive() {
            override fun toString() = "Blob"

            override fun poetize() = CodeBlock.of("%T", Blob::class).toString()
        }

        data class Record(val fields: List<IDLFieldType> = emptyList()) : Constructive() {
            override fun poetize(): String {
                val poetizedFields = fields.joinToString { it.poetize() }
                return CodeBlock.of("%T(fields = listOf($poetizedFields))", Record::class).toString()
            }
        }

        data class Variant(val fields: List<IDLFieldType> = emptyList()) : Constructive() {
            override fun poetize(): String {
                val poetizedFields = fields.joinToString { it.poetize() }
                return CodeBlock.of("%T(fields = listOf($poetizedFields))", Variant::class).toString()
            }
        }
    }

    sealed class Primitive : IDLType() {
        object Natural : Primitive() {
            override fun toString() = "Nat"

            override fun poetize() = CodeBlock.of("%T", Natural::class).toString()
        }

        object Nat8 : Primitive() {
            override fun toString() = "Nat8"

            override fun poetize() = CodeBlock.of("%T", Nat8::class).toString()
        }

        object Nat16 : Primitive() {
            override fun toString() = "Nat16"

            override fun poetize() = CodeBlock.of("%T", Nat16::class).toString()
        }

        object Nat32 : Primitive() {
            override fun toString() = "Nat32"

            override fun poetize() = CodeBlock.of("%T", Nat32::class).toString()
        }

        object Nat64 : Primitive() {
            override fun toString() = "Nat64"

            override fun poetize() = CodeBlock.of("%T", Nat64::class).toString()
        }

        object Integer : Primitive() {
            override fun toString() = "Int"

            override fun poetize() = CodeBlock.of("%T", Integer::class).toString()
        }

        object Int8 : Primitive() {
            override fun toString() = "Int8"

            override fun poetize() = CodeBlock.of("%T", Int8::class).toString()
        }

        object Int16 : Primitive() {
            override fun toString() = "Int16"

            override fun poetize() = CodeBlock.of("%T", Int16::class).toString()
        }

        object Int32 : Primitive() {
            override fun toString() = "Int32"

            override fun poetize() = CodeBlock.of("%T", Int32::class).toString()
        }

        object Int64 : Primitive() {
            override fun toString() = "Int64"

            override fun poetize() = CodeBlock.of("%T", Int64::class).toString()
        }

        object Float32 : Primitive() {
            override fun toString() = "Float32"

            override fun poetize() = CodeBlock.of("%T", Float32::class).toString()
        }

        object Float64 : Primitive() {
            override fun toString() = "Float64"

            override fun poetize() = CodeBlock.of("%T", Float64::class).toString()
        }

        object Bool : Primitive() {
            override fun toString() = "Bool"

            override fun poetize() = CodeBlock.of("%T", Bool::class).toString()
        }

        object Text : Primitive() {
            override fun toString() = "Text"

            override fun poetize() = CodeBlock.of("%T", Text::class).toString()
        }

        object Null : Primitive() {
            override fun toString() = "Null"

            override fun poetize() = CodeBlock.of("%T", Null::class).toString()
        }

        object Reserved : Primitive() {
            override fun toString() = "Reserved"

            override fun poetize() = CodeBlock.of("%T", Reserved::class).toString()
        }

        object Empty : Primitive() {
            override fun toString() = "Empty"

            override fun poetize() = CodeBlock.of("%T", Empty::class).toString()
        }
    }

    sealed class Other : IDLType() {
        data class Custom(val opcode: Int) : Other() {
            override fun poetize() = CodeBlock.of("%T($opcode)", Custom::class).toString()
        }

        data class Future(val opcode: Int) : Other() {
            override fun poetize() = CodeBlock.of("%T($opcode)", Future::class).toString()
        }
    }
}

data class IDLFieldType(val name: String?, val type: IDLType, var idx: Int) {
    fun poetize() = CodeBlock.of("%T(\"$name\", ${type.poetize()}, $idx)", IDLFieldType::class.asTypeName()).toString()
}

data class IDLArgType(val name: String?, val type: IDLType) {
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