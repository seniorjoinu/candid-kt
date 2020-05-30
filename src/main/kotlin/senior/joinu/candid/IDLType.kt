package senior.joinu.candid

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.math.BigInteger

sealed class IDLType {
    companion object {
        val typeTable = mutableListOf<IDLType>()
        val labels = mutableMapOf<Id, Int>()

        fun addLabeledType(label: Id, type: IDLType): Int {
            val idx = typeTable.lastIndex + 1
            labels[label] = idx
            typeTable.add(type)

            return idx
        }

        fun addType(type: IDLType): Int {
            val idx = typeTable.indexOf(type)

            return if (idx == -1) {
                typeTable.add(type)
                typeTable.lastIndex
            } else {
                idx
            }
        }

        fun getInnerTypeOpcodeList(type: IDLType): List<Opcode> = when (type) {
            is Primitive -> type.opcodeList
            is Id -> getOpcodeListByLabel(type)
            else -> Opcode.Integer.listFrom(addType(type))
        }

        fun getOpcodeListByLabel(label: Id): List<Opcode> {
            return Opcode.Integer.listFrom(labels[label]!!)
        }

        fun getTypeByLabel(label: Id): IDLType {
            val id = labels[label]!!
            return typeTable[id]
        }

        fun transpileFuncBody(type: IDLType): CodeBlock {
            return when (type) {
                is Id -> {
                    val innerType = getTypeByLabel(type)
                    return transpileFuncBody(innerType)
                }
                is Reference.Func -> {
                    val argNames = type.arguments
                        .mapIndexed { index, arg -> arg.name ?: "arg$index" }
                        .joinToString(postfix = " ->") { it }
                    CodeBlock.of(
                        """{ $argNames
                            |
                            |}""".trimMargin()
                    )
                }
                else -> CodeBlock.of("")
            }
        }

        var anonymousTypeCount = 0

        private fun nextAnonymousTypeName(`package`: String) =
            ClassName(`package`, "AnonIDLType${anonymousTypeCount++}")

        private fun nextAnonymousFuncTypeName(`package`: String) =
            ClassName(`package`, "AnonFunc${anonymousTypeCount++}")

        private fun createResultValueTypeName(funcName: String, `package`: String) =
            ClassName(`package`, "${funcName.capitalize()}Result")
    }

    abstract val opcodeList: List<Opcode>
    abstract fun transpile(fileSpec: FileSpec.Builder, packageName: String): TypeName

    data class Id(override val value: String) : IDLType(), IDLMethodType, IDLTextToken, IDLActorType {
        override val opcodeList: List<Opcode> by lazy {
            getOpcodeListByLabel(Id(value))
        }

        override fun computeOpcodeList() = opcodeList
        override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = ClassName(packageName, value)
    }

    sealed class Reference : IDLType() {
        data class Func(
            val arguments: List<IDLArgType>,
            val results: List<IDLArgType>,
            val annotations: List<IDLFuncAnn>
        ) : Reference(), IDLMethodType {
            override val opcodeList: List<Opcode> by lazy {
                val funcOpcode = Opcode.Integer.listFrom(IDLOpcode.FUNC)
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

                funcOpcode + argsOpcode + resultsOpcode + annotationsOpcode
            }

            override fun computeOpcodeList() = opcodeList
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String): TypeName {
                val funcTypeName = nextAnonymousFuncTypeName(packageName)

                val args = arguments.mapIndexed { idx, arg ->
                    val argName = arg.name ?: "arg$idx"
                    val argTypeName = arg.type.transpile(fileSpec, packageName)

                    ParameterSpec.builder(argName, argTypeName).build()
                }

                val resultName = when {
                    results.size == 1 -> {
                        val result = results.first()

                        result.type.transpile(fileSpec, packageName)
                    }
                    results.isNotEmpty() -> {
                        val resultClassName =
                            createResultValueTypeName(
                                funcTypeName.simpleName,
                                packageName
                            )
                        val resultClassBuilder = TypeSpec.classBuilder(resultClassName).addModifiers(KModifier.DATA)

                        val constructorBuilder = FunSpec.constructorBuilder()
                        results.forEachIndexed { idx, res ->
                            val resName = res.name ?: "$idx"
                            val resTypeName = res.type.transpile(fileSpec, packageName)

                            constructorBuilder.addParameter(resName, resTypeName)
                            val propertyBuilder = PropertySpec
                                .builder(resName, resTypeName)
                                .initializer(resName)
                                .build()
                            resultClassBuilder.addProperty(propertyBuilder)
                        }

                        resultClassBuilder.primaryConstructor(constructorBuilder.build())
                        val resultDataClass = resultClassBuilder.build()
                        fileSpec.addType(resultDataClass)

                        resultClassName
                    }
                    else -> Unit::class.asClassName()
                }

                return LambdaTypeName
                    .get(parameters = args, returnType = resultName)
                    .copy(suspending = true)
            }
        }

