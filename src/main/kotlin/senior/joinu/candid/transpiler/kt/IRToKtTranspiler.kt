package senior.joinu.candid.transpiler.kt

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.*
import senior.joinu.candid.parser.*
import java.lang.RuntimeException
import java.math.BigInteger
import kotlin.reflect.KClass

interface IRTranspiler<T> {
    fun transpile(program: IDLIRProgram): T
}

object IRToKtTranspiler : IRTranspiler<FileSpec> {
    private var currentSpec = FileSpec.builder("", "Example.kt")
    private var anonymousTypeCount = 0

    private fun nextAnonymousTypeName(`package`: String = "")
            = ClassName(`package`, "AnonIDLType${anonymousTypeCount++}")

    private fun nextAnonymousFuncTypeName(`package`: String = "")
            = ClassName(`package`, "AnonFunc${anonymousTypeCount++}")

    private fun createResultValueTypeName(funcName: String, `package`: String = "")
            = ClassName(`package`, "${funcName.capitalize()}Result")

    override fun transpile(program: IDLIRProgram): FileSpec {
        program.importDefinitions.forEach { transpileImportDef(it) }
        program.typeDefinitions.forEach {
            val typeName = transpileTypeDef(it.description)

            val typeAlias = TypeAliasSpec.builder(it.name, typeName).build()
            currentSpec.addTypeAlias(typeAlias)
        }

        return currentSpec.build()
    }

    fun transpileImportDef(def: IDLImportDefinition): String {
        return ""
    }

    fun transpileTypeDef(description: IDLTypeDescription): TypeName {
        return when (description.typeDescriptionKind) {
            EIDLDescriptionTypeKind.PrimitiveType -> {
                val klass = transpilePrimitiveTypeDef(description as IDLPrimitiveType)

                klass.asTypeName()
            }
            EIDLDescriptionTypeKind.Id -> {
                val type = description as IDLIdType

                ClassName("", type.id)
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

                                val actorClassName = nextAnonymousTypeName()
                                val actorClassBuilder = TypeSpec.classBuilder(actorClassName)

                                for (method in actorDef.methods) {
                                    when (method.description.methodDescriptionKind) {
                                        EIDLActorMethodTypeKind.Id -> {
                                            val idDescription = method.description as IDLIdType
                                            val idClass = ClassName("", idDescription.id)
                                            actorClassBuilder.addProperty(method.name, idClass)
                                        }
                                        EIDLActorMethodTypeKind.Signature -> {
                                            val funcDescription = method.description as IDLFunctionDefinition
                                            val funcName = transpileFuncTypeDef(funcDescription)
                                            actorClassBuilder.addProperty(method.name, funcName)
                                        }
                                    }
                                }

                                val actorClass = actorClassBuilder.build()
                                currentSpec.addType(actorClass)

                                actorClassName
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
                        val variantBuilder = TypeSpec.enumBuilder(variantName)

                        val fields = cons.description as IDLFieldList
                        for (field in fields.value) {
                            val fieldName = field.getName()
                            variantBuilder.addEnumConstant(fieldName)
                        }

                        val variant = variantBuilder.build()
                        currentSpec.addType(variant)

                        variantName
                    }
                    EIDLConstructiveTypeKind.Record -> {
                        val dataClassName = nextAnonymousTypeName()
                        val dataClassBuilder = TypeSpec.classBuilder(dataClassName).addModifiers(KModifier.DATA)

                        val constructorBuilder = FunSpec.constructorBuilder()
                        val fields = cons.description as IDLFieldList
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
                        val dataClass = dataClassBuilder.build()
                        currentSpec.addType(dataClass)

                        dataClassName
                    }
                }
            }
        }
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
object IDLPrincipal