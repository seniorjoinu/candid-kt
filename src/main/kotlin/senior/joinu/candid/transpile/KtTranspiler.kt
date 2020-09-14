package senior.joinu.candid.transpile

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import senior.joinu.candid.idl.IDLProgram
import senior.joinu.candid.idl.IDLType
import senior.joinu.candid.serialize.*
import java.math.BigInteger

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
                is IDLType.Reference.Service -> transpileTypeAndValueSer(
                    value,
                    context,
                    ClassName(context.packageName, name)
                )
                else -> {
                    val (type, ser) = transpileTypeAndValueSer(value, context)

                    val serTypeAliasSpec = PropertySpec.builder(
                        context.createValueSerTypeName(name).simpleName,
                        ValueSer::class.asClassName().parameterizedBy(type)
                    ).initializer(ser)

                    val typeAliasSpec = TypeAliasSpec.builder(name, type)

                    context.currentSpec.addTypeAlias(typeAliasSpec.build())
                    context.currentSpec.addProperty(serTypeAliasSpec.build())
                }
            }
        }

        if (program.actor != null) {
            val name = program.actor.name ?: "MainActor"
            transpileTypeAndValueSer(program.actor.type as IDLType, context, ClassName(context.packageName, name))
        }

        return context
    }

    fun transpileTypeAndValueSer(
        type: IDLType,
        context: TranspileContext,
        name: ClassName? = null
    ): Pair<TypeName, CodeBlock> {
        return when (type) {
            is IDLType.Id -> Pair(
                ClassName(context.packageName, type.value),
                CodeBlock.of("%T", context.createValueSerTypeName(type.value))
            )
            is IDLType.Primitive -> when (type) {
                is IDLType.Primitive.Natural -> Pair(
                    BigInteger::class.asClassName(),
                    CodeBlock.of("%T", NatValueSer::class)
                )
                is IDLType.Primitive.Nat8 -> Pair(
                    Byte::class.asClassName(),
                    CodeBlock.of("%T", Nat8ValueSer::class)
                )
                is IDLType.Primitive.Nat16 -> Pair(
                    Short::class.asClassName(),
                    CodeBlock.of("%T", Nat16ValueSer::class)
                )
                is IDLType.Primitive.Nat32 -> Pair(
                    Int::class.asClassName(),
                    CodeBlock.of("%T", Nat32ValueSer::class)
                )
                is IDLType.Primitive.Nat64 -> Pair(
                    Long::class.asClassName(),
                    CodeBlock.of("%T", Nat64ValueSer::class)
                )

                is IDLType.Primitive.Integer -> Pair(
                    BigInteger::class.asClassName(),
                    CodeBlock.of("%T", IntValueSer::class)
                )
                is IDLType.Primitive.Int8 -> Pair(
                    Byte::class.asClassName(),
                    CodeBlock.of("%T", Int8ValueSer::class)
                )
                is IDLType.Primitive.Int16 -> Pair(
                    Short::class.asClassName(),
                    CodeBlock.of("%T", Int16ValueSer::class)
                )
                is IDLType.Primitive.Int32 -> Pair(
                    Int::class.asClassName(),
                    CodeBlock.of("%T", Int32ValueSer::class)
                )
                is IDLType.Primitive.Int64 -> Pair(
                    Long::class.asClassName(),
                    CodeBlock.of("%T", Int64ValueSer::class)
                )

                is IDLType.Primitive.Float32 -> Pair(
                    Float::class.asClassName(),
                    CodeBlock.of("%T", Float32ValueSer::class)
                )
                is IDLType.Primitive.Float64 -> Pair(
                    Double::class.asClassName(),
                    CodeBlock.of("%T", Float64ValueSer::class)
                )

                is IDLType.Primitive.Bool -> Pair(
                    Boolean::class.asClassName(),
                    CodeBlock.of("%T", BoolValueSer::class)
                )
                is IDLType.Primitive.Text -> Pair(
                    String::class.asClassName(),
                    CodeBlock.of("%T", TextValueSer::class)
                )
                is IDLType.Primitive.Null -> Pair(
                    Null::class.asClassName(),
                    CodeBlock.of("%T", NullValueSer::class)
                )
                is IDLType.Primitive.Reserved -> Pair(
                    Reserved::class.asClassName(),
                    CodeBlock.of("%T", ReservedValueSer::class)
                )
                is IDLType.Primitive.Empty -> Pair(
                    Empty::class.asClassName(),
                    CodeBlock.of("%T", EmptyValueSer::class)
                )
            }
            is IDLType.Constructive -> when (type) {
                is IDLType.Constructive.Opt -> {
                    val (innerType, innerSer) = transpileTypeAndValueSer(type.type, context)

                    Pair(
                        innerType.copy(true),
                        CodeBlock.of("%T( $innerSer )", OptValueSer::class)
                    )
                }
                is IDLType.Constructive.Vec -> {
                    if (type.type is IDLType.Primitive.Nat8) {
                        Pair(
                            ByteArray::class.asTypeName(),
                            CodeBlock.of("%T", BlobValueSer::class)
                        )
                    } else {
                        val (innerType, innerSer) = transpileTypeAndValueSer(type.type, context)

                        Pair(
                            List::class.asClassName().parameterizedBy(innerType),
                            CodeBlock.of("%T( $innerSer )", VecValueSer::class)
                        )
                    }
                }
                is IDLType.Constructive.Blob -> Pair(
                    ByteArray::class.asClassName(),
                    CodeBlock.of("%T", BlobValueSer::class)
                )
                is IDLType.Constructive.Record -> transpileRecord(name, type, context)

                is IDLType.Constructive.Variant -> transpileVariant(name, type, context)
            }
            is IDLType.Reference -> when (type) {
                is IDLType.Reference.Func -> Pair(
                    transpileFunc(name, type, context),
                    CodeBlock.of("%T", FuncValueSer::class)
                )
                is IDLType.Reference.Service -> Pair(
                    transpileService(name, type, context),
                    CodeBlock.of("%T", ServiceValueSer::class)
                )
                is IDLType.Reference.Principal -> Pair(
                    SimpleIDLPrincipal::class.asClassName(),
                    CodeBlock.of("%T", PrincipalValueSer::class)
                )
            }
            else -> throw RuntimeException("Unable to transpile - invalid input")
        }
    }
}