        data class Service(val methods: List<IDLMethod>) : Reference(),
            IDLActorType {
            override val opcodeList by lazy {
                Opcode.Integer.listFrom(IDLOpcode.SERVICE) +
                        Opcode.Integer.listFrom(methods.size, OpcodeEncoding.LEB) +
                        methods
                            .map { method ->
                                Opcode.Integer.listFrom(method.name.length, OpcodeEncoding.LEB) +
                                        Opcode.Utf8.listFrom(method.name) +
                                        method.type.computeOpcodeList()
                            }
                            .flatten()
            }

            override fun transpile(fileSpec: FileSpec.Builder, packageName: String): TypeName {
                val actorClassName = nextAnonymousTypeName(packageName)

                val actorClassBuilder = if (methods.isEmpty()) {
                    TypeSpec.objectBuilder(actorClassName)
                } else {
                    val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

                    for (method in methods) {
                        val propertySpec = PropertySpec
                            .builder(
                                method.name,
                                (method.type as IDLType).transpile(fileSpec, packageName)
                            )
                            .initializer(transpileFuncBody(method.type))

                        actorClassBuilder.addProperty(propertySpec.build())
                    }

                    actorClassBuilder
                }

                val actorClass = actorClassBuilder.build()
                fileSpec.addType(actorClass)

                return actorClassName
            }
        }

        object Principal : Reference() {
            override val opcodeList by lazy {
                Opcode.Integer.listFrom(IDLOpcode.PRINCIPAL)
            }

