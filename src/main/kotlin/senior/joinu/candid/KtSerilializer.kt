package senior.joinu.candid

import senior.joinu.leb128.Leb128
import senior.joinu.leb128.Leb128BI
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object KtSerilializer {
    fun serializeIDLValue(type: IDLType, value: Any?, buf: ByteBuffer, typeTable: TypeTable) {
        when (type) {
            is IDLType.Id -> {
                val labeledType = typeTable.getTypeByLabel(type)
                serializeIDLValue(labeledType, value, buf, typeTable)
            }
            is IDLType.Primitive -> {
                when (type) {
                    is IDLType.Primitive.Natural -> Leb128BI.writeNat(buf, value as BigInteger)
                    is IDLType.Primitive.Nat8 -> buf.put((value as UByte).toByte())
                    is IDLType.Primitive.Nat16 -> buf.putShort((value as UShort).toShort())
                    is IDLType.Primitive.Nat32 -> buf.putInt((value as UInt).toInt())
                    is IDLType.Primitive.Nat64 -> buf.putLong((value as ULong).toLong())

                    is IDLType.Primitive.Integer -> Leb128BI.writeInt(buf, value as BigInteger)
                    is IDLType.Primitive.Int8 -> buf.put(value as Byte)
                    is IDLType.Primitive.Int16 -> buf.putShort(value as Short)
                    is IDLType.Primitive.Int32 -> buf.putInt(value as Int)
                    is IDLType.Primitive.Int64 -> buf.putLong(value as Long)

                    is IDLType.Primitive.Float32 -> buf.putFloat(value as Float)
                    is IDLType.Primitive.Float64 -> buf.putDouble(value as Double)

                    is IDLType.Primitive.Bool -> {
                        if (value as Boolean)
                            buf.put(1)
                        else
                            buf.put(0)
                    }
                    is IDLType.Primitive.Text -> {
                        val strBytes = (value as String).toByteArray(StandardCharsets.UTF_8)
                        Leb128.writeUnsigned(buf, strBytes.size.toUInt())
                        buf.put(strBytes)
                    }
                    is IDLType.Primitive.Null -> {
                    }
                    is IDLType.Primitive.Reserved -> {
                    }
                    is IDLType.Primitive.Empty -> throw RuntimeException("Unable to serialize Empty value!")
                }
            }
            is IDLType.Constructive -> {
                when (type) {
                    is IDLType.Constructive.Opt -> {
                        if (value != null) {
                            buf.put(1)
                            serializeIDLValue(type.type, value, buf, typeTable)
                        } else
                            buf.put(0)
                    }
                    is IDLType.Constructive.Vec -> {
                        val listValue = value as List<Any?>
                        Leb128.writeUnsigned(buf, listValue.size.toUInt())

                        for (listValueEntry in listValue) {
                            serializeIDLValue(type.type, listValueEntry, buf, typeTable)
                        }
                    }
                    is IDLType.Constructive.Blob -> {
                        val bytesValue = value as ByteArray
                        Leb128.writeUnsigned(buf, bytesValue.size.toUInt())

                        buf.put(bytesValue)
                    }
                    is IDLType.Constructive.Record -> {
                        (value as IDLSerializable).serialize(buf, typeTable)
                    }
                    is IDLType.Constructive.Variant -> {
                        (value as IDLSerializable).serialize(buf, typeTable)
                    }
                }
            }
            is IDLType.Reference -> {
                when (type) {
                    is IDLType.Reference.Service -> {
                        val serviceValue = value as IDLService
                        if (serviceValue.id != null) {
                            buf.put(1)
                            serializeIDLValue(IDLType.Constructive.Blob, serviceValue.id, buf, typeTable)
                        } else
                            buf.put(0)
                    }
                    is IDLType.Reference.Func -> {
                        val funcValue = value as IDLFunc
                        if (funcValue.funcName != null && funcValue.service != null) {
                            buf.put(1)
                            serializeIDLValue(IDLType.Reference.Service(emptyList()), funcValue.service, buf, typeTable)
                            serializeIDLValue(IDLType.Primitive.Text, funcValue.funcName, buf, typeTable)
                        } else
                            buf.put(0)
                    }
                    is IDLType.Reference.Principal -> {
                        val principalValue = value as IDLPrincipal
                        if (principalValue.id != null) {
                            buf.put(1)
                            serializeIDLValue(IDLType.Constructive.Blob, principalValue.id, buf, typeTable)
                        } else
                            buf.put(0)
                    }
                }
            }
        }
    }
}
// TODO make any generated RECORD, VARIANT, FUNCTION and SERVICE - Serializer

interface IDLSerializable {
    fun serialize(buf: ByteBuffer, typeTable: TypeTable)
}

abstract class IDLService {
    abstract val id: ByteArray?
}

abstract class IDLFunc {
    abstract val service: IDLService?
    abstract val funcName: String?
}

abstract class IDLPrincipal {
    abstract val id: ByteArray?
}