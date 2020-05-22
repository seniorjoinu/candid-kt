package senior.joinu.candid.parser

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import java.util.regex.Pattern

enum class EIDLAstNodeType {
    Nat, HexNat, Text, Id, Name, RefType, ConsType, PrimType, DataType, FieldType, ArgType, FuncAnn, FuncType, MethType,
    ActorType, Actor, Def
}

enum class EIDLAstNodeTag {
    None,
    Nat, Nat8, Nat16, Nat32, Nat64,
    Int, Int8, Int16, Int32, Int64,
    Float32, Float64,
    Bool, Text, Null, Reserved, Empty,
    Func, Service, Principal,
    Opt, Vec, Record, Variant,
    Oneway, Query,
    TypeDef, ImportDef,
    PosArgType, NameArgType,
    Blob,
    StrNameFieldType, NatNameFieldType, ShortStrNameFieldType, ShortNatNameFieldType, ShortRecordFieldType
}

class IDLAstNode(val payload: Any?, val type: EIDLAstNodeType, val tag: EIDLAstNodeTag = EIDLAstNodeTag.None)
class IDLFuncTypeNode(val arguments: List<IDLAstNode>, val results: List<IDLAstNode>, val annotations: List<IDLAstNode>)
class IDLRootNode(val defs: List<IDLAstNode>, val actor: IDLAstNode?)
class IDLKeyValueNode(val key: IDLAstNode?, val value: IDLAstNode)


object IDLGrammar : Grammar<IDLRootNode>() {
    // -----------------------------PUNCTUATION-------------------------------
    private val tSLComment by token("//.*?[\r\n]", ignore = true)
    private val tColon by token(":")
    private val tSemicolon by token(";")
    private val tComma by token(",")
    private val tAssign by token("=")
    private val tArrow by token("->")
    private val tOpBlock by token("\\{")
    private val tClBlock by token("}")
    private val tOpBracket by token("\\(")
    private val tClBracket by token("\\)")
    private val tWs by token("\\s+", ignore = true)

    // -----------------------------KEYWORDS-------------------------------
    private val tType by token("type")
    private val tImport by token("import")
    private val tOneway by token("oneway")
    private val tQuery by token("query")
    private val tVec by token("vec")
    private val tOpt by token("opt")
    private val tRecord by token("record")
    private val tVariant by token("variant")
    private val tFunc by token("func")
    private val tService by token("service")
    private val tPrincipal by token("principal")

    private val tNat8 by token("nat8")
    private val tNat16 by token("nat16")
    private val tNat32 by token("nat32")
    private val tNat64 by token("nat64")
    private val tNat by token("nat")
    private val tInt8 by token("int8")
    private val tInt16 by token("int16")
    private val tInt32 by token("int32")
    private val tInt64 by token("int64")
    private val tInt by token("int")
    private val tFloat32 by token("float32")
    private val tFloat64 by token("float64")
    private val tBool by token("bool")
    private val tText by token("text")
    private val tNull by token("null")
    private val tReserved by token("reserved")
    private val tEmpty by token("empty")
    private val tBlob by token("blob")

    // -----------------------------Nat-------------------------------
    private val tHex by token("0x[0-9a-fA-F][_0-9a-fA-F]*")
    private val tId by token("[A-Za-z_][A-Za-z0-9_]*")
    private val tDec by token("[\\-+]?[0-9][_0-9]*")
    private val tUtfScalar by token(Pattern.compile("\"[\\w._\\-\\\\/:]+\"", Pattern.UNICODE_CHARACTER_CLASS).pattern()) // TODO: check if it is a scalar for sure

    // -----------------------------ROOT-------------------------------
    private val pDefList by zeroOrMore(parser { pDef } and skip(optional(tSemicolon)))
    private val pActorOpt by optional(parser { pActor } and skip(optional(tSemicolon)))
    override val rootParser: Parser<IDLRootNode> by pDefList and pActorOpt map { (defs, actor) ->
        IDLRootNode(defs, actor)
    }

    private val pTypeDef by skip(tType) and parser { pId } and skip(tAssign) and parser { pDataType } map { (name, type) ->
        val value = IDLKeyValueNode(name, type)
        IDLAstNode(value, EIDLAstNodeType.Def, EIDLAstNodeTag.TypeDef)
    }
    private val pImportDef by skip(tImport) and parser { pTextVal } use {
        IDLAstNode(this, EIDLAstNodeType.Def, EIDLAstNodeTag.ImportDef)
    }
    private val pDef: Parser<IDLAstNode> by pTypeDef or pImportDef

