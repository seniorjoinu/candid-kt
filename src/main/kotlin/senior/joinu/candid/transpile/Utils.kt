package senior.joinu.candid.transpile

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import senior.joinu.candid.*
import senior.joinu.candid.serialize.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    val recordBuilder = TypeSpec.classBuilder(recordName)
        .addModifiers(KModifier.OPEN)
        .addSuperinterface(IDLRecord::class)

    val fieldsProp = PropertySpec.builder(
        "fields",
        RecordSerIntermediate_typeName,
        KModifier.FINAL, KModifier.OVERRIDE, KModifier.LATEINIT
    )
    recordBuilder.addProperty(fieldsProp.build())

    val companionProp = PropertySpec
        .builder("companion", IDLRecordCompanion::class, KModifier.OVERRIDE)
        .initializer("%T.Companion", recordName)
    recordBuilder.addProperty(companionProp.build())

    val simpleConstructor = FunSpec.constructorBuilder()
        .addParameter("fields", RecordSerIntermediate_typeName)
        .addStatement("this.fields = fields")
    recordBuilder.addFunction(simpleConstructor.build())

    val actualConstructor = FunSpec.constructorBuilder()
    val actualConstructorStatements = mutableListOf<String>()
    type.fields.forEach { field ->
        val fieldName = field.name ?: field.idx.toString()
        val fieldTypeClassName = KtTranspiler.transpileType(field.type, context)

        actualConstructor.addParameter(fieldName, fieldTypeClassName)
        actualConstructorStatements.add("${field.idx} to $fieldName")

        val fieldProp = PropertySpec
            .builder(fieldName, fieldTypeClassName)
            .delegate("lazy { this.fields[${field.idx}] as %T }", fieldTypeClassName)
        recordBuilder.addProperty(fieldProp.build())
    }
    val actualConstructorBody = actualConstructorStatements.joinToString(prefix = "this.fields = mapOf(", postfix = ")")
    actualConstructor.addStatement(actualConstructorBody)
    recordBuilder.addFunction(actualConstructor.build())

    val recordCompanionBuilder = TypeSpec.companionObjectBuilder()
        .addSuperinterface(IDLRecordCompanion::class)

    val fromFunc = FunSpec.builder("from")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("fields", RecordSerIntermediate_typeName)
        .returns(IDLRecord::class)
        .addStatement("return %T(fields)", recordName)

    recordCompanionBuilder.addFunction(fromFunc.build())
    recordBuilder.addType(recordCompanionBuilder.build())

    context.currentSpec.addType(recordBuilder.build())

    return recordName
}

fun transpileVariant(name: ClassName?, type: IDLType.Constructive.Variant, context: TranspileContext): ClassName {
    val variantSuperName = name ?: context.nextAnonymousTypeName()
    val variantSuperBuilder = TypeSpec.classBuilder(variantSuperName).addModifiers(KModifier.SEALED)

    val idxToCompanionEntries = mutableListOf<String>()

    type.fields.forEach { field ->
        val variantName = field.name ?: field.idx.toString()
        val variantValueType = KtTranspiler.transpileType(field.type, context)
        val variantClassName = ClassName(context.packageName, variantName)

        idxToCompanionEntries.add(CodeBlock.of("${field.idx} to %T.Companion", variantClassName).toString())

        val constructor = FunSpec.constructorBuilder()
        val variantBuilder = TypeSpec.classBuilder(variantName)
            .addModifiers(KModifier.DATA)
            .superclass(variantSuperName)
            .addSuperinterface(IDLVariant::class.asClassName().parameterizedBy(variantValueType))

        val variantValueProp = PropertySpec.builder("value", variantValueType, KModifier.OVERRIDE)
            .initializer("value")

        constructor.addParameter("value", variantValueType)
        variantBuilder.addProperty(variantValueProp.build())
        variantBuilder.primaryConstructor(constructor.build())

        val idxProp = PropertySpec.builder("idx", Int::class, KModifier.OVERRIDE)
            .initializer("${field.idx}")
        variantBuilder.addProperty(idxProp.build())

        val variantCompanionBuilder = TypeSpec.companionObjectBuilder()
            .addSuperinterface(
                IDLVariantCompanion::class.asClassName().parameterizedBy(variantValueType, variantClassName)
            )

        val fromFunc = FunSpec.builder("from")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("value", variantValueType)
            .returns(variantClassName)
            .addStatement("return %T(value)", variantClassName)
        variantCompanionBuilder.addFunction(fromFunc.build())
        variantBuilder.addType(variantCompanionBuilder.build())
    }

    val variantsInitializer = idxToCompanionEntries.joinToString(prefix = "mapOf(", postfix = ")")
    val variantsProp = PropertySpec
        .builder(
            "variants",
            Map::class.asClassName().parameterizedBy(Int::class.asClassName(), IDLVariantCompanionType_typeName),
            KModifier.OVERRIDE
        )
        .initializer(variantsInitializer)
    val variantSuperCompanionBuilder = TypeSpec.companionObjectBuilder()
        .addSuperinterface(IDLVariantSuperCompanion::class)
        .addProperty(variantsProp.build())

    variantSuperBuilder.addType(variantSuperCompanionBuilder.build())

    context.currentSpec.addType(variantSuperBuilder.build())

    return variantSuperName
}

