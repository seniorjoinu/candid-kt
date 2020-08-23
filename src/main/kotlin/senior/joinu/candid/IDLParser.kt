package senior.joinu.candid

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import java.util.regex.Pattern


object IDLGrammar : Grammar<IDLProgram>() {
    // -----------------------------PUNCTUATION-------------------------------
    private val tSLComment by regexToken("//.*?[\r\n]", ignore = true)
    private val tColon by literalToken(":")
    private val tSemicolon by literalToken(";")
    private val tComma by literalToken(",")
    private val tAssign by literalToken("=")
    private val tArrow by literalToken("->")
    private val tOpBlock by literalToken("{")
    private val tClBlock by literalToken("}")
    private val tOpBracket by literalToken("(")
    private val tClBracket by literalToken(")")
    private val tWs by regexToken("\\s+", ignore = true)

    // -----------------------------KEYWORDS-------------------------------
    private val tType by literalToken(IDLDef.Type.text)
    private val tImport by literalToken(IDLDef.Import.text)
    private val tOneway by literalToken(IDLFuncAnn.Oneway.text)
    private val tQuery by literalToken(IDLFuncAnn.Query.text)
    private val tVec by literalToken(IDLType.Constructive.Vec.text)
    private val tOpt by literalToken(IDLType.Constructive.Opt.text)
    private val tRecord by literalToken(IDLType.Constructive.Record.text)
    private val tVariant by literalToken(IDLType.Constructive.Variant.text)
    private val tFunc by literalToken(IDLType.Reference.Func.text)
    private val tService by literalToken(IDLType.Reference.Service.text)
    private val tPrincipal by literalToken(IDLType.Reference.Principal.text)

    private val tNat8 by literalToken(IDLType.Primitive.Nat8.text)
    private val tNat16 by literalToken(IDLType.Primitive.Nat16.text)
    private val tNat32 by literalToken(IDLType.Primitive.Nat32.text)
    private val tNat64 by literalToken(IDLType.Primitive.Nat64.text)
    private val tNat by literalToken(IDLType.Primitive.Natural.text)
    private val tInt8 by literalToken(IDLType.Primitive.Int8.text)
    private val tInt16 by literalToken(IDLType.Primitive.Int16.text)
    private val tInt32 by literalToken(IDLType.Primitive.Int32.text)
    private val tInt64 by literalToken(IDLType.Primitive.Int64.text)
    private val tInt by literalToken(IDLType.Primitive.Integer.text)
    private val tFloat32 by literalToken(IDLType.Primitive.Float32.text)
    private val tFloat64 by literalToken(IDLType.Primitive.Float64.text)
    private val tBool by literalToken(IDLType.Primitive.Bool.text)
    private val tText by literalToken(IDLType.Primitive.Text.text)
    private val tNull by literalToken(IDLType.Primitive.Null.text)
    private val tReserved by literalToken(IDLType.Primitive.Reserved.text)
    private val tEmpty by literalToken(IDLType.Primitive.Empty.text)
    private val tBlob by literalToken(IDLType.Constructive.Blob.text)