    private val pActorTypeType by parser { pActorType } or parser { pId }
    private val pActor: Parser<IDLAstNode> by skip(tService) and optional(parser { pId }) and skip(tColon) and pActorTypeType map { (id, type) ->
        val value = IDLKeyValueNode(id, type)
        IDLAstNode(value, EIDLAstNodeType.Actor)
    }

    // -----------------------------COMPTYPE-------------------------------
    private val pMethTypeList by zeroOrMore(parser { pMethType } and skip(optional(tSemicolon)))
    private val pActorType: Parser<IDLAstNode> by skip(tOpBlock) and pMethTypeList and skip(tClBlock) map { methods ->
        IDLAstNode(methods, EIDLAstNodeType.ActorType)
    }

    private val pMethTypeType: Parser<IDLAstNode> by parser { pFuncType } or parser { pId }
    private val pMethType: Parser<IDLAstNode> by parser { pName } and skip(tColon) and pMethTypeType map { (name, type) ->
        val value = IDLKeyValueNode(name, type)
        IDLAstNode(value, EIDLAstNodeType.MethType)
    }

    private val pArgTypeList by zeroOrMore(parser { pArgType } and skip(optional(tComma)))
    private val pArgTypeListBlock by skip(tOpBracket) and optional(pArgTypeList) and skip(tClBracket)
    private val pFuncAnnList by zeroOrMore(parser { pFuncAnn })
    private val pFuncType: Parser<IDLAstNode> by pArgTypeListBlock and skip(tArrow) and pArgTypeListBlock and pFuncAnnList map { (args, ress, anns) ->
        val value = IDLFuncTypeNode(args ?: emptyList(), ress ?: emptyList(), anns)
        IDLAstNode(value, EIDLAstNodeType.FuncType)
    }

    private val pOneway by tOneway asJust IDLAstNode(null, EIDLAstNodeType.FuncAnn, EIDLAstNodeTag.Oneway)
    private val pQuery by tQuery asJust IDLAstNode(null, EIDLAstNodeType.FuncAnn, EIDLAstNodeTag.Query)
    private val pFuncAnn: Parser<IDLAstNode> by pOneway or pQuery

    private val pPosArgType by parser { pDataType } use { IDLAstNode(this, EIDLAstNodeType.ArgType, EIDLAstNodeTag.PosArgType) }
    private val pNameArgType by parser { pName } and skip(tColon) and parser { pDataType } map { (name, type) ->
        val value = IDLKeyValueNode(name, type)
        IDLAstNode(value, EIDLAstNodeType.ArgType, EIDLAstNodeTag.NameArgType)
    }
    private val pArgType: Parser<IDLAstNode> by pNameArgType or pPosArgType

