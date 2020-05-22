package senior.joinu.candid.parser

import java.lang.RuntimeException
import java.math.BigInteger

enum class EIDLPrimitiveTypeKind {
    Nat, Nat8, Nat16, Nat32, Nat64,
    Int, Int8, Int16, Int32, Int64,
    Float32, Float64,
    Bool, Text, Null, Reserved, Empty
}
enum class EIDLConstructiveTypeKind {
    Blob, Opt, Vec, Record, Variant
}
enum class EIDLReferenceTypeKind {
    Entity, Principal
}
enum class EIDLFunctionAnnotationKind {
    Oneway, Query
}

data class IDLIdType(val id: String) : IDLTypeDescription, IDLActorMethodTypeDescription {
    override val typeDescriptionKind = EIDLDescriptionTypeKind.Id
    override val methodDescriptionKind = EIDLActorMethodTypeKind.Id
}
data class IDLPrimitiveType(val type: EIDLPrimitiveTypeKind) : IDLTypeDescription {
    override val typeDescriptionKind = EIDLDescriptionTypeKind.PrimitiveType
}
data class IDLConstructiveType(
    val type: EIDLConstructiveTypeKind,
    val description: IDLConstructiveTypeDescription?
) : IDLTypeDescription {
    override val typeDescriptionKind = EIDLDescriptionTypeKind.ConstructiveType
}
data class IDLReferenceType(
    val type: EIDLReferenceTypeKind,
    val description: IDLReferenceTypeDescription?
) : IDLTypeDescription {
    override val typeDescriptionKind = EIDLDescriptionTypeKind.ReferenceType
}

data class IDLFieldList(val value: List<IDLFieldDefinition>) : IDLConstructiveTypeDescription {
    override val constructiveTypeKind = EIDLConstructiveDescriptionKind.Fields
}
data class IDLFieldDefinition(val strName: String?, val natName: BigInteger?, val type: IDLTypeDescription) {
    fun getName() = strName
        ?: if (natName != null) {
            "$natName"
        } else {
            throw RuntimeException("Unable to parse variant (no name)")
        }
}
data class IDLFunctionDefinition(
    val arguments: List<IDLArgumentDefinition>,
    val returnValues: List<IDLArgumentDefinition>,
    val annotations: List<EIDLFunctionAnnotationKind>
) : IDLReferenceTypeDescription, IDLActorMethodTypeDescription {
    override val referenceDescriptionKind = EIDLReferenceKind.Function
    override val methodDescriptionKind = EIDLActorMethodTypeKind.Signature
}
data class IDLArgumentDefinition(
    val name: String?,
    val position: Int,
    val type: IDLTypeDescription
)
data class IDLActorTypeDefinition(val methods: List<IDLActorMethodDefinition>) : IDLReferenceTypeDescription {
    override val referenceDescriptionKind = EIDLReferenceKind.Actor
}

interface IDLTypeDescription : IDLConstructiveTypeDescription {
    val typeDescriptionKind: EIDLDescriptionTypeKind

    override val constructiveTypeKind: EIDLConstructiveDescriptionKind
        get() = EIDLConstructiveDescriptionKind.Subkind
}
enum class EIDLDescriptionTypeKind {
    Id, PrimitiveType, ConstructiveType, ReferenceType
}

interface IDLConstructiveTypeDescription {
    val constructiveTypeKind: EIDLConstructiveDescriptionKind
}
enum class EIDLConstructiveDescriptionKind {
    Subkind, Fields
}

interface IDLReferenceTypeDescription {
    val referenceDescriptionKind: EIDLReferenceKind
}
enum class EIDLReferenceKind {
    Function, Actor
}

interface IDLActorMethodTypeDescription {
    val methodDescriptionKind: EIDLActorMethodTypeKind
}
enum class EIDLActorMethodTypeKind {
    Id, Signature
}

interface IDLDefinition {
    val definitionKind: EIDLDefinitionKind
}
enum class EIDLDefinitionKind {
    Type, Import
}

data class IDLActorMethodDefinition(
    val name: String,
    val description: IDLActorMethodTypeDescription
)

data class IDLActorDefinition(
    val name: String?,
    val type: IDLActorTypeDefinition
)