    // -----------------------------Nat-------------------------------
    private val tHex by regexToken("0x[0-9a-fA-F][_0-9a-fA-F]*")
    private val tId by regexToken(IDLType.Id.pattern)
    private val tDec by regexToken("[\\-+]?[0-9][_0-9]*")
    private val tUtfScalar by regexToken(
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

    private val pActorType: Parser<IDLActorType> by parser { pActorService } or parser { pId }
    private val pActor: Parser<IDLActor> by skip(tService) and optional(parser { pId }) and skip(
        tColon
    ) and pActorType map { (id, type) ->
        IDLActor(id?.value, type)
    }

    // -----------------------------COMPTYPE-------------------------------
    private val pMethTypeList by zeroOrMore(parser { pMethType } and skip(
        optional(
            tSemicolon
        )
    ))
    private val pActorService: Parser<IDLType.Reference.Service> by skip(tOpBlock) and pMethTypeList and skip(
        tClBlock
    ) map { methods ->
        IDLType.Reference.Service(methods.sortedBy { it.name })
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
            is IDLToken.NatVal.Dec -> IDLFieldType(
                name.value.toString(),
                type,
                name.value
            )
            is IDLToken.NatVal.Hex -> IDLFieldType(name.value, type, name.value.drop(2).toInt(16))
        }
    }
    private val pStrNameFieldType: Parser<IDLFieldType> by parser { pName } and skip(tColon) and
            parser { pDataType } map { (name, type) ->
        IDLFieldType(name.value, type, idlHash(name.value))
    }
    private val pShortNatNameFieldType: Parser<IDLFieldType> by parser { pNatVal } use {
        when (this) {
            is IDLToken.NatVal.Dec -> IDLFieldType(
                value.toString(),
                IDLType.Primitive.Null,
                value
            )
            is IDLToken.NatVal.Hex -> IDLFieldType(
                value,
                IDLType.Primitive.Null,
                value.drop(2).toInt(16)
            )
        }
    }
    private val pShortRecordFieldType: Parser<IDLFieldType> by parser { pDataType } use {
        IDLFieldType(null, this, -1)
    }
    private val pShortStrNameFieldType: Parser<IDLFieldType> by parser { pName } use {
        val id = IDLType.Id(value)

        if (ids.contains(id)) {
            IDLFieldType(null, id, -1)
        } else {
            IDLFieldType(value, IDLType.Primitive.Null, idlHash(value))
        }
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
        fields.forEachIndexed { idx, field ->
            if (field.idx == -1) field.idx = idx
        }

        IDLType.Constructive.Record(fields.sortedWith(Comparator { t1, t2 -> (t1.idx.toUInt() - t2.idx.toUInt()).toInt() }))
    }
    private val pVariant: Parser<IDLType.Constructive.Variant> by skip(tVariant) and pFieldTypeListBlock map { fields ->
        fields.forEachIndexed { idx, field ->
            if (field.idx == -1) field.idx = idx
        }

        IDLType.Constructive.Variant(fields.sortedWith(Comparator { t1, t2 -> (t1.idx.toUInt() - t2.idx.toUInt()).toInt() }))
    }

    // -----------------------------REFTYPE-------------------------------
    private val pRefType: Parser<IDLType.Reference> by parser { pRefTypeFunc } or parser { pRefTypeService } or parser { pRefTypePrincipal }
    private val pRefTypeFunc: Parser<IDLType.Reference.Func> by skip(tFunc) and pFuncType use {
        this
    }
    private val pRefTypeService: Parser<IDLType.Reference.Service> by skip(tService) and pActorService use {
        this
    }
    private val pRefTypePrincipal: Parser<IDLType.Reference.Principal> by tPrincipal asJust IDLType.Reference.Principal

    // -----------------------------OTHER-------------------------------
    private val pName: Parser<IDLToken.Name> by parser { pId } or parser { pTextVal } use {
        IDLToken.Name(value)
    }
    private val ids = mutableSetOf<IDLType.Id>()
    private val pId: Parser<IDLType.Id> by tId use {
        val id = IDLType.Id(text)
        ids.add(id)
        id
    }
    private val pTextVal: Parser<IDLToken.TextVal> by tUtfScalar use {
        IDLToken.TextVal(text.substring(1, text.length - 1))
    }
    private val pNatDec by tDec use {
        val value = text.filterNot { it == '_' }.toInt()
        IDLToken.NatVal.Dec(value)
    }
    private val pNatHex by tHex use {
        IDLToken.NatVal.Hex(text.filterNot { it == '_' })
    }
    private val pNatVal: Parser<IDLToken.NatVal> by pNatDec or pNatHex
}

interface IDLTextToken {
    val value: String
}

sealed class IDLToken {
    sealed class NatVal : IDLToken() {
        class Dec(val value: Int) : NatVal()
        class Hex(val value: String) : NatVal()
    }

    class TextVal(override val value: String) : IDLToken(),
        IDLTextToken

    class Name(override val value: String) : IDLToken(),
        IDLTextToken
}