    private val pNatNameFieldType: Parser<IDLAstNode> by parser { pNatVal } and skip(tColon) and parser { pDataType } map { (name, type) ->
        IDLAstNode(IDLKeyValueNode(name, type), EIDLAstNodeType.FieldType, EIDLAstNodeTag.NatNameFieldType)
    }
    private val pStrNameFieldType: Parser<IDLAstNode> by parser { pName } and skip(tColon) and parser { pDataType } map { (name, type) ->
        IDLAstNode(IDLKeyValueNode(name, type), EIDLAstNodeType.FieldType, EIDLAstNodeTag.StrNameFieldType)
    }
    private val pShortNatNameFieldType: Parser<IDLAstNode> by parser { pNatVal } use {
        val type = IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Null)
        IDLAstNode(IDLKeyValueNode(this, type), EIDLAstNodeType.FieldType, EIDLAstNodeTag.ShortNatNameFieldType)
    }
    private val pShortStrNameFieldType: Parser<IDLAstNode> by parser { pName } use {
        val type = IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Null)
        IDLAstNode(IDLKeyValueNode(this, type), EIDLAstNodeType.FieldType, EIDLAstNodeTag.ShortStrNameFieldType)
    }
    private val pShortRecordFieldType: Parser<IDLAstNode> by parser { pDataType } use {
        val key = IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Nat)
        val value = IDLKeyValueNode(key, this)
        IDLAstNode(value, EIDLAstNodeType.FieldType, EIDLAstNodeTag.ShortRecordFieldType)
    }
    private val pFieldType: Parser<IDLAstNode> by pNatNameFieldType or pStrNameFieldType or pShortNatNameFieldType or pShortStrNameFieldType or pShortRecordFieldType

    private val pDataType: Parser<IDLAstNode> by parser { pId } or parser { pPrimType } or parser { pConsType } or parser { pRefType } use {
        IDLAstNode(this, EIDLAstNodeType.DataType)
    }

    // -----------------------------PRIMTYPE-------------------------------
    private val pPrimType: Parser<IDLAstNode> by (
            parser { pNat } or parser { pNat8 } or parser { pNat16 } or parser { pNat32 } or parser { pNat64 }
                    or parser { pInt } or parser { pInt8 } or parser { pInt16 } or parser { pInt32 } or parser { pInt64 }
                    or parser { pFloat32 } or parser { pFloat64 } or parser { pBool } or parser { pText }
                    or parser { pNull } or parser { pReserved } or parser { pEmpty }
            )

    private val pNat by tNat asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Nat)
    private val pNat8 by tNat8 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Nat8)
    private val pNat16 by tNat16 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Nat16)
    private val pNat32 by tNat32 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Nat32)
    private val pNat64 by tNat64 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Nat64)

    private val pInt by tInt asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Int)
    private val pInt8 by tInt8 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Int8)
    private val pInt16 by tInt16 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Int16)
    private val pInt32 by tInt32 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Int32)
    private val pInt64 by tInt64 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Int64)

    private val pFloat32 by tFloat32 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Float32)
    private val pFloat64 by tFloat64 asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Float64)

    private val pBool by tBool asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Bool)
    private val pText by tText asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Text)
    private val pNull by tNull asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Null)
    private val pReserved by tReserved asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Reserved)
    private val pEmpty by tEmpty asJust IDLAstNode(null, EIDLAstNodeType.PrimType, EIDLAstNodeTag.Empty)

    // -----------------------------CONSTYPE-------------------------------
    private val pConsType: Parser<IDLAstNode> by parser { pOpt } or parser { pVec } or parser { pRecord } or parser { pVariant } or parser { pBlob }

    private val pBlob: Parser<IDLAstNode> by tBlob asJust IDLAstNode(
        null,
        EIDLAstNodeType.ConsType,
        EIDLAstNodeTag.Blob
    )

    private val pOpt: Parser<IDLAstNode> by skip(tOpt) and pDataType use {
        IDLAstNode(this, EIDLAstNodeType.ConsType, EIDLAstNodeTag.Opt)
    }

    private val pVec: Parser<IDLAstNode> by skip(tVec) and pDataType use {
        IDLAstNode(this, EIDLAstNodeType.ConsType, EIDLAstNodeTag.Vec)
    }

    private val pFieldTypeList by zeroOrMore(pFieldType and skip(optional(tSemicolon)))
    private val pFieldTypeListBlock by skip(tOpBlock) and pFieldTypeList and skip(tClBlock)
    private val pRecord: Parser<IDLAstNode> by skip(tRecord) and pFieldTypeListBlock map { fields ->
        IDLAstNode(fields, EIDLAstNodeType.ConsType, EIDLAstNodeTag.Record)
    }

    private val pVariant: Parser<IDLAstNode> by skip(tVariant) and pFieldTypeListBlock map { fields ->
        IDLAstNode(fields, EIDLAstNodeType.ConsType, EIDLAstNodeTag.Variant)
    }

    // -----------------------------REFTYPE-------------------------------
    private val pRefType: Parser<IDLAstNode> by parser { pFunc } or parser { pService } or parser { pPrincipal }

    private val pFunc: Parser<IDLAstNode> by skip(tFunc) and pFuncType use {
        IDLAstNode(this, EIDLAstNodeType.RefType, EIDLAstNodeTag.Func)
    }

    private val pService: Parser<IDLAstNode> by skip(tService) and pActorType use {
        IDLAstNode(this, EIDLAstNodeType.RefType, EIDLAstNodeTag.Service)
    }

    private val pPrincipal: Parser<IDLAstNode> by tPrincipal asJust IDLAstNode(
        null,
        EIDLAstNodeType.RefType,
        EIDLAstNodeTag.Principal
    )

    // -----------------------------OTHER-------------------------------
    private val pName: Parser<IDLAstNode> by parser { pId } or parser { pTextVal } use {
        IDLAstNode(payload, EIDLAstNodeType.Name)
    }

    private val pId: Parser<IDLAstNode> by tId use {
        IDLAstNode(text, EIDLAstNodeType.Id)
    }

    private val pTextVal: Parser<IDLAstNode> by tUtfScalar use {
        IDLAstNode(text, EIDLAstNodeType.Text)
    }

    private val pNatDec by tDec use {
        val value = text.filterNot { it == '_' }.toBigInteger()
        IDLAstNode(value, EIDLAstNodeType.Nat)
    }
    private val pNatHex by tHex use {
        IDLAstNode(text, EIDLAstNodeType.HexNat)
    }
    private val pNatVal: Parser<IDLAstNode> by pNatDec or pNatHex
}