fun transpileFunc(name: ClassName?, type: IDLType.Reference.Func, context: TranspileContext): ClassName {
    val funcTypeName = name ?: context.nextAnonymousFuncTypeName()

    val callableCompanionBuilder = TypeSpec.companionObjectBuilder()
    val callable = TypeSpec.classBuilder(funcTypeName).superclass(AbstractIDLFunc::class)
    val invoke = FunSpec.builder("invoke").addModifiers(KModifier.SUSPEND, KModifier.OPERATOR)

    // ------------- serialization body pt.1 ------------------
    val bufSizeValInit = "val ${TC.bufSizeName} = ${TC.staticPayloadName}.size"
    val calculateMSize = if (type.arguments.isEmpty()) {
        bufSizeValInit
    } else {
        type.arguments.foldIndexed("$bufSizeValInit ") { idx, acc, arg ->
            val argName = arg.name ?: "arg$idx"
            val statement = KtSerializer.poetizeGetSizeBytesOfIDLValueInvocation(argName)

            "$acc + $statement"
        }
    }

    invoke
        .addStatement(calculateMSize)
        .addStatement("val ${TC.bufName} = %T.allocate(${TC.bufSizeName})", ByteBuffer::class)
        .addStatement("${TC.bufName}.order(%T.LITTLE_ENDIAN)", ByteOrder::class)
        .addStatement("${TC.bufName}.put(${TC.staticPayloadName})")
    // --------------------------------------------------------

    // ---------- transpile function arguments ---------------
    val typeTable = TypeTable()
    type.arguments.forEachIndexed { idx, arg ->
        val argName = arg.name ?: "arg$idx"
        val argTypeName = KtTranspiler.transpileType(arg.type, context)

        invoke.addParameter(argName, argTypeName)
        createTypeFieldInCompanion(argName, arg.type, callableCompanionBuilder)
        invoke.addStatement(KtSerializer.poetizeSerializeIDLValueInvocation(argName))

        context.typeTable.copyLabelsForType(arg.type, typeTable)
    }
    // -------------------------------------------------------

    // ---------- transpile function result value --------------
    val resultName = when {
        type.results.isNotEmpty() -> {
            val resultClassName = context.createResultValueTypeName(funcTypeName.simpleName)
            val resultClassBuilder = TypeSpec.classBuilder(resultClassName).addModifiers(KModifier.DATA)

            val constructorBuilder = FunSpec.constructorBuilder()
            type.results.forEachIndexed { idx, res ->
                val resName = res.name ?: "$idx"
                val resTypeName = KtTranspiler.transpileType(res.type, context)

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

    createField(
        callable,
        "service",
        AbstractIDLService::class.asTypeName().copy(true),
        constructorSpec,
        true
    )
    createField(
        callable,
        "funcName",
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

    val argTypes = type.arguments.map { it.type }

    val staticPayloadSize = MAGIC_PREFIX.size + argTypes.sizeBytes(typeTable) + typeTable.sizeBytes()
    val staticPayloadBuf = ByteBuffer.allocate(staticPayloadSize)
    staticPayloadBuf.put(MAGIC_PREFIX)
    typeTable.serialize(staticPayloadBuf)
    argTypes.serialize(staticPayloadBuf, typeTable)

    staticPayloadBuf.rewind()
    val staticPayload = ByteArray(staticPayloadSize)
    staticPayloadBuf.get(staticPayload)
    val poetizedStaticPayload = staticPayload.poetize()

    val staticPayloadProp = PropertySpec
        .builder(TC.staticPayloadName, ByteArray::class)
        .initializer(CodeBlock.of("%T.getDecoder().decode(\"$poetizedStaticPayload\")", Base64::class))
        .build()

    callableCompanionBuilder
        .addProperty(typeTableProcSpec)
        .addProperty(staticPayloadProp)

    callable
        .addFunction(invoke.build())
        .addType(callableCompanionBuilder.build())
    // --------------------------------------------------

    context.currentSpec.addType(callable.build())

    return funcTypeName
}

fun transpileService(name: ClassName?, type: IDLType.Reference.Service, context: TranspileContext): ClassName {
    val actorClassName = name ?: context.nextAnonymousTypeName()
    val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

    val constructorSpec = FunSpec.constructorBuilder()

    createField(
        actorClassBuilder,
        "id",
        ByteArray::class.asTypeName().copy(true),
        constructorSpec,
        true
    )

    actorClassBuilder
        .superclass(AbstractIDLService::class)
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
    val bufSizeName = "bufSize"
    val staticPayloadName = "staticPayload"
    val valueSerName = "valueSer"
    val typeSerName = "typeSer"
}

fun createIDLSerializableMethods(valueSerParameter: TypeName): Pair<FunSpec.Builder, FunSpec.Builder> {
    val serialize = FunSpec
        .builder(TC.serializeName)
        .addParameter(TC.bufName, ByteBuffer::class.asTypeName())
        .addParameter(TC.valueSerName, valueSerParameter)
        .returns(Unit::class)
        .addModifiers(KModifier.OVERRIDE)

    val sizeBytes = FunSpec
        .builder(TC.sizeBytesName)
        .addParameter(TC.typeTableName, TypeTable::class.asTypeName())
        .returns(Int::class)
        .addModifiers(KModifier.OVERRIDE)

    return serialize to sizeBytes
}

fun createIDLDeserializableMethods(forName: ClassName): FunSpec.Builder {
    return FunSpec.builder("deserialize")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(TC.bufName, ByteBuffer::class)
        .addParameter(TC.typeTableName, TypeTable::class)
        .returns(forName)
}

fun createTypeFieldInCompanion(name: String, type: IDLType, companion: TypeSpec.Builder) {
    val fieldType = PropertySpec
        .builder("${name}Type", IDLType::class.asTypeName())
        .initializer(type.poetize())

    companion.addProperty(fieldType.build())
}

fun createField(
    record: TypeSpec.Builder,
    name: String,
    typeName: TypeName,
    constructor: FunSpec.Builder?,
    override: Boolean = false
) {
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