package senior.joinu.candid.transpile

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import senior.joinu.candid.*
import senior.joinu.candid.serialize.IDLDeserializable
import senior.joinu.candid.serialize.IDLSerializable
import senior.joinu.leb128.Leb128
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
    val recordBuilder = TypeSpec.classBuilder(recordName).addModifiers(KModifier.OPEN)

    recordBuilder.addSuperinterface(IDLSerializable::class.asClassName())
    val (serializeFunc, sizeBytesFunc) = createIDLSerializableMethods()
    var sizeBytesStatement = "return 0"

    val recordCompanionBuilder = TypeSpec.companionObjectBuilder()
        .addSuperinterface(IDLDeserializable::class.asClassName().parameterizedBy(recordName))
    val deserializeFunc = createIDLDeserializableMethods(recordName)
    val deserializeFuncStatements = mutableListOf<CodeBlock>()

    val constructorBuilder = FunSpec.constructorBuilder()
    type.fields.forEach { field ->
        val fieldName = field.name ?: field.idx.toString()
        val fieldTypeClassName = KtTranspiler.transpileType(field.type, context)

        createField(recordBuilder, fieldName, fieldTypeClassName, constructorBuilder)
        createTypeFieldInCompanion(fieldName, field.type, recordCompanionBuilder)

        serializeFunc.addStatement(KtSerializer.poetizeSerializeIDLValueInvocation(fieldName))
        val calcSizeStatement = KtSerializer.poetizeGetSizeBytesOfIDLValueInvocation(fieldName)
        sizeBytesStatement += " + $calcSizeStatement"

        val fieldTypeName = "${fieldName}Type".escapeIfNecessary()
        val escapedFieldName = fieldName.escapeIfNecessary()
        val companion = if (field.type is IDLType.Constructive.Record) {
            CodeBlock.of("%T.Companion", fieldTypeClassName).toString()
        } else {
            "null"
        }
        deserializeFuncStatements.add(
            CodeBlock.of(
                "$escapedFieldName = ${IDLDeserializer.poetizeDeserializeIDLValueInvocation(
                    fieldTypeName,
                    companion
                )} as %T",
                fieldTypeClassName
            )
        )
    }

    val deserializeStatement =
        CodeBlock.of("return %T(${deserializeFuncStatements.joinToString { it.toString() }})", recordName).toString()
    deserializeFunc.addStatement(deserializeStatement)
    recordCompanionBuilder.addFunction(deserializeFunc.build())
    sizeBytesFunc.addStatement(sizeBytesStatement)

    recordBuilder
        .primaryConstructor(constructorBuilder.build())
        .addFunction(serializeFunc.build())
        .addFunction(sizeBytesFunc.build())
        .addType(recordCompanionBuilder.build())

    context.currentSpec.addType(recordBuilder.build())

    return recordName
}

fun transpileVariant(name: ClassName?, type: IDLType.Constructive.Variant, context: TranspileContext): ClassName {
    val variantName = name ?: context.nextAnonymousTypeName()
    val variantBuilder = TypeSpec.classBuilder(variantName).addModifiers(KModifier.SEALED)

    val variantCompanionBuilder = TypeSpec.companionObjectBuilder()
        .addSuperinterface(IDLDeserializable::class.asClassName().parameterizedBy(variantName))

    val deserializeFunc = createIDLDeserializableMethods(variantName)
    deserializeFunc
        .addStatement("val idx = %T.readUnsigned(${TC.bufName}).toInt()", Leb128::class)
        .addStatement("val companion = idxToCompanion[idx]!!")
        .addStatement("return companion.deserialize(${TC.bufName}, ${TC.typeTableName})")
    variantCompanionBuilder.addFunction(deserializeFunc.build())

    val idxToCompanionMappings = mutableListOf<CodeBlock>()

    type.fields.forEach { field ->
        val fieldName = field.name ?: field.idx.toString()
        val fieldTypeName = KtTranspiler.transpileType(field.type, context)
        val fieldNameClassName = ClassName(context.packageName, fieldName)

        idxToCompanionMappings.add(CodeBlock.of("${field.idx} to %T.Companion", fieldNameClassName))

        val sealedSubclassCompanionBuilder = TypeSpec.companionObjectBuilder()
            .addSuperinterface(IDLDeserializable::class.asClassName().parameterizedBy(variantName))
        val deserializeFieldFunc = createIDLDeserializableMethods(fieldNameClassName)
        val companionName =
            if ((field.type is IDLType.Constructive.Record) or (field.type is IDLType.Constructive.Variant)) {
                CodeBlock.of("%T.Companion", fieldNameClassName).toString()
            } else {
                "null"
            }
        val deserializeStatement = IDLDeserializer.poetizeDeserializeIDLValueInvocation("valueType", companionName)
        deserializeFieldFunc.addStatement("return %T($deserializeStatement as %T)", fieldNameClassName, fieldTypeName)

        val constructor = FunSpec.constructorBuilder()
        val sealedSubclass = TypeSpec
            .classBuilder(fieldName)
            .addModifiers(KModifier.DATA)
            .superclass(variantName)

        createField(sealedSubclass, "value", fieldTypeName, constructor)
        createTypeFieldInCompanion("value", field.type, sealedSubclassCompanionBuilder)

        sealedSubclass
            .primaryConstructor(constructor.build())
            .addSuperinterface(IDLSerializable::class.asClassName())
        val (serializeFunc, sizeBytesFunc) = createIDLSerializableMethods()

        // serialize body
        serializeFunc
            .addStatement("%T.writeUnsigned(${TC.bufName}, ${field.idx}.toUInt())", Leb128::class)
            .addStatement(KtSerializer.poetizeSerializeIDLValueInvocation("value"))
        // sizeBytes body
        sizeBytesFunc.addStatement(
            "return %T.sizeUnsigned(${field.idx}) + ${KtSerializer.poetizeGetSizeBytesOfIDLValueInvocation("value")}",
            Leb128::class
        )

        sealedSubclassCompanionBuilder.addFunction(deserializeFieldFunc.build())
        sealedSubclass
            .addFunction(serializeFunc.build())
            .addFunction(sizeBytesFunc.build())
            .addType(sealedSubclassCompanionBuilder.build())

        variantBuilder.addType(sealedSubclass.build())
    }

    val idxToCompanionField = PropertySpec.builder(
        "idxToCompanion",
        Map::class.asClassName().parameterizedBy(
            Int::class.asClassName(),
            IDLDeserializable::class.asClassName().parameterizedBy(variantName)
        )
    ).initializer("mapOf(${idxToCompanionMappings.joinToString { it.toString() }})")
    variantCompanionBuilder.addProperty(idxToCompanionField.build())

    variantBuilder.addType(variantCompanionBuilder.build())
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