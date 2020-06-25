package senior.joinu.candid.transpile

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.whispersystems.curve25519.Curve25519KeyPair
import senior.joinu.candid.*
import senior.joinu.candid.serialize.FuncValueSer
import senior.joinu.candid.serialize.ServiceValueSer
import senior.joinu.candid.serialize.ValueSer
import senior.joinu.candid.serialize.getTypeSerForType
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
        ClassName(packageName, "${funcName}Result")

    fun createValueSerTypeName(typeName: String) =
        ClassName(packageName, "${typeName}ValueSer")
}

fun transpileRecord(
    name: ClassName?,
    type: IDLType.Constructive.Record,
    context: TranspileContext
): Pair<TypeName, CodeBlock> {
    val recordName = name ?: context.nextAnonymousTypeName()
    val hasFields = type.fields.isNotEmpty()

    val recordBuilder = TypeSpec.classBuilder(recordName)
    if (hasFields) recordBuilder.addModifiers(KModifier.DATA)

    val primaryConstructor = FunSpec.constructorBuilder()

    val serName = context.createValueSerTypeName(recordName.simpleName)
    val serBuilder = TypeSpec.objectBuilder(serName)
        .addSuperinterface(ValueSer::class.asClassName().parameterizedBy(recordName))

    val calcSizeBytesFunc = FunSpec.builder("calcSizeBytes")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("value", recordName)
        .returns(Int::class)
    val calcSizeBytesFuncStatements = mutableListOf<String>()

    val serFunc = FunSpec.builder("ser")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("buf", ByteBuffer::class)
        .addParameter("value", recordName)

    val deserFunc = FunSpec.builder("deser")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("buf", ByteBuffer::class)
        .returns(recordName)
    val deserFuncStatements = mutableListOf<String>()

    type.fields.forEach { field ->
        val fieldName = field.name ?: field.idx.toString()
        val (fieldClassName, fieldSer) = KtTranspiler.transpileTypeAndValueSer(field.type, context)

        primaryConstructor.addParameter(fieldName, fieldClassName)
        val fieldProp = PropertySpec.builder(fieldName, fieldClassName)
            .initializer(fieldName)
        recordBuilder.addProperty(fieldProp.build())

        val fieldValueSerPropName = context.createValueSerTypeName(fieldName).simpleName
        val fieldValueSerProp = PropertySpec.builder(
            fieldValueSerPropName,
            ValueSer::class.asClassName().parameterizedBy(fieldClassName)
        ).initializer(fieldSer)
        serBuilder.addProperty(fieldValueSerProp.build())

        calcSizeBytesFuncStatements.add("this.${fieldValueSerPropName.escapeIfNecessary()}.calcSizeBytes(value.${fieldName.escapeIfNecessary()})")

        serFunc.addStatement("this.${fieldValueSerPropName.escapeIfNecessary()}.ser(buf, value.${fieldName.escapeIfNecessary()})")

        deserFuncStatements.add("this.${fieldValueSerPropName.escapeIfNecessary()}.deser(buf)")
    }
    recordBuilder.primaryConstructor(primaryConstructor.build())

    if (!hasFields) calcSizeBytesFuncStatements.add("0")

    val calcSizeBytesFuncBody = calcSizeBytesFuncStatements.joinToString(" + ", "return ")
    calcSizeBytesFunc.addStatement(calcSizeBytesFuncBody)
    serBuilder.addFunction(calcSizeBytesFunc.build())

    serBuilder.addFunction(serFunc.build())

    val deserFuncBody = deserFuncStatements.joinToString(prefix = "return ${recordName.simpleName}(", postfix = ")")
    deserFunc.addStatement(deserFuncBody)
    serBuilder.addFunction(deserFunc.build())

    val poetizeFunc = FunSpec.builder("poetize")
        .addModifiers(KModifier.OVERRIDE)
        .returns(String::class)
        .addStatement("return %T.of(%S, ${serName.simpleName}::class).toString()", CodeBlock::class, "%T")
    serBuilder.addFunction(poetizeFunc.build())

    context.currentSpec.addType(recordBuilder.build())
    context.currentSpec.addType(serBuilder.build())

    return Pair(recordName, CodeBlock.of("%T", serName))
}

