package senior.joinu.candid

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import senior.joinu.leb128.Leb128
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*

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
                            val fieldTypeProp = PropertySpec
                                .builder("${fieldName}Type", IDLType::class.asTypeName())
                                .initializer(CodeBlock.of(field.type.poetize()))
                                .build()

                            dataClassBuilder.addProperty(propertyBuilder)
                            dataClassBuilder.addProperty(fieldTypeProp)
                        }

                        dataClassBuilder.primaryConstructor(constructorBuilder.build())
                    }

                    recordBuilder.addSuperinterface(IDLSerializable::class.asClassName())

                    val serialize = FunSpec
                        .builder("serialize")
                        .addParameter("buf", ByteBuffer::class.asTypeName())
                        .addParameter("typeTable", TypeTable::class.asTypeName())
                        .addModifiers(KModifier.OVERRIDE)
                    type.fields.forEachIndexed { idx, field ->
                        val fieldName = field.name ?: idx.toString()
                        serialize.addStatement(
                            "%T.serializeIDLValue(${"${fieldName}Type".escapeIfNecessary()}, ${fieldName.escapeIfNecessary()}, buf, typeTable)",
                            KtSerilializer::class.asTypeName()
                        )
                    }
                    recordBuilder.addFunction(serialize.build())

                    val sizeBytes = FunSpec
                        .builder("sizeBytes")
                        .addParameter("typeTable", TypeTable::class.asTypeName())
                        .returns(Int::class)
                        .addModifiers(KModifier.OVERRIDE)
                    val ops = type.fields.mapIndexed { idx, field ->
                        val fieldName = field.name ?: idx.toString()
                        CodeBlock.of(
                            "%T.getSizeBytesOfIDLValue(${"${fieldName}Type".escapeIfNecessary()}, ${fieldName.escapeIfNecessary()}, typeTable)",
                            KtSerilializer::class.asTypeName()
                        )
                    }
                    sizeBytes.addStatement("return " + ops.joinToString(" + ") { it.toString() })
                    recordBuilder.addFunction(sizeBytes.build())

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

                            val valueProp = PropertySpec
                                .builder("value", fieldType)
                                .initializer("value")
                                .build()
                            val valuePropType = PropertySpec
                                .builder("valueType", IDLType::class.asTypeName())
                                .initializer(field.type.poetize())
                                .build()

                            val constructor = FunSpec
                                .constructorBuilder()
                                .addParameter("value", fieldType)
                                .build()
                            val sealedSubclass = TypeSpec
                                .classBuilder(fieldName)
                                .addModifiers(KModifier.DATA)
                                .superclass(variantName)
                                .addProperty(valueProp)
                                .addProperty(valuePropType)
                                .primaryConstructor(constructor)

                            sealedSubclass.addSuperinterface(IDLSerializable::class.asClassName())

                            val serialize = FunSpec
                                .builder("serialize")
                                .addParameter("buf", ByteBuffer::class.asTypeName())
                                .addParameter("typeTable", TypeTable::class.asTypeName())
                                .addModifiers(KModifier.OVERRIDE)
                            serialize.addStatement("%T.writeUnsigned(buf, ${idx}.toUInt())", Leb128::class)
                            serialize.addStatement(
                                "%T.serializeIDLValue(valueType, value, buf, typeTable)",
                                KtSerilializer::class.asTypeName()
                            )
                            sealedSubclass.addFunction(serialize.build())

                            val sizeBytes = FunSpec
                                .builder("sizeBytes")
                                .addParameter("typeTable", TypeTable::class.asTypeName())
                                .returns(Int::class)
                                .addModifiers(KModifier.OVERRIDE)
                            sizeBytes.addStatement("return %T.sizeUnsigned($idx) + %T.getSizeBytesOfIDLValue(valueType, value, typeTable)", Leb128::class, KtSerilializer::class.asTypeName())
                            sealedSubclass.addFunction(sizeBytes.build())


                            variantBuilder.addType(sealedSubclass.build())
                        }
                        variantBuilder
                    }

                    variantBuilder.addSuperinterface(IDLSerializable::class.asClassName())
                    val serialize = FunSpec
                        .builder("serialize")
                        .addParameter("buf", ByteBuffer::class.asTypeName())
                        .addParameter("typeTable", TypeTable::class.asTypeName())
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Unit::class)

                    serialize.addStatement(
                        "throw %T(\"Unable·to·serialize·sealed·superclass·$variantName\")",
                        RuntimeException::class
                    )

                    variantBuilder.addFunction(serialize.build())

                    val variant = variantBuilder.build()
                    context.currentSpec.addType(variant)

                    return variantName
                }
            }
            is IDLType.Reference.Func -> {
                val funcTypeName = name ?: context.nextAnonymousFuncTypeName()

                val typeTable = TypeTable()
                val invoke = FunSpec.builder("invoke")
                    .addModifiers(KModifier.OPERATOR)
                    .addModifiers(KModifier.SUSPEND)

                val callable = TypeSpec.classBuilder(funcTypeName)
                    .superclass(IDLFunc::class)

                val ops = type.arguments.mapIndexed { idx, arg ->
                    val argName = arg.name ?: "arg$idx"

                    CodeBlock.of("%T.getSizeBytesOfIDLValue(${"${argName}Type".escapeIfNecessary()}, ${argName.escapeIfNecessary()}, typeTable)", KtSerilializer::class)
                }

                invoke
                    .addStatement("val mSize = " + ops.joinToString(" + ") { it.toString() })
                    .addStatement("val buf = %T.allocate(mSize)", ByteBuffer::class)

                type.arguments.forEachIndexed { idx, arg ->
                    val argName = arg.name ?: "arg$idx"
                    val argTypeName = transpileType(arg.type, context)

                    val argParam = ParameterSpec.builder(argName, argTypeName).build()
                    invoke.addParameter(argParam)

                    val argType = PropertySpec
                        .builder("${argName}Type", IDLType::class)
                        .initializer(arg.type.poetize())
                        .build()

                    context.typeTable.copyLabelsForType(arg.type, typeTable)

                    callable.addProperty(argType)
                    invoke.addStatement("%T.serializeIDLValue(${"${argName}Type".escapeIfNecessary()}, ${argName.escapeIfNecessary()}, buf, typeTable)", KtSerilializer::class)
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
                invoke.returns(resultName)

                val constructorSpec = FunSpec.constructorBuilder()
                    .addParameter("service", IDLService::class.asTypeName().copy(true))
                    .addParameter("funcName", String::class.asTypeName().copy(true))
                    .build()
                val servicePropSpec = PropertySpec
                    .builder("service", IDLService::class.asTypeName().copy(true), KModifier.OVERRIDE)
                    .initializer("service")
                    .build()
                val funcNamePropSpec = PropertySpec
                    .builder("funcName", String::class.asTypeName().copy(true), KModifier.OVERRIDE)
                    .initializer("funcName")
                    .build()
                val typeTableProcSpec = PropertySpec
                    .builder("typeTable", TypeTable::class)
                    .initializer(typeTable.poetize())
                    .build()

                val staticPayloadSize = MAGIC_PREFIX.size + typeTable.sizeBytes()
                val staticPayloadBuf = ByteBuffer.allocate(staticPayloadSize)
                staticPayloadBuf.put(MAGIC_PREFIX)
                typeTable.encode(staticPayloadBuf)
                staticPayloadBuf.rewind()
                val staticPayload = ByteArray(staticPayloadSize)
                staticPayloadBuf.get(staticPayload)
                val poetizedStaticPayload = staticPayload.poetize()

                val staticPayloadProp = PropertySpec
                    .builder("staticPayload", ByteArray::class)
                    .initializer(CodeBlock.of("%T.getDecoder().decode(\"$poetizedStaticPayload\")", Base64::class))
                    .build()

                callable
                    .primaryConstructor(constructorSpec)
                    .addProperty(servicePropSpec)
                    .addProperty(funcNamePropSpec)
                    .addProperty(typeTableProcSpec)
                    .addProperty(staticPayloadProp)
                    .addFunction(invoke.build())

                context.currentSpec.addType(callable.build())

                return funcTypeName
            }
            is IDLType.Reference.Service -> {
                val actorClassName = name ?: context.nextAnonymousTypeName()
                val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

                val constructorSpec = FunSpec.constructorBuilder()
                    .addParameter("id", ByteArray::class.asTypeName().copy(true))
                    .build()

                val idPropspec = PropertySpec
                    .builder("id", ByteArray::class.asTypeName().copy(true), KModifier.OVERRIDE)
                    .initializer("id")
                    .build()

                actorClassBuilder
                    .superclass(IDLService::class)
                    .primaryConstructor(constructorSpec)
                    .addProperty(idPropspec)

                for (method in type.methods) {
                    val methodFuncTypeName = transpileType(method.type as IDLType, context)
                    val propertySpec = PropertySpec
                        .builder(method.name, methodFuncTypeName)
                        .initializer(CodeBlock.of("%T(this, \"${method.name}\")", methodFuncTypeName))

                    actorClassBuilder.addProperty(propertySpec.build())
                }

                val actorClass = actorClassBuilder.build()
                context.currentSpec.addType(actorClass)

                return actorClassName
            }
            is IDLType.Reference.Principal -> Principal::class.asClassName()
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