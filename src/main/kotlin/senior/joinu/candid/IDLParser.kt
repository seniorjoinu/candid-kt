package senior.joinu.candid

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import java.math.BigInteger
import java.util.regex.Pattern


object IDLGrammar : Grammar<IDLProgram>() {
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
    private val tUtfScalar by token(
        Pattern.compile("\"[\\w._\\-\\\\/:]+\"", Pattern.UNICODE_CHARACTER_CLASS).pattern()
    ) // TODO: check if it is a scalar for sure

    // -----------------------------ROOT-------------------------------
    private val pDefList by zeroOrMore(parser { pDef } and skip(
        optional(
            tSemicolon
        )
    ))
    private val pActorOpt by optional(parser { pActor } and skip(
        optional(
            tSemicolon
        )
    ))
    override val rootParser: Parser<IDLProgram> by pDefList and pActorOpt map { (defs, actor) ->
        val importDefs = defs.filterIsInstance<IDLDef.Import>()
        val typeDefs = defs.filterIsInstance<IDLDef.Type>()

        IDLProgram(importDefs, typeDefs, actor)
    }

    private val pTypeDef by skip(tType) and parser { pId } and skip(
        tAssign
    ) and parser { pDataType } map { (name, type) ->
        IDLDef.Type(name.value, type)
    }
    private val pImportDef by skip(tImport) and parser { pTextVal } use {
        IDLDef.Import(this.value)
    }
    private val pDef: Parser<IDLDef> by pTypeDef or pImportDef

    private val pActorTypeType: Parser<IDLActorType> by parser { pActorType } or parser { pId }
    private val pActor: Parser<IDLActorDef> by skip(tService) and optional(parser { pId }) and skip(
        tColon
    ) and pActorTypeType map { (id, type) ->
        IDLActorDef(id?.value, type)
    }

    // -----------------------------COMPTYPE-------------------------------
    private val pMethTypeList by zeroOrMore(parser { pMethType } and skip(
        optional(
            tSemicolon
        )
    ))
    private val pActorType: Parser<IDLType.Reference.Service> by skip(tOpBlock) and pMethTypeList and skip(
        tClBlock
    ) map { methods ->
        IDLType.Reference.Service(methods)
    }

    private val pMethTypeType: Parser<IDLMethodType> by parser { pFuncType } or parser { pId }
    private val pMethType: Parser<IDLMethod> by parser { pName } and skip(
        tColon
    ) and pMethTypeType map { (name, type) ->
        IDLMethod(name.value, type)
    }

    private val pArgTypeList by zeroOrMore(parser { pArgType } and skip(optional(tComma)))
    private val pArgTypeListBlock by skip(tOpBracket) and optional(
        pArgTypeList
    ) and skip(tClBracket)
    private val pFuncAnnList by zeroOrMore(parser { pFuncAnn })
    private val pFuncType: Parser<IDLType.Reference.Func> by pArgTypeListBlock and skip(
        tArrow
    ) and pArgTypeListBlock and pFuncAnnList map { (args, ress, anns) ->
        IDLType.Reference.Func(args ?: emptyList(), ress ?: emptyList(), anns)
    }

    private val pOneway by tOneway asJust IDLFuncAnn.Oneway
    private val pQuery by tQuery asJust IDLFuncAnn.Query
    private val pFuncAnn: Parser<IDLFuncAnn> by pOneway or pQuery

    private val pPosArgType by parser { pDataType } use {
        IDLArgType(null, this)
    }
    private val pNameArgType by parser { pName } and skip(
        tColon
    ) and parser { pDataType } map { (name, type) ->
        IDLArgType(name.value, type)
    }
    private val pArgType: Parser<IDLArgType> by pNameArgType or pPosArgType

    private val pNatNameFieldType: Parser<IDLFieldType> by parser { pNatVal } and skip(
        tColon
    ) and parser { pDataType } map { (name, type) ->
        when (name) {
            is IDLToken.NatVal.Dec -> IDLFieldType(name.value.toString(), type)
            is IDLToken.NatVal.Hex -> IDLFieldType(name.value, type)
        }
    }
    private val pStrNameFieldType: Parser<IDLFieldType> by parser { pName } and skip(
        tColon
    ) and parser { pDataType } map { (name, type) ->
        IDLFieldType(name.value, type)
    }
    private val pShortNatNameFieldType: Parser<IDLFieldType> by parser { pNatVal } use {
        when (this) {
            is IDLToken.NatVal.Dec -> IDLFieldType(value.toString(), IDLType.Primitive.Null)
            is IDLToken.NatVal.Hex -> IDLFieldType(value, IDLType.Primitive.Null)
        }
    }
    private val pShortStrNameFieldType: Parser<IDLFieldType> by parser { pName } use {
        IDLFieldType(value, IDLType.Primitive.Null)
    }
    private val pShortRecordFieldType: Parser<IDLFieldType> by parser { pDataType } use {
        IDLFieldType(null, this)
    }
    private val pFieldType: Parser<IDLFieldType> by pNatNameFieldType or pStrNameFieldType or pShortNatNameFieldType or pShortStrNameFieldType or pShortRecordFieldType

