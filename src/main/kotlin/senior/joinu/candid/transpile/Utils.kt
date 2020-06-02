package senior.joinu.candid.transpile

import com.squareup.kotlinpoet.*
import senior.joinu.candid.*
import senior.joinu.leb128.Leb128
import java.nio.ByteBuffer
import java.util.*


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

fun transpileRecord(name: ClassName?, type: IDLType.Constructive.Record, context: TranspileContext): ClassName {
    val recordName = name ?: context.nextAnonymousTypeName()

    val recordBuilder = if (type.fields.isEmpty()) {
        TypeSpec.objectBuilder(recordName)
    } else {
        val dataClassBuilder = TypeSpec.classBuilder(recordName).addModifiers(KModifier.DATA)
        val constructorBuilder = FunSpec.constructorBuilder()
        type.fields.forEachIndexed { idx, field ->
            val fieldName = field.name ?: idx.toString()
            val fieldTypeName = KtTranspiler.transpileType(field.type, context)

            createRecordField(dataClassBuilder, fieldName, field.type, fieldTypeName, constructorBuilder)
        }

        dataClassBuilder.primaryConstructor(constructorBuilder.build())
    }

    recordBuilder.addSuperinterface(IDLSerializable::class.asClassName())
    val (serializeFunc, sizeBytesFunc) = createIDLSerializableMethods()

    // serialize body
    type.fields.forEachIndexed { idx, field ->
        val fieldName = field.name ?: idx.toString()
        serializeFunc.addStatement(KtSerilializer.poetizeSerializeIDLValueInvocation(fieldName))
    }
    // sizeBytesBody
    val sizeBytesStatement = if (type.fields.isEmpty()) {
        "return 0"
    } else {
        type.fields.foldIndexed("return ") { idx, acc, field ->
            val fieldName = field.name ?: idx.toString()
            val calcSizeStatement = KtSerilializer.poetizeGetSizeBytesOfIDLValueInvocation(fieldName)

            "$acc + $calcSizeStatement"
        }
    }
    sizeBytesFunc.addStatement(sizeBytesStatement)

    recordBuilder.addFunction(serializeFunc.build())
    recordBuilder.addFunction(sizeBytesFunc.build())

    context.currentSpec.addType(recordBuilder.build())

    return recordName
}

fun transpileVariant(name: ClassName?, type: IDLType.Constructive.Variant, context: TranspileContext): ClassName {
    val variantName = name ?: context.nextAnonymousTypeName()

    val variantBuilder = if (type.fields.isEmpty()) {
        TypeSpec.objectBuilder(variantName)
    } else {
        val variantBuilder = TypeSpec.classBuilder(variantName).addModifiers(KModifier.SEALED)
        type.fields.forEachIndexed { idx, field ->
            val fieldName = field.name ?: idx.toString()
            val fieldTypeName = KtTranspiler.transpileType(field.type, context)

            val constructor = FunSpec
                .constructorBuilder()
            val sealedSubclass = TypeSpec
                .classBuilder(fieldName)
                .addModifiers(KModifier.DATA)
                .superclass(variantName)

            createRecordField(sealedSubclass, "value", field.type, fieldTypeName, constructor)

            sealedSubclass
                .primaryConstructor(constructor.build())
                .addSuperinterface(IDLSerializable::class.asClassName())
            val (serializeFunc, sizeBytesFunc) = createIDLSerializableMethods()

            // serialize body
            serializeFunc.addStatement("%T.writeUnsigned(${TC.bufName}, ${idx}.toUInt())", Leb128::class)
            serializeFunc.addStatement(KtSerilializer.poetizeSerializeIDLValueInvocation("value"))
            // sizeBytes body
            sizeBytesFunc.addStatement(
                "return %T.sizeUnsigned($idx) + ${KtSerilializer.poetizeGetSizeBytesOfIDLValueInvocation("value")}",
                Leb128::class
            )

            sealedSubclass.addFunction(serializeFunc.build())
            sealedSubclass.addFunction(sizeBytesFunc.build())

            variantBuilder.addType(sealedSubclass.build())
        }
        variantBuilder
    }

    variantBuilder.addSuperinterface(IDLSerializable::class.asClassName())
    val (serializeFunc, sizeBytesFunc) = createIDLSerializableMethods()

    // serialize body
    serializeFunc.addStatement(
        "throw %T(\"Unable·to·serialize·sealed·superclass·$variantName\")",
        RuntimeException::class
    )
    // sizeBytes body
    sizeBytesFunc.addStatement(
        "throw %T(\"Unable·to·get·byte·size·of·sealed·superclass·$variantName\")",
        RuntimeException::class
    )

    variantBuilder.addFunction(serializeFunc.build())
    variantBuilder.addFunction(sizeBytesFunc.build())

    val variant = variantBuilder.build()
    context.currentSpec.addType(variant)

    return variantName
}

