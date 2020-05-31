package senior.joinu.candid

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.math.BigInteger
import java.nio.ByteBuffer

object KtTranspiler {
    fun transpile(program: IDLProgram, packageName: String, fileName: String): TranspileContext {
        val context = TranspileContext(packageName, fileName)
        // TODO: handle imports

        program.types.forEach { (name, value) ->
            context.typeTable.registerTypeWithLabel(IDLType.Id(name), value)

            when (value) {
                is IDLType.Constructive.Variant,
                    is IDLType.Constructive.Record,
                    is IDLType.Reference.Func,
                    is IDLType.Reference.Service -> transpileType(value, context, ClassName(context.packageName, name))
                else -> {
                    val typeAliasSpec = TypeAliasSpec
                        .builder(name, transpileType(value, context))
                        .build()

                    context.currentSpec.addTypeAlias(typeAliasSpec)
                }
            }
        }

        if (program.actor != null) {
            val name = program.actor.name ?: "MainActor"
            transpileType(program.actor.type as IDLType, context, ClassName(context.packageName, name))
        }

        return context
    }

    fun transpileType(type: IDLType, context: TranspileContext, name: ClassName? = null): TypeName {
        return when (type) {
            is IDLType.Id -> ClassName(context.packageName, type.value)
            is IDLType.Primitive -> when (type) {
                is IDLType.Primitive.Natural -> BigInteger::class.asClassName()
                is IDLType.Primitive.Nat8 -> UByte::class.asClassName()
                is IDLType.Primitive.Nat16 -> UShort::class.asClassName()
                is IDLType.Primitive.Nat32 -> UInt::class.asClassName()
                is IDLType.Primitive.Nat64 -> ULong::class.asClassName()

                is IDLType.Primitive.Integer -> BigInteger::class.asClassName()
                is IDLType.Primitive.Int8 -> Byte::class.asClassName()
                is IDLType.Primitive.Int16 -> Short::class.asClassName()
                is IDLType.Primitive.Int32 -> Int::class.asClassName()
                is IDLType.Primitive.Int64 -> Long::class.asClassName()

                is IDLType.Primitive.Float32 -> Float::class.asClassName()
                is IDLType.Primitive.Float64 -> Double::class.asClassName()

                is IDLType.Primitive.Bool -> Boolean::class.asClassName()
                is IDLType.Primitive.Text -> String::class.asClassName()
                is IDLType.Primitive.Null -> Null::class.asClassName()
                is IDLType.Primitive.Reserved -> Reserved::class.asClassName()
                is IDLType.Primitive.Empty -> Empty::class.asClassName()
            }
            is IDLType.Constructive -> when (type) {
                is IDLType.Constructive.Opt -> transpileType(type.type, context).copy(true)
                is IDLType.Constructive.Vec -> ClassName("kotlin.collections", "List")
                    .parameterizedBy(transpileType(type.type, context))
                is IDLType.Constructive.Blob -> ByteArray::class.asClassName()
                is IDLType.Constructive.Record -> {
                    val recordName = name ?: context.nextAnonymousTypeName()

                    val recordBuilder = if (type.fields.isEmpty()) {
                        TypeSpec.objectBuilder(recordName)
                    } else {
                        val dataClassBuilder = TypeSpec.classBuilder(recordName).addModifiers(KModifier.DATA)
                        val constructorBuilder = FunSpec.constructorBuilder()
                        type.fields.forEachIndexed { idx, field ->
                            val fieldName = field.name ?: idx.toString()
                            val fieldTypeName = transpileType(field.type, context)

                            constructorBuilder.addParameter(fieldName, fieldTypeName)

                            val propertyBuilder = PropertySpec
                                .builder(fieldName, fieldTypeName)
                                .initializer(fieldName)
                                .build()
                            dataClassBuilder.addProperty(propertyBuilder)
                        }

                        dataClassBuilder.primaryConstructor(constructorBuilder.build())
                    }

                    makeSerializer(recordBuilder)

                    val record = recordBuilder.build()
                    context.currentSpec.addType(record)

                    return recordName
                }
                is IDLType.Constructive.Variant -> {
                    val variantName = context.nextAnonymousTypeName()

                    val variantBuilder = if (type.fields.isEmpty()) {
                        TypeSpec.objectBuilder(variantName)
                    } else {
                        val variantBuilder = TypeSpec.classBuilder(variantName).addModifiers(KModifier.SEALED)
                        type.fields.forEachIndexed { idx, field ->
                            val fieldName = field.name ?: idx.toString()
                            val fieldType = transpileType(field.type, context)

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
                    context.currentSpec.addType(variant)

                    return variantName
                }
            }
            is IDLType.Reference.Func -> {
                val funcTypeName = name ?: context.nextAnonymousFuncTypeName()

                val args = type.arguments.mapIndexed { idx, arg ->
                    val argName = arg.name ?: "arg$idx"
                    val argTypeName = transpileType(arg.type, context)

                    ParameterSpec.builder(argName, argTypeName).build()
                }

                val resultName = when {
                    type.results.size == 1 -> {
                        val result = type.results.first()

                        transpileType(result.type, context)
                    }
                    type.results.isNotEmpty() -> {
                        val resultClassName = context.createResultValueTypeName(funcTypeName.simpleName)
                        val resultClassBuilder = TypeSpec.classBuilder(resultClassName).addModifiers(KModifier.DATA)

                        val constructorBuilder = FunSpec.constructorBuilder()
                        type.results.forEachIndexed { idx, res ->
                            val resName = res.name ?: "$idx"
                            val resTypeName = transpileType(type, context)

                            constructorBuilder.addParameter(resName, resTypeName)
                            val propertyBuilder = PropertySpec
                                .builder(resName, resTypeName)
                                .initializer(resName)
                                .build()
                            resultClassBuilder.addProperty(propertyBuilder)
                        }

                        resultClassBuilder.primaryConstructor(constructorBuilder.build())
                        val resultDataClass = resultClassBuilder.build()
                        context.currentSpec.addType(resultDataClass)

                        resultClassName
                    }
                    else -> Unit::class.asClassName()
                }

                val func = FunSpec.builder("invoke")
                    .addModifiers(KModifier.OPERATOR)
                    .addModifiers(KModifier.SUSPEND)

                for (arg in args) {
                    func.addParameter(arg)
                }

                func.returns(resultName)

                val callable = TypeSpec.classBuilder(funcTypeName)
                    .addFunction(func.build())

                makeSerializer(callable)

                context.currentSpec.addType(callable.build())

                return funcTypeName
            }
            is IDLType.Reference.Service -> {
                val actorClassName = name ?: context.nextAnonymousTypeName()

                val actorClassBuilder = if (type.methods.isEmpty()) {
                    TypeSpec.objectBuilder(actorClassName)
                } else {
                    val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

                    for (method in type.methods) {
                        val propertySpec = PropertySpec
                            .builder(method.name, transpileType(method.type as IDLType, context))
                            .initializer(transpileFuncBody(method.type, context))

                        actorClassBuilder.addProperty(propertySpec.build())
                    }

                    actorClassBuilder
                }

                makeSerializer(actorClassBuilder)

                val actorClass = actorClassBuilder.build()
                context.currentSpec.addType(actorClass)

                return actorClassName
            }
            is IDLType.Reference.Principal -> Principal::class.asClassName()
        }
    }

    private fun makeSerializer(spec: TypeSpec.Builder) {
        spec.addSuperinterface(Serializer::class.asClassName())
        val serialize = FunSpec
            .builder("serialize")
            .addParameter("buf", ByteBuffer::class.asTypeName())
            .addModifiers(KModifier.OVERRIDE)
            .build()
        spec.addFunction(serialize)
    }

    fun transpileFuncBody(type: IDLType, context: TranspileContext): CodeBlock {
        return when (type) {
            is IDLType.Id -> {
                val innerType = context.typeTable.getTypeByLabel(type)
                return transpileFuncBody(innerType, context)
            }
            is IDLType.Reference.Func -> {
                // TODO: add "serialize type table", "serialize arg type", "serialize arg value" functions
                val argNames = type.arguments
                    .mapIndexed { index, arg -> arg.name ?: "arg$index" }
                    .joinToString(postfix = " ->") { it }

                val typeTable = TypeTable()
                for (arg in type.arguments) {
                    context.typeTable.copyLabelsForType(arg.type, typeTable)
                }

                CodeBlock.of("""{
                    | // I = ${type.arguments.map { it.type.getTOpcodeList(typeTable) }}
                    | // T = ${typeTable.registry.map { it.getTOpcodeList(typeTable) }}
                    | // labels = ${typeTable.labels}
                    |}""".trimMargin())
            }
            else -> CodeBlock.of("")
        }
    }
}

class TranspileContext(val packageName: String, val fileName: String) {
    val typeTable = TypeTable()
    val currentSpec = FileSpec.builder(packageName, fileName)

    var anonymousTypeCount = 0
    var anonymousFuncCount = 0

    fun nextAnonymousTypeName() =
        ClassName(packageName, "AnonIDLType${anonymousTypeCount++}")

    fun nextAnonymousFuncTypeName() =
        ClassName(packageName, "AnonFunc${anonymousFuncCount++}")

    fun createResultValueTypeName(funcName: String) =
        ClassName(packageName, "${funcName.capitalize()}Result")
}