data class IDLTypeDefinition(val name: String, val description: IDLTypeDescription) : IDLDefinition {
    override val definitionKind = EIDLDefinitionKind.Type
}

data class IDLImportDefinition(val path: String) : IDLDefinition {
    override val definitionKind = EIDLDefinitionKind.Import
}

data class IDLIRProgram(
    val importDefinitions: List<IDLImportDefinition>,
    val typeDefinitions: List<IDLTypeDefinition>,
    val actor: IDLActorDefinition?
)

object AstToIRCompiler {
    private fun astNodeTypeTagIncompatibility(got: IDLAstNode, vararg expected: EIDLAstNodeType)
            = RuntimeException("Incompatible type-tag combination: expected one of - ${expected.map { it.name }}, got - ${got.type.name}")

    fun compile(ast: IDLRootNode): IDLIRProgram {
        val definitions = ast.defs.map { compileDefinition(it) }
        val typeDefinitions = definitions.filter { it.definitionKind == EIDLDefinitionKind.Type } as List<IDLTypeDefinition>
        val importDefinitions = definitions.filter { it.definitionKind == EIDLDefinitionKind.Import } as List<IDLImportDefinition>

        val actor = if (ast.actor != null) {
            compileActor(ast.actor)
        } else {
            null
        }

        return IDLIRProgram(importDefinitions, typeDefinitions, actor)
    }

    fun compileActor(node: IDLAstNode): IDLActorDefinition {
        val innerNode = node.payload as IDLKeyValueNode
        val keyNode = innerNode.key
        val typeNode = innerNode.value

        val key = if (keyNode != null) {
            compileTextNode(keyNode)
        } else {
            null
        }

        val actorType = compileActorTypeNode(typeNode)

        return IDLActorDefinition(key, actorType)
    }

    fun compileDefinition(node: IDLAstNode): IDLDefinition {
        return when (node.tag) {
            EIDLAstNodeTag.ImportDef -> {
                val textNode = node.payload as IDLAstNode
                val text = textNode.payload as String

                IDLImportDefinition(text)
            }
            EIDLAstNodeTag.TypeDef -> {
                val typeNode = node.payload as IDLKeyValueNode
                val keyNode = typeNode.key!!
                val typeDefNode = typeNode.value

                val typeName = compileTextNode(keyNode)
                val description = compileDataTypeNode(typeDefNode)

                IDLTypeDefinition(typeName, description)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.Def)
        }
    }

    fun compileTextNode(node: IDLAstNode): String {
        val text = node.payload as String

        return text
    }

    fun compileNatNode(node: IDLAstNode): BigInteger {
        return node.payload as BigInteger
    }

    fun compilePrimTypeNode(node: IDLAstNode): IDLPrimitiveType {
        val type = when (node.tag) {
            EIDLAstNodeTag.Nat -> EIDLPrimitiveTypeKind.Nat
            EIDLAstNodeTag.Nat8 -> EIDLPrimitiveTypeKind.Nat8
            EIDLAstNodeTag.Nat16 -> EIDLPrimitiveTypeKind.Nat16
            EIDLAstNodeTag.Nat32 -> EIDLPrimitiveTypeKind.Nat32
            EIDLAstNodeTag.Nat64 -> EIDLPrimitiveTypeKind.Nat64

            EIDLAstNodeTag.Int -> EIDLPrimitiveTypeKind.Int
            EIDLAstNodeTag.Int8 -> EIDLPrimitiveTypeKind.Int8
            EIDLAstNodeTag.Int16 -> EIDLPrimitiveTypeKind.Int16
            EIDLAstNodeTag.Int32 -> EIDLPrimitiveTypeKind.Int32
            EIDLAstNodeTag.Int64 -> EIDLPrimitiveTypeKind.Int64

            EIDLAstNodeTag.Float32 -> EIDLPrimitiveTypeKind.Float32
            EIDLAstNodeTag.Float64 -> EIDLPrimitiveTypeKind.Float64

            EIDLAstNodeTag.Bool -> EIDLPrimitiveTypeKind.Bool
            EIDLAstNodeTag.Text -> EIDLPrimitiveTypeKind.Text
            EIDLAstNodeTag.Null -> EIDLPrimitiveTypeKind.Null
            EIDLAstNodeTag.Reserved -> EIDLPrimitiveTypeKind.Reserved
            EIDLAstNodeTag.Empty -> EIDLPrimitiveTypeKind.Empty
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.PrimType)
        }