fun transpileVariant(
    name: ClassName?,
    type: IDLType.Constructive.Variant,
    context: TranspileContext
): Pair<ClassName, CodeBlock> {
    val variantSuperName = name ?: context.nextAnonymousTypeName()
    val variantSuperBuilder = TypeSpec.classBuilder(variantSuperName).addModifiers(KModifier.SEALED)

    val variantSuperValueSerName = context.createValueSerTypeName(variantSuperName.simpleName)
    val variantSuperValueSerBuilder = TypeSpec.objectBuilder(variantSuperValueSerName)
        .addSuperinterface(ValueSer::class.asClassName().parameterizedBy(variantSuperName))

    val calcSizeBytesFunc = FunSpec.builder("calcSizeBytes")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("value", variantSuperName)
        .returns(Int::class)
    val calcSizeBytesFuncStatements = mutableListOf<String>()

    val serFunc = FunSpec.builder("ser")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("buf", ByteBuffer::class)
        .addParameter("value", variantSuperName)
    val serFuncStatements = mutableListOf<String>()

    val deserFunc = FunSpec.builder("deser")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("buf", ByteBuffer::class)
        .returns(variantSuperName)
        .addStatement("val idx = %T.readUnsigned(buf)", Leb128::class)
    val deserFuncStatements = mutableListOf<String>()

    type.fields.forEach { field ->
        val variantName = field.name ?: field.idx.toString()
        val (variantValueType, variantValueSer) = KtTranspiler.transpileTypeAndValueSer(field.type, context)
        val variantClassName = ClassName(context.packageName, variantName)

        val constructor = FunSpec.constructorBuilder()
        val variantBuilder = TypeSpec.classBuilder(variantName)
            .addModifiers(KModifier.DATA)
            .superclass(variantSuperName)

        val variantValueProp = PropertySpec.builder("value", variantValueType)
            .initializer("value")

        constructor.addParameter("value", variantValueType)
        variantBuilder.addProperty(variantValueProp.build())
        variantBuilder.primaryConstructor(constructor.build())

        variantSuperBuilder.addType(variantBuilder.build())

        calcSizeBytesFuncStatements.add(
            CodeBlock.of(
                "is %T.%T -> %T.sizeUnsigned(${field.idx}) + ${variantValueSer}.calcSizeBytes(value.value)",
                variantSuperName, variantClassName, Leb128::class
            ).toString()
        )

        serFuncStatements.add(
            CodeBlock.of(
                """
                    is %T.%T -> {
                        %T.writeUnsigned(buf, ${field.idx})
                        ${variantValueSer}.ser(buf, value.value)
                    }
                """.trimIndent(),
                variantSuperName, variantClassName, Leb128::class
            ).toString()
        )

        deserFuncStatements.add(
            CodeBlock.of(
                """
                    ${field.idx} -> %T.%T(${variantValueSer}.deser(buf))
                """.trimIndent(),
                variantSuperName, variantClassName
            ).toString()
        )
    }

    val calcSizeBytesFuncBody = calcSizeBytesFuncStatements.joinToString("\n", "return when (value) {\n", "\n}")
    calcSizeBytesFunc.addStatement(calcSizeBytesFuncBody)
    variantSuperValueSerBuilder.addFunction(calcSizeBytesFunc.build())

    val serFuncBody = serFuncStatements.joinToString("\n", "when (value) {\n", "\n}")
    serFunc.addStatement(serFuncBody)
    variantSuperValueSerBuilder.addFunction(serFunc.build())

    deserFuncStatements.add(
        CodeBlock.of(
            "else -> throw %T(\"Unknown·idx·met·during·variant·deserialization\")",
            RuntimeException::class
        ).toString()
    )
    val deserFuncBody = deserFuncStatements.joinToString("\n", "return when (idx) {\n", "\n}")
    deserFunc.addStatement(deserFuncBody)
    variantSuperValueSerBuilder.addFunction(deserFunc.build())

    val poetizeFunc = FunSpec.builder("poetize")
        .addModifiers(KModifier.OVERRIDE)
        .returns(String::class)
        .addStatement("return %T.of(%S, ${variantSuperValueSerName}::class).toString()", CodeBlock::class, "%T")
    variantSuperValueSerBuilder.addFunction(poetizeFunc.build())

    context.currentSpec.addType(variantSuperBuilder.build())
    context.currentSpec.addType(variantSuperValueSerBuilder.build())

    return Pair(variantSuperName, CodeBlock.of("%T", variantSuperValueSerName))
}

