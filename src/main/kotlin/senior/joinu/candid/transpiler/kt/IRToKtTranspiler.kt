
package senior.joinu.candid.transpiler.kt
/*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import senior.joinu.candid.parser.*
import java.math.BigInteger
import kotlin.reflect.KClass

interface IRTranspiler<T> {
    fun transpile(program: IDLIRProgram): T
}

class IRToKtTranspiler(val resultPackageName: String, val resultFileName: String) : IRTranspiler<FileSpec> {
    var currentSpec: FileSpec.Builder = FileSpec.builder(resultPackageName, "$resultFileName.kt")
    var anonymousTypeCount = 0
    private var used = false

    private fun nextAnonymousTypeName(`package`: String = resultPackageName) =
        ClassName(`package`, "AnonIDLType${anonymousTypeCount++}")

    private fun nextAnonymousFuncTypeName(`package`: String = resultPackageName) =
        ClassName(`package`, "AnonFunc${anonymousTypeCount++}")

    private fun createResultValueTypeName(funcName: String, `package`: String = resultPackageName) =
        ClassName(`package`, "${funcName.capitalize()}Result")

    override fun transpile(program: IDLIRProgram): FileSpec {
        require(!used) { "Transpiler instance was already used!" }

        program.importDefinitions.forEach {
            transpileImportDef(it)
        }

        program.typeDefinitions.forEach {
            val typeName = transpileTypeDef(it.description)

            val typeAlias = TypeAliasSpec.builder(it.name, typeName).build()
            currentSpec.addTypeAlias(typeAlias)
        }

        if (program.actor != null) {
            val actorName = program.actor.name ?: "${resultFileName}MainActor"
            val actorTypeDef = program.actor.type

            transpileActorTypeDef(actorTypeDef, ClassName(resultPackageName, actorName))
        }

        used = true
        return currentSpec.build()
    }

    // TODO: enable imports
    fun transpileImportDef(def: IDLImportDefinition) {
    }

    fun transpileTypeDef(description: IDLTypeDescription): TypeName {
        return when (description.typeDescriptionKind) {
            EIDLDescriptionTypeKind.PrimitiveType -> {
                val klass = transpilePrimitiveTypeDef(description as IDLPrimitiveType)

                klass.asTypeName()
            }
            EIDLDescriptionTypeKind.Id -> {
                val type = description as IDLIdType

                ClassName(resultPackageName, type.id)
            }
            EIDLDescriptionTypeKind.ReferenceType -> {
                val ref = description as IDLReferenceType

                when (ref.type) {
                    EIDLReferenceTypeKind.Principal -> IDLPrincipal::class.asTypeName()
                    EIDLReferenceTypeKind.Entity -> {
                        val innerDescription = ref.description!!

                        when (innerDescription.referenceDescriptionKind) {
                            EIDLReferenceKind.Function -> {
                                transpileFuncTypeDef(innerDescription as IDLFunctionDefinition)
                            }
                            EIDLReferenceKind.Actor -> {
                                val actorDef = innerDescription as IDLActorTypeDefinition

                                transpileActorTypeDef(actorDef, null)
                            }
                        }
                    }
                }
            }
            EIDLDescriptionTypeKind.ConstructiveType -> {
                val cons = description as IDLConstructiveType

                when (cons.type) {
                    EIDLConstructiveTypeKind.Blob -> {
                        ByteArray::class.asTypeName()
                    }
                    EIDLConstructiveTypeKind.Opt -> {
                        val innerDescription = cons.description as IDLTypeDescription
                        val typeName = transpileTypeDef(innerDescription)
                        typeName.copy(true)
                    }
                    EIDLConstructiveTypeKind.Vec -> {
                        val innerDescription = cons.description as IDLTypeDescription
                        val parameterTypeName = transpileTypeDef(innerDescription)

                        ClassName("kotlin.collections", "List").parameterizedBy(parameterTypeName)
                    }
                    EIDLConstructiveTypeKind.Variant -> {
                        val variantName = nextAnonymousTypeName()

                        val fields = cons.description as IDLFieldList
                        val variantBuilder = if (fields.value.isEmpty()) {
                            TypeSpec.objectBuilder(variantName)
                        } else {
                            val variantBuilder = TypeSpec.classBuilder(variantName).addModifiers(KModifier.SEALED)
                            for (field in fields.value) {
                                val fieldName = field.getName()
                                val fieldType = transpileTypeDef(field.type)

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
                        currentSpec.addType(variant)

                        variantName
                    }
                    EIDLConstructiveTypeKind.Record -> {
                        val recordName = nextAnonymousTypeName()
                        val fields = cons.description as IDLFieldList
                        val recordBuilder = if (fields.value.isEmpty()) {
                            TypeSpec.objectBuilder(recordName)
                        } else {
                            val dataClassBuilder = TypeSpec.classBuilder(recordName).addModifiers(KModifier.DATA)
                            val constructorBuilder = FunSpec.constructorBuilder()
                            for (field in fields.value) {
                                val fieldName = field.getName()
                                val fieldTypeName = transpileTypeDef(field.type)

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
                        currentSpec.addType(record)

                        recordName
                    }
                }
            }
        }
    }

    fun transpileActorTypeDef(description: IDLActorTypeDefinition, name: ClassName?): TypeName {
        val actorClassName = name ?: nextAnonymousTypeName()

        val actorClassBuilder = if (description.methods.isEmpty()) {
            TypeSpec.objectBuilder(actorClassName)
        } else {
            val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

            for (method in description.methods) {
                when (method.description.methodDescriptionKind) {
                    EIDLActorMethodTypeKind.Id -> {
                        val idDescription = method.description as IDLIdType
                        val idClass = ClassName(resultPackageName, idDescription.id)
                        actorClassBuilder.addProperty(method.name, idClass)
                    }
                    EIDLActorMethodTypeKind.Signature -> {
                        val funcDescription = method.description as IDLFunctionDefinition
                        val funcName = transpileFuncTypeDef(funcDescription)
                        actorClassBuilder.addProperty(method.name, funcName)
                    }
                }
            }

            actorClassBuilder
        }

        val actorClass = actorClassBuilder.build()
        currentSpec.addType(actorClass)

        return actorClassName
    }

    fun transpileFuncTypeDef(description: IDLFunctionDefinition): TypeName {
        val funcTypeName = nextAnonymousFuncTypeName()

        val arguments = description.arguments.map { arg ->
            val argName = arg.name ?: "arg${arg.position}"
            val argTypeName = transpileTypeDef(arg.type)

            ParameterSpec.builder(argName, argTypeName).build()
        }

        val resultName = when {
            description.returnValues.size == 1 -> {
                val result = description.returnValues.first()

                transpileTypeDef(result.type)
            }
            description.returnValues.isNotEmpty() -> {
                val resultClassName = createResultValueTypeName(funcTypeName.simpleName)
                val resultClassBuilder = TypeSpec.classBuilder(resultClassName).addModifiers(KModifier.DATA)

                val constructorBuilder = FunSpec.constructorBuilder()
                for (res in description.returnValues) {
                    val resName = res.name ?: "${res.position}"
                    val resTypeName = transpileTypeDef(res.type)

                    constructorBuilder.addParameter(resName, resTypeName)
                    val propertyBuilder = PropertySpec
                        .builder(resName, resTypeName)
                        .initializer(resName)
                        .build()
                    resultClassBuilder.addProperty(propertyBuilder)
                }

                resultClassBuilder.primaryConstructor(constructorBuilder.build())
                val resultDataClass = resultClassBuilder.build()
                currentSpec.addType(resultDataClass)

                resultClassName
            }
            else -> Unit::class.asClassName()
        }

        return LambdaTypeName
            .get(parameters = arguments, returnType = resultName)
            .copy(suspending = true)
    }

    fun transpilePrimitiveTypeDef(def: IDLPrimitiveType): KClass<*> {
        return when (def.type) {
            EIDLPrimitiveTypeKind.Nat -> BigInteger::class
            EIDLPrimitiveTypeKind.Nat8 -> UByte::class
            EIDLPrimitiveTypeKind.Nat16 -> UShort::class
            EIDLPrimitiveTypeKind.Nat32 -> UInt::class
            EIDLPrimitiveTypeKind.Nat64 -> ULong::class

            EIDLPrimitiveTypeKind.Int -> BigInteger::class
            EIDLPrimitiveTypeKind.Int8 -> Byte::class
            EIDLPrimitiveTypeKind.Int16 -> Short::class
            EIDLPrimitiveTypeKind.Int32 -> Int::class
            EIDLPrimitiveTypeKind.Int64 -> Long::class

            EIDLPrimitiveTypeKind.Float32 -> Float::class
            EIDLPrimitiveTypeKind.Float64 -> Double::class

            EIDLPrimitiveTypeKind.Bool -> Boolean::class
            EIDLPrimitiveTypeKind.Text -> String::class
            EIDLPrimitiveTypeKind.Null -> IDLNull::class
            EIDLPrimitiveTypeKind.Reserved -> IDLReserved::class
            EIDLPrimitiveTypeKind.Empty -> IDLEmpty::class
        }
    }
}

object IDLNull
object IDLReserved
object IDLEmpty
data class IDLPrincipal(val value: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IDLPrincipal

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}*/