        return IDLPrimitiveType(type)
    }

    fun compileConType(node: IDLAstNode): IDLConstructiveType {
        return when (node.tag) {
            EIDLAstNodeTag.Blob -> {
                IDLConstructiveType(EIDLConstructiveTypeKind.Blob, null)
            }
            EIDLAstNodeTag.Opt -> {
                val innerNode = node.payload as IDLAstNode
                IDLConstructiveType(EIDLConstructiveTypeKind.Opt, compileDataTypeNode(innerNode))
            }
            EIDLAstNodeTag.Vec -> {
                val innerNode = node.payload as IDLAstNode
                IDLConstructiveType(EIDLConstructiveTypeKind.Vec, compileDataTypeNode(innerNode))
            }
            EIDLAstNodeTag.Record -> {
                val fieldNodes = node.payload as List<IDLAstNode>
                val fieldsTypes = fieldNodes.mapIndexed { idx, it -> compileFieldTypeNode(idx, it) }
                val fields = IDLFieldList(fieldsTypes)

                IDLConstructiveType(EIDLConstructiveTypeKind.Record, fields)
            }
            EIDLAstNodeTag.Variant -> {
                val fieldNodes = node.payload as List<IDLAstNode>
                val fieldsTypes = fieldNodes.mapIndexed { idx, it -> compileFieldTypeNode(idx, it) }
                val fields = IDLFieldList(fieldsTypes)

                IDLConstructiveType(EIDLConstructiveTypeKind.Variant, fields)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.ConsType)
        }
    }

    fun compileFieldTypeNode(idx: Int, node: IDLAstNode): IDLFieldDefinition {
        val innerNode = node.payload as IDLKeyValueNode
        val keyNode = innerNode.key
        val typeNode = innerNode.value

        return when (node.tag) {
            EIDLAstNodeTag.StrNameFieldType -> {
                val name = compileTextNode(keyNode!!)
                val type = compileDataTypeNode(typeNode)

                IDLFieldDefinition(name, null, type)
            }
            EIDLAstNodeTag.ShortStrNameFieldType -> {
                val name = compileTextNode(keyNode!!)

                IDLFieldDefinition(name, null, IDLPrimitiveType(EIDLPrimitiveTypeKind.Null))
            }
            EIDLAstNodeTag.ShortNatNameFieldType -> {
                if (keyNode!!.type == EIDLAstNodeType.Nat) {
                    val name = compileNatNode(keyNode)
                    IDLFieldDefinition(null, name, IDLPrimitiveType(EIDLPrimitiveTypeKind.Null))
                } else {
                    val name = compileTextNode(keyNode)
                    IDLFieldDefinition(name, null, IDLPrimitiveType(EIDLPrimitiveTypeKind.Null))
                }
            }
            EIDLAstNodeTag.NatNameFieldType -> {
                if (keyNode!!.type == EIDLAstNodeType.Nat) {
                    val name = compileNatNode(keyNode)
                    val type = compileDataTypeNode(typeNode)
                    IDLFieldDefinition(null, name, type)
                } else {
                    val name = compileTextNode(keyNode)
                    val type = compileDataTypeNode(typeNode)
                    IDLFieldDefinition(name, null, type)
                }
            }
            EIDLAstNodeTag.ShortRecordFieldType -> {
                val type = compileDataTypeNode(typeNode)

                IDLFieldDefinition(null, idx.toBigInteger(), type)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.FieldType)
        }
    }

    fun compileArgumentNode(node: IDLAstNode, idx: Int): IDLArgumentDefinition {
        return when (node.tag) {
            EIDLAstNodeTag.PosArgType -> {
                val innerNode = node.payload as IDLAstNode
                val type = compileDataTypeNode(innerNode)

                IDLArgumentDefinition(null, idx, type)
            }
            EIDLAstNodeTag.NameArgType -> {
                val innerNode = node.payload as IDLKeyValueNode
                val keyNode = innerNode.key
                val typeNode = innerNode.value

                val name = compileTextNode(keyNode!!)
                val type = compileDataTypeNode(typeNode)

                IDLArgumentDefinition(name, idx, type)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.ArgType)
        }
    }

    fun compileFuncAnnNode(node: IDLAstNode): EIDLFunctionAnnotationKind {
        return when (node.tag) {
            EIDLAstNodeTag.Oneway -> EIDLFunctionAnnotationKind.Oneway
            EIDLAstNodeTag.Query -> EIDLFunctionAnnotationKind.Query
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.FuncAnn)
        }
    }

    fun compileMethodTypeNode(node: IDLAstNode): IDLActorMethodDefinition {
        return when (node.type) {
            EIDLAstNodeType.MethType -> {
                val innerNode = node.payload as IDLKeyValueNode
                val keyNode = innerNode.key
                val typeNode = innerNode.value

                val name = compileTextNode(keyNode!!)

                val methodDef = when (typeNode.type) {
                    EIDLAstNodeType.FuncType -> compileFuncTypeNode(typeNode)
                    EIDLAstNodeType.Id -> IDLIdType(compileTextNode(typeNode))
                    else -> throw astNodeTypeTagIncompatibility(typeNode, EIDLAstNodeType.FuncType, EIDLAstNodeType.Id)
                }

                IDLActorMethodDefinition(name, methodDef)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.MethType)
        }
    }

    fun compileActorTypeNode(node: IDLAstNode): IDLActorTypeDefinition {
        return when (node.type) {
            EIDLAstNodeType.ActorType -> {
                val methodNodes = node.payload as List<IDLAstNode>
                val methods = methodNodes.map { compileMethodTypeNode(it) }

                IDLActorTypeDefinition(methods)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.ActorType)
        }
    }

    fun compileFuncTypeNode(node: IDLAstNode): IDLFunctionDefinition {
        val funcNode = node.payload as IDLFuncTypeNode

        val arguments = funcNode.arguments.mapIndexed { idx, it -> compileArgumentNode(it, idx) }
        val results = funcNode.results.mapIndexed { idx, it -> compileArgumentNode(it, idx) }
        val annotations = funcNode.annotations.map { compileFuncAnnNode(it) }

        return IDLFunctionDefinition(arguments, results, annotations)
    }

    fun compileRefTypeNode(node: IDLAstNode): IDLReferenceType {
        return when (node.tag) {
            EIDLAstNodeTag.Func -> {
                val innerNode = node.payload as IDLAstNode
                val funcDef = compileFuncTypeNode(innerNode)
                IDLReferenceType(EIDLReferenceTypeKind.Entity, funcDef)
            }
            EIDLAstNodeTag.Service -> {
                val innerNode = node.payload as IDLAstNode
                val actorDef = compileActorTypeNode(innerNode)
                IDLReferenceType(EIDLReferenceTypeKind.Entity, actorDef)
            }
            EIDLAstNodeTag.Principal -> {
                IDLReferenceType(EIDLReferenceTypeKind.Principal, null)
            }
            else -> throw astNodeTypeTagIncompatibility(node, EIDLAstNodeType.RefType)
        }
    }

    fun compileDataTypeNode(node: IDLAstNode): IDLTypeDescription {
        val innerNode = node.payload as IDLAstNode

        return when (innerNode.type) {
            EIDLAstNodeType.Id -> {
                val text = compileTextNode(innerNode)

                IDLIdType(text)
            }
            EIDLAstNodeType.PrimType -> {
                compilePrimTypeNode(innerNode)
            }
            EIDLAstNodeType.ConsType -> {
                compileConType(innerNode)
            }
            EIDLAstNodeType.RefType -> {
                compileRefTypeNode(innerNode)
            }
            else -> throw astNodeTypeTagIncompatibility(
                innerNode,
                EIDLAstNodeType.Id,
                EIDLAstNodeType.PrimType,
                EIDLAstNodeType.ConsType,
                EIDLAstNodeType.RefType
            )
        }
    }
}