            override fun toString() = "Principal"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) =
                senior.joinu.candid.Principal::class.asClassName()
        }
    }

    sealed class Constructive : IDLType() {
        data class Opt(val type: IDLType) : Constructive() {
            override val opcodeList by lazy {
                Opcode.Integer.listFrom(IDLOpcode.OPT) + getInnerTypeOpcodeList(type)
            }

            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) =
                type.transpile(fileSpec, packageName).copy(true)
        }

        data class Vec(val type: IDLType) : Constructive() {
            override val opcodeList by lazy {
                Opcode.Integer.listFrom(IDLOpcode.VEC) + getInnerTypeOpcodeList(type)
            }

            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) =
                ClassName("kotlin.collections", "List")
                    .parameterizedBy(type.transpile(fileSpec, packageName))
        }

        object Blob : Constructive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.VEC) + Primitive.Nat8.opcodeList

            override fun toString() = "Blob"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = ByteArray::class.asClassName()
        }

        data class Record(val fields: List<IDLFieldType>) : Constructive() {
            override val opcodeList by lazy {
                Opcode.Integer.listFrom(IDLOpcode.RECORD) +
                        Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                        fields
                            .mapIndexed { idx, field -> field.getOpcodeList(idx) }
                            .flatten()
            }

            override fun transpile(fileSpec: FileSpec.Builder, packageName: String): TypeName {
                val recordName =
                    nextAnonymousTypeName(packageName)

                val recordBuilder = if (fields.isEmpty()) {
                    TypeSpec.objectBuilder(recordName)
                } else {
                    val dataClassBuilder = TypeSpec.classBuilder(recordName).addModifiers(KModifier.DATA)
                    val constructorBuilder = FunSpec.constructorBuilder()
                    fields.forEachIndexed { idx, field ->
                        val fieldName = field.name ?: idx.toString()
                        val fieldTypeName = field.type.transpile(fileSpec, packageName)

                        constructorBuilder.addParameter(fieldName, fieldTypeName)

                        val propertyBuilder = PropertySpec
                            .builder(fieldName, fieldTypeName)
                            .initializer(fieldName)
                            .build()
                        dataClassBuilder.addProperty(propertyBuilder)
                    }

                    dataClassBuilder.primaryConstructor(constructorBuilder.build())
                }

                val record = recordBuilder.build()
                fileSpec.addType(record)

                return recordName
            }
        }

        data class Variant(val fields: List<IDLFieldType>) : Constructive() {
            override val opcodeList by lazy {
                Opcode.Integer.listFrom(IDLOpcode.VARIANT) +
                        Opcode.Integer.listFrom(fields.size, OpcodeEncoding.LEB) +
                        fields
                            .mapIndexed { idx, field -> field.getOpcodeList(idx) }
                            .flatten()
            }

            override fun transpile(fileSpec: FileSpec.Builder, packageName: String): TypeName {
                val variantName =
                    nextAnonymousTypeName(packageName)

                val variantBuilder = if (fields.isEmpty()) {
                    TypeSpec.objectBuilder(variantName)
                } else {
                    val variantBuilder = TypeSpec.classBuilder(variantName).addModifiers(KModifier.SEALED)
                    fields.forEachIndexed { idx, field ->
                        val fieldName = field.name ?: idx.toString()
                        val fieldType = field.type.transpile(fileSpec, packageName)

                        val property = PropertySpec
                            .builder("value", fieldType)
                            .initializer("value")
                            .build()
                        val constructor = FunSpec
                            .constructorBuilder()
                            .addParameter("value", fieldType)
                            .build()
                        val sealedSubclass = TypeSpec
                            .classBuilder(fieldName)
                            .addModifiers(KModifier.DATA)
                            .superclass(variantName)
                            .addProperty(property)
                            .primaryConstructor(constructor)
                            .build()

                        variantBuilder.addType(sealedSubclass)
                    }
                    variantBuilder
                }

                val variant = variantBuilder.build()
                fileSpec.addType(variant)

                return variantName
            }
        }
    }

    sealed class Primitive : IDLType() {
        object Natural : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.NAT)

            override fun toString() = "Nat"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = BigInteger::class.asClassName()
        }

        object Nat8 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.NAT8)

            override fun toString() = "Nat8"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = UByte::class.asClassName()
        }

        object Nat16 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.NAT16)

            override fun toString() = "Nat16"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = UShort::class.asClassName()
        }

        object Nat32 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.NAT32)

            override fun toString() = "Nat32"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = UInt::class.asClassName()
        }

        object Nat64 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.NAT64)

            override fun toString() = "Nat64"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = ULong::class.asClassName()
        }

        object Integer : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.INT)

            override fun toString() = "Int"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = BigInteger::class.asClassName()
        }

        object Int8 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.INT8)

            override fun toString() = "Int8"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Byte::class.asClassName()
        }

        object Int16 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.INT16)

            override fun toString() = "Int16"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Short::class.asClassName()
        }

        object Int32 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.INT32)

            override fun toString() = "Int32"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Int::class.asClassName()
        }

        object Int64 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.INT64)

            override fun toString() = "Int64"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Long::class.asClassName()
        }

        object Float32 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.FLOAT32)

            override fun toString() = "Float32"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Float::class.asClassName()
        }

        object Float64 : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.FLOAT64)

            override fun toString() = "Float64"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Double::class.asClassName()
        }

        object Bool : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.BOOL)

            override fun toString() = "Bool"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = Boolean::class.asClassName()
        }

        object Text : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.TEXT)

            override fun toString() = "Text"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) = String::class.asClassName()
        }

        object Null : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.NULL)

            override fun toString() = "Null"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) =
                senior.joinu.candid.Null::class.asClassName()
        }

        object Reserved : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.RESERVED)

            override fun toString() = "Reserved"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) =
                senior.joinu.candid.Reserved::class.asClassName()
        }

        object Empty : Primitive() {
            override val opcodeList = Opcode.Integer.listFrom(IDLOpcode.EMPTY)

            override fun toString() = "Empty"
            override fun transpile(fileSpec: FileSpec.Builder, packageName: String) =
                senior.joinu.candid.Empty::class.asClassName()
        }
    }
}

data class IDLFieldType(val name: String?, val type: IDLType) {
    fun getOpcodeList(idx: Int) = Opcode.Integer.listFrom(idx, OpcodeEncoding.LEB) + IDLType.getInnerTypeOpcodeList(
        type
    )
}

data class IDLArgType(val name: String?, val type: IDLType) {
    fun getOpcodeList() = IDLType.getInnerTypeOpcodeList(type)
}

enum class IDLFuncAnn {
    Oneway, Query
}

data class IDLMethod(val name: String, val type: IDLMethodType)
sealed class IDLDef {
    data class Type(val name: String, val type: IDLType) : IDLDef() {
        init {
            val id = IDLType.Id(name)
            IDLType.addLabeledType(id, type)

            // hack
            id.computeOpcodeList()
        }
    }

    data class Import(val filePath: String) : IDLDef()
}

data class IDLActorDef(val name: String?, val type: IDLActorType)
data class IDLProgram(val imports: List<IDLDef.Import>, val types: List<IDLDef.Type>, val actor: IDLActorDef?)

interface IDLMethodType {
    fun computeOpcodeList(): List<Opcode>
}

interface IDLActorType