fun deserUntilM(buf: ByteBuffer): TypeTable {
    TODO()
}

suspend fun receive(): ByteBuffer {
    TODO()
}

fun transpileFunc(name: ClassName?, type: IDLType.Reference.Func, context: TranspileContext): ClassName {
    val funcTypeName = name ?: context.nextAnonymousFuncTypeName()

    // making a type alias for value ser of this func
    val funcValueSerName = context.createValueSerTypeName(funcTypeName.simpleName)
    val funcValueSerTypeAlias = TypeAliasSpec.builder(funcValueSerName.simpleName, FuncValueSer::class)
    context.currentSpec.addTypeAlias(funcValueSerTypeAlias.build())

    // creating a func object
    val funcBuilder = TypeSpec.classBuilder(funcTypeName)
        .superclass(SimpleIDLFunc::class)
        .addSuperclassConstructorParameter("funcName")
        .addSuperclassConstructorParameter("service")
    val funcConstructor = FunSpec.constructorBuilder()
        .addParameter("funcName", String::class.asTypeName().copy(true))
        .addParameter("service", SimpleIDLService::class.asTypeName().copy(true))
    funcBuilder.primaryConstructor(funcConstructor.build())

    // computing bytes for magic prefix + type table + types
    val requestTypeTable = TypeTable()
    val argTypeSers = type.arguments.map { arg ->
        context.typeTable.copyLabelsForType(arg.type, requestTypeTable)
        getTypeSerForType(arg.type, requestTypeTable)
    }
    val typesSizeBytes = Leb128.sizeUnsigned(argTypeSers.size) + argTypeSers.map { it.calcTypeSizeBytes() }.sum()
    val staticPayloadSize = MAGIC_PREFIX.size + requestTypeTable.sizeBytes() + typesSizeBytes
    val staticPayloadBuf = ByteBuffer.allocate(staticPayloadSize)
    staticPayloadBuf.put(MAGIC_PREFIX)
    requestTypeTable.serialize(staticPayloadBuf)

    Leb128.writeUnsigned(staticPayloadBuf, argTypeSers.size)
    argTypeSers.forEach { it.serType(staticPayloadBuf) }

    staticPayloadBuf.rewind()
    val staticPayload = ByteArray(staticPayloadSize)
    staticPayloadBuf.get(staticPayload)
    val poetizedStaticPayload = staticPayload.poetize()

    // "baking" all these bytes into the class
    val staticPayloadProp = PropertySpec.builder("staticPayload", ByteArray::class)
        .initializer("%T.getDecoder().decode(\"$poetizedStaticPayload\")", Base64::class)
    funcBuilder.addProperty(staticPayloadProp.build())

    // transpilling function return value
    val returnValueTypeName = context.createResultValueTypeName(funcTypeName.simpleName)
    val hasResults = type.results.isNotEmpty()

    val returnValueBuilder = TypeSpec.classBuilder(returnValueTypeName.simpleName)
    if (hasResults) returnValueBuilder.addModifiers(KModifier.DATA)

    val returnValueConstructor = FunSpec.constructorBuilder()
    val deserStatements = mutableListOf<String>()
    val returnValuesValueSers = type.results.mapIndexed { idx, res ->
        val (resClassName, resSer) = KtTranspiler.transpileTypeAndValueSer(res.type, context)

        val resProp = PropertySpec.builder(idx.toString(), resClassName).initializer(idx.toString())
        returnValueBuilder.addProperty(resProp.build())
        returnValueConstructor.addParameter(idx.toString(), resClassName)

        deserStatements.add(CodeBlock.of("${resSer}.deser(receiveBuf) as %T", resClassName).toString())

        resSer
    }
    returnValueBuilder.primaryConstructor(returnValueConstructor.build())
    context.currentSpec.addType(returnValueBuilder.build())

    // stuffing with actual ser/deser/request/response logic
    val invoke = FunSpec.builder("invoke").addModifiers(KModifier.SUSPEND, KModifier.OPERATOR)
        .returns(returnValueTypeName)

    val valueSizeBytesStatements = mutableListOf("0")
    val serStatements = mutableListOf<String>()
    type.arguments.forEachIndexed { idx, arg ->
        val argName = "arg$idx"
        val (argClassName, argSer) = KtTranspiler.transpileTypeAndValueSer(arg.type, context)

        val argValueSerName = "${argName}ValueSer"
        invoke.addParameter(argName, argClassName)
            .addStatement("val $argValueSerName = $argSer")

        valueSizeBytesStatements.add("${argValueSerName}.calcSizeBytes($argName)")
        serStatements.add("${argValueSerName}.ser(sendBuf, $argName)")
    }

    val valueSizeBytesBody = valueSizeBytesStatements.joinToString(" + ", "val valueSizeBytes = ")
    invoke.addStatement(valueSizeBytesBody)

    invoke.addStatement("val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)")
    invoke.addStatement("sendBuf.order(%T.LITTLE_ENDIAN)", ByteOrder::class)
    invoke.addStatement("sendBuf.put(staticPayload)")
    serStatements.forEach { invoke.addStatement(it) }

    invoke.addStatement("val sendBytes = %T(staticPayload.size + valueSizeBytes)", ByteArray::class)
    invoke.addStatement("sendBuf.rewind()")
    invoke.addStatement("sendBuf.get(sendBytes)")
    invoke.addStatement("")

    val sendFuncName = if (type.annotations.contains(IDLFuncAnn.Query)) {
        "query"
    } else {
        "call"
    }
    invoke.addStatement("val receiveBytes = this.service!!.${sendFuncName}(this.funcName!!, sendBytes)")
    invoke.addStatement("val receiveBuf = %T.allocate(receiveBytes.size)", ByteBuffer::class)
    invoke.addStatement("receiveBuf.order(%T.LITTLE_ENDIAN)", ByteOrder::class)

    // TODO: transpile deserUntilM logic
    invoke.addStatement("val responseTypeTable = %T()", TypeTable::class)

    val deserBody = deserStatements.joinToString(
        prefix = "return ${returnValueTypeName.simpleName}(",
        postfix = ")"
    )
    invoke.addStatement(deserBody)

    funcBuilder.addFunction(invoke.build())
    context.currentSpec.addType(funcBuilder.build())

    return funcTypeName
}