    private val pDataType: Parser<IDLType> by parser { pId } or parser { pPrimType } or parser { pConsType } or parser { pRefType } use {
        this
    }

    // -----------------------------PRIMTYPE-------------------------------
    private val pPrimType: Parser<IDLType.Primitive> by (
            parser { pNat } or parser { pNat8 } or parser { pNat16 } or parser { pNat32 } or parser { pNat64 }
                    or parser { pInt } or parser { pInt8 } or parser { pInt16 } or parser { pInt32 } or parser { pInt64 }
                    or parser { pFloat32 } or parser { pFloat64 } or parser { pBool } or parser { pText }
                    or parser { pNull } or parser { pReserved } or parser { pEmpty }
            )

    private val pNat by tNat asJust IDLType.Primitive.Natural
    private val pNat8 by tNat8 asJust IDLType.Primitive.Nat8
    private val pNat16 by tNat16 asJust IDLType.Primitive.Nat16
    private val pNat32 by tNat32 asJust IDLType.Primitive.Nat32
    private val pNat64 by tNat64 asJust IDLType.Primitive.Nat64

    private val pInt by tInt asJust IDLType.Primitive.Integer
    private val pInt8 by tInt8 asJust IDLType.Primitive.Int8
    private val pInt16 by tInt16 asJust IDLType.Primitive.Int16
    private val pInt32 by tInt32 asJust IDLType.Primitive.Int32
    private val pInt64 by tInt64 asJust IDLType.Primitive.Int64

    private val pFloat32 by tFloat32 asJust IDLType.Primitive.Float32
    private val pFloat64 by tFloat64 asJust IDLType.Primitive.Float64

    private val pBool by tBool asJust IDLType.Primitive.Bool
    private val pText by tText asJust IDLType.Primitive.Text
    private val pNull by tNull asJust IDLType.Primitive.Null
    private val pReserved by tReserved asJust IDLType.Primitive.Reserved
    private val pEmpty by tEmpty asJust IDLType.Primitive.Empty

    // -----------------------------CONSTYPE-------------------------------
    private val pConsType: Parser<IDLType.Constructive> by parser { pOpt } or parser { pVec } or parser { pBlob } or parser { pRecord } or parser { pVariant }
    private val pOpt by skip(tOpt) and pDataType use {
        IDLType.Constructive.Opt(this)
    }
    private val pVec by skip(tVec) and pDataType use {
        IDLType.Constructive.Vec(this)
    }
    private val pBlob by tBlob asJust IDLType.Constructive.Blob
    private val pFieldTypeList by zeroOrMore(
        pFieldType and skip(
            optional(
                tSemicolon
            )
        )
    )
    private val pFieldTypeListBlock by skip(tOpBlock) and pFieldTypeList and skip(
        tClBlock
    )
    private val pRecord: Parser<IDLType.Constructive.Record> by skip(tRecord) and pFieldTypeListBlock map { fields ->
        IDLType.Constructive.Record(fields)
    }
    private val pVariant: Parser<IDLType.Constructive.Variant> by skip(tVariant) and pFieldTypeListBlock map { fields ->
        IDLType.Constructive.Variant(fields)
    }

    // -----------------------------REFTYPE-------------------------------
    private val pRefType: Parser<IDLType.Reference> by parser { pFunc } or parser { pService } or parser { pPrincipal }
    private val pFunc: Parser<IDLType.Reference.Func> by skip(tFunc) and pFuncType use {
        this
    }
    private val pService: Parser<IDLType.Reference.Service> by skip(tService) and pActorType use {
        this
    }
    private val pPrincipal: Parser<IDLType.Reference.Principal> by tPrincipal asJust IDLType.Reference.Principal

    // -----------------------------OTHER-------------------------------
    private val pName: Parser<IDLToken.Name> by parser { pId } or parser { pTextVal } use {
        IDLToken.Name(value)
    }
    private val pId: Parser<IDLType.Id> by tId use {
        IDLType.Id(text)
    }
    private val pTextVal: Parser<IDLToken.TextVal> by tUtfScalar use {
        IDLToken.TextVal(text)
    }
    private val pNatDec by tDec use {
        val value = text.filterNot { it == '_' }.toBigInteger()
        IDLToken.NatVal.Dec(value)
    }
    private val pNatHex by tHex use {
        IDLToken.NatVal.Hex(text)
    }
    private val pNatVal: Parser<IDLToken.NatVal> by pNatDec or pNatHex
}

interface IDLTextToken {
    val value: String
}

sealed class IDLToken {
    sealed class NatVal : IDLToken() {
        class Dec(val value: BigInteger) : NatVal()
        class Hex(val value: String) : NatVal()
    }

    class TextVal(override val value: String) : IDLToken(),
        IDLTextToken

    class Name(override val value: String) : IDLToken(),
        IDLTextToken
}