fun transpileFunc(name: ClassName?, type: IDLType.Reference.Func, context: TranspileContext): ClassName {
    val funcTypeName = name ?: context.nextAnonymousFuncTypeName()

    val callable = TypeSpec.classBuilder(funcTypeName).superclass(IDLFunc::class)
    val invoke = FunSpec.builder("invoke").addModifiers(KModifier.SUSPEND, KModifier.OPERATOR)

    // ------------- serialization body pt.1 ------------------
    val calculateMSize = if (type.arguments.isEmpty()) {
        "val ${TC.mSizeName} = 0"
    } else {
        type.arguments.foldIndexed("val ${TC.mSizeName} = ") { idx, acc, arg ->
            val argName = arg.name ?: "arg$idx"
            val statement = KtSerilializer.poetizeGetSizeBytesOfIDLValueInvocation(argName)

            "$acc + $statement"
        }
    }

    invoke
        .addStatement(calculateMSize)
        .addStatement("val ${TC.bufName} = %T.allocate(${TC.mSizeName})", ByteBuffer::class)
    // --------------------------------------------------------

    // ---------- transpile function arguments ---------------
    val typeTable = TypeTable()
    type.arguments.forEachIndexed { idx, arg ->
        val argName = arg.name ?: "arg$idx"
        val argTypeName = KtTranspiler.transpileType(arg.type, context)

        invoke.addParameter(argName, argTypeName)
        createRecordField(callable, argName, arg.type, null, null)
        invoke.addStatement(KtSerilializer.poetizeSerializeIDLValueInvocation(argName))

        context.typeTable.copyLabelsForType(arg.type, typeTable)
    }
    // -------------------------------------------------------

    // ---------- transpile function result value --------------
    val resultName = when {
        type.results.size == 1 -> {
            val result = type.results.first()

            KtTranspiler.transpileType(result.type, context)
        }
        type.results.isNotEmpty() -> {
            val resultClassName = context.createResultValueTypeName(funcTypeName.simpleName)
            val resultClassBuilder = TypeSpec.classBuilder(resultClassName).addModifiers(KModifier.DATA)

            val constructorBuilder = FunSpec.constructorBuilder()
            type.results.forEachIndexed { idx, res ->
                val resName = res.name ?: "$idx"
                val resTypeName = KtTranspiler.transpileType(type, context)

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
    // -----------------------------------------------------

    // ----------- implement of IDLFunc ------------------
    val constructorSpec = FunSpec.constructorBuilder()

    createRecordField(
        callable,
        "service",
        null,
        IDLService::class.asTypeName().copy(true),
        constructorSpec,
        true
    )
    createRecordField(
        callable,
        "funcName",
        null,
        String::class.asTypeName().copy(true),
        constructorSpec,
        true
    )

    callable.primaryConstructor(constructorSpec.build())

    // -----------------------------------------------

    // -------------- serialization body pt.2 -------------
    val typeTableProcSpec = PropertySpec
        .builder(TC.typeTableName, TypeTable::class)
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
        .addProperty(typeTableProcSpec)
        .addProperty(staticPayloadProp)
        .addFunction(invoke.build())
    // --------------------------------------------------

    context.currentSpec.addType(callable.build())

    return funcTypeName
}

fun transpileService(name: ClassName?, type: IDLType.Reference.Service, context: TranspileContext): ClassName {
    val actorClassName = name ?: context.nextAnonymousTypeName()
    val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

    val constructorSpec = FunSpec.constructorBuilder()

    createRecordField(
        actorClassBuilder,
        "id",
        null,
        ByteArray::class.asTypeName().copy(true),
        constructorSpec,
        true
    )

    actorClassBuilder
        .superclass(IDLService::class)
        .primaryConstructor(constructorSpec.build())

    for (method in type.methods) {
        val methodFuncTypeName =
            KtTranspiler.transpileType(method.type as IDLType, context)

        val propertySpec = PropertySpec
            .builder(method.name, methodFuncTypeName)
            .initializer(CodeBlock.of("%T(this, \"${method.name}\")", methodFuncTypeName))

        actorClassBuilder.addProperty(propertySpec.build())
    }

    val actorClass = actorClassBuilder.build()
    context.currentSpec.addType(actorClass)

    return actorClassName
}

object TC {
    val serializeName = "serialize"
    val sizeBytesName = "sizeBytes"
    val bufName = "buf"
    val typeTableName = "typeTable"
    val mSizeName = "mSize"
}

fun createIDLSerializableMethods(): Pair<FunSpec.Builder, FunSpec.Builder> {
    val serialize = FunSpec
        .builder(TC.serializeName)
        .addParameter(TC.bufName, ByteBuffer::class.asTypeName())
        .addParameter(TC.typeTableName, TypeTable::class.asTypeName())
        .returns(Unit::class)
        .addModifiers(KModifier.OVERRIDE)

    val sizeBytes = FunSpec
        .builder(TC.sizeBytesName)
        .addParameter(TC.typeTableName, TypeTable::class.asTypeName())
        .returns(Int::class)
        .addModifiers(KModifier.OVERRIDE)

    return serialize to sizeBytes
}

fun createRecordField(
    record: TypeSpec.Builder,
    name: String,
    type: IDLType?,
    typeName: TypeName?,
    constructor: FunSpec.Builder?,
    override: Boolean = false
) {
    if (typeName != null) {
        val field = PropertySpec
            .builder(name, typeName)

        if (override) {
            field.addModifiers(KModifier.OVERRIDE)
        }

        if (constructor != null) {
            field.initializer(name)
            constructor.addParameter(name, typeName)
        }

        record.addProperty(field.build())
    }

    if (type != null) {
        val fieldType = PropertySpec
            .builder("${name}Type", IDLType::class.asTypeName())
            .initializer(type.poetize())
        record.addProperty(fieldType.build())
    }
}