fun transpileService(name: ClassName?, type: IDLType.Reference.Service, context: TranspileContext): ClassName {
    val actorClassName = name ?: context.nextAnonymousTypeName()
    val actorClassBuilder = TypeSpec.classBuilder(actorClassName)
        .superclass(SimpleIDLService::class)
        .addSuperclassConstructorParameter("host")
        .addSuperclassConstructorParameter("id")
        .addSuperclassConstructorParameter("keyPair")

    // making a type alias for service value ser
    val actorValueSerName = context.createValueSerTypeName(actorClassName.simpleName)
    val actorTypeAlias = TypeAliasSpec.builder(actorValueSerName.simpleName, ServiceValueSer::class)
    context.currentSpec.addTypeAlias(actorTypeAlias.build())

    // creating a constructor
    val constructorSpec = FunSpec.constructorBuilder()
        .addParameter("host", String::class)
        .addParameter("id", ByteArray::class.asTypeName().copy(true))
        .addParameter("keyPair", Curve25519KeyPair::class.asTypeName().copy(true))
    actorClassBuilder.primaryConstructor(constructorSpec.build())

    // adding methods
    type.methods.forEach { (methodName, methodType) ->
        val (methodClassName, _) = KtTranspiler.transpileTypeAndValueSer(methodType as IDLType, context)

        val methodProp = PropertySpec.builder(methodName, methodClassName)
            .initializer("%T(\"$methodName\", this)", methodClassName)
        actorClassBuilder.addProperty(methodProp.build())
    }

    context.currentSpec.addType(actorClassBuilder.build())

    return actorClassName
}