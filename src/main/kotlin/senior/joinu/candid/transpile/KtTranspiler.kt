package senior.joinu.candid.transpile

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import senior.joinu.candid.*
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
                is IDLType.Reference.Service -> transpileType(
                    value,
                    context,
                    ClassName(context.packageName, name)
                )
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
            transpileType(
                program.actor.type as IDLType,
                context,
                ClassName(context.packageName, name)
            )
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
                is IDLType.Constructive.Record -> transpileRecord(name, type, context)
                is IDLType.Constructive.Variant -> transpileVariant(name, type, context)
            }
            is IDLType.Reference.Func -> transpileFunc(name, type, context)
            is IDLType.Reference.Service -> transpileService(name, type, context)
            is IDLType.Reference.Principal -> Principal::class.asClassName()
        }
    }
}