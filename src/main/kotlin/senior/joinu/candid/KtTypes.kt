package senior.joinu.candid

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import kotlinx.coroutines.delay
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

open class SimpleIDLService(
    var host: String?,
    val canisterId: SimpleIDLPrincipal?,
    var keyPair: EdDSAKeyPair?,
    var apiVersion: String = "v1",
    var pollingInterval: Long = 100
) {
    suspend fun call(funcName: String, arg: ByteArray): ByteArray {
        val req = ICCommonRequest(
            IDLFuncRequestType.Call,
            canisterId?.id!!,
            funcName,
            arg,
            SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte)
        )
        val requestId = submit(req)

        println("0x" + requestId.toHex())

        var status: ICStatusResponse
        loop@while (true) {
            status = requestStatus(requestId)
            when (status) {
                is ICStatusResponse.Replied -> break@loop
                is ICStatusResponse.Rejected -> throw RuntimeException("Request was rejected. code: ${status.rejectCode}, message: ${status.rejectMessage}")
                else -> {}
            }

            delay(pollingInterval)
        }

        return (status as ICStatusResponse.Replied).reply.arg
    }

    suspend fun query(funcName: String, arg: ByteArray): ByteArray {
        val req = ICCommonRequest(
            IDLFuncRequestType.Query,
            canisterId!!.id!!,
            funcName,
            arg,
            SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte)
        )
        val responseBody = read(req)
        val status = ICStatusResponse.fromCBORBytes(responseBody)

        return when (status) {
            is ICStatusResponse.Replied -> status.reply.arg
            is ICStatusResponse.Rejected -> throw RuntimeException("Request was rejected. code: ${status.rejectCode}, message: ${status.rejectMessage}")
            else -> throw RuntimeException("Unknown response for query")
        }
    }

    suspend fun requestStatus(requestId: ByteArray): ICStatusResponse {
        val req = ICStatusRequest(requestId)
        val responseBody = read(req)

        return ICStatusResponse.fromCBORBytes(responseBody)
    }

    suspend fun submit(req: ICRequest): ByteArray {
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        val (_, error) = Fuel.post("${host!!}/api/$apiVersion/submit")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArrayResult()

        if (error != null) {
            val errorMessage = error.response.body().toByteArray().toString(StandardCharsets.ISO_8859_1)
            throw RuntimeException(errorMessage, error)
        }

        return req.id
    }

    suspend fun read(req: ICRequest): ByteArray {
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        val (responseBody, error) = Fuel.post("${host!!}/api/$apiVersion/read")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArrayResult()

        if (error != null) {
            val errorMessage = error.response.body().toByteArray().toString(StandardCharsets.ISO_8859_1)
            throw RuntimeException(errorMessage, error)
        }

        return responseBody!!
    }
}

enum class IDLFuncRequestType(val value: String) {
    Call("call"),
    Query("query"),
    RequestStatus("request_status")
}

open class SimpleIDLFunc(
    val funcName: String?,
    val service: SimpleIDLService?
)

open class SimpleIDLPrincipal(
    val id: ByteArray?
) {
    companion object {
        fun selfAuthenticating(pubKey: ByteArray): SimpleIDLPrincipal {
            val pubKeyHash = hash(pubKey, HashAlgorithm.SHA224)
            val buf = ByteBuffer.allocate(pubKeyHash.size + 1)

            buf.put(pubKeyHash)
            buf.put(2)
            val id = ByteArray(pubKeyHash.size + 1)
            buf.rewind()
            buf.get(id)

            return SimpleIDLPrincipal(id)
        }

        fun fromHex(hex: String): SimpleIDLPrincipal {
            return SimpleIDLPrincipal(hex.hexStringToByteArray())
        }

        fun fromText(principal: String): SimpleIDLPrincipal {
            val text = principal.toLowerCase().replace("-", "")
            val bytes = Base32().decode(text)

            return SimpleIDLPrincipal(bytes.copyOfRange(4, bytes.size))
        }
    }

    fun toText(): String? {
        if (id == null)
            return null

        val capacity = 4 + id.size
        val buf = ByteBuffer.allocate(capacity)
        val crc = CRC32()
        crc.update(id)
        buf.putInt(crc.value.toInt())
        buf.put(id)

        val arr = buf.array()
        val str = Base32().encodeToString(arr)

        return str.replace("=", "").chunked(5).joinToString("-").toLowerCase()
    }

    fun toHex(): String? = id?.toHex()
}

object Null
object Reserved
object Empty

object CRC8 {
    @JvmStatic
    private val table = ubyteArrayOf(
        0x00.toUByte(), 0x07.toUByte(), 0x0e.toUByte(), 0x09.toUByte(), 0x1c.toUByte(), 0x1b.toUByte(), 0x12.toUByte(), 0x15.toUByte(), 0x38.toUByte(), 0x3f.toUByte(), 0x36.toUByte(), 0x31.toUByte(), 0x24.toUByte(), 0x23.toUByte(), 0x2a.toUByte(), 0x2d.toUByte(),
        0x70.toUByte(), 0x77.toUByte(), 0x7e.toUByte(), 0x79.toUByte(), 0x6c.toUByte(), 0x6b.toUByte(), 0x62.toUByte(), 0x65.toUByte(), 0x48.toUByte(), 0x4f.toUByte(), 0x46.toUByte(), 0x41.toUByte(), 0x54.toUByte(), 0x53.toUByte(), 0x5a.toUByte(), 0x5d.toUByte(),
        0xe0.toUByte(), 0xe7.toUByte(), 0xee.toUByte(), 0xe9.toUByte(), 0xfc.toUByte(), 0xfb.toUByte(), 0xf2.toUByte(), 0xf5.toUByte(), 0xd8.toUByte(), 0xdf.toUByte(), 0xd6.toUByte(), 0xd1.toUByte(), 0xc4.toUByte(), 0xc3.toUByte(), 0xca.toUByte(), 0xcd.toUByte(),
        0x90.toUByte(), 0x97.toUByte(), 0x9e.toUByte(), 0x99.toUByte(), 0x8c.toUByte(), 0x8b.toUByte(), 0x82.toUByte(), 0x85.toUByte(), 0xa8.toUByte(), 0xaf.toUByte(), 0xa6.toUByte(), 0xa1.toUByte(), 0xb4.toUByte(), 0xb3.toUByte(), 0xba.toUByte(), 0xbd.toUByte(),
        0xc7.toUByte(), 0xc0.toUByte(), 0xc9.toUByte(), 0xce.toUByte(), 0xdb.toUByte(), 0xdc.toUByte(), 0xd5.toUByte(), 0xd2.toUByte(), 0xff.toUByte(), 0xf8.toUByte(), 0xf1.toUByte(), 0xf6.toUByte(), 0xe3.toUByte(), 0xe4.toUByte(), 0xed.toUByte(), 0xea.toUByte(),
        0xb7.toUByte(), 0xb0.toUByte(), 0xb9.toUByte(), 0xbe.toUByte(), 0xab.toUByte(), 0xac.toUByte(), 0xa5.toUByte(), 0xa2.toUByte(), 0x8f.toUByte(), 0x88.toUByte(), 0x81.toUByte(), 0x86.toUByte(), 0x93.toUByte(), 0x94.toUByte(), 0x9d.toUByte(), 0x9a.toUByte(),
        0x27.toUByte(), 0x20.toUByte(), 0x29.toUByte(), 0x2e.toUByte(), 0x3b.toUByte(), 0x3c.toUByte(), 0x35.toUByte(), 0x32.toUByte(), 0x1f.toUByte(), 0x18.toUByte(), 0x11.toUByte(), 0x16.toUByte(), 0x03.toUByte(), 0x04.toUByte(), 0x0d.toUByte(), 0x0a.toUByte(),
        0x57.toUByte(), 0x50.toUByte(), 0x59.toUByte(), 0x5e.toUByte(), 0x4b.toUByte(), 0x4c.toUByte(), 0x45.toUByte(), 0x42.toUByte(), 0x6f.toUByte(), 0x68.toUByte(), 0x61.toUByte(), 0x66.toUByte(), 0x73.toUByte(), 0x74.toUByte(), 0x7d.toUByte(), 0x7a.toUByte(),
        0x89.toUByte(), 0x8e.toUByte(), 0x87.toUByte(), 0x80.toUByte(), 0x95.toUByte(), 0x92.toUByte(), 0x9b.toUByte(), 0x9c.toUByte(), 0xb1.toUByte(), 0xb6.toUByte(), 0xbf.toUByte(), 0xb8.toUByte(), 0xad.toUByte(), 0xaa.toUByte(), 0xa3.toUByte(), 0xa4.toUByte(),
        0xf9.toUByte(), 0xfe.toUByte(), 0xf7.toUByte(), 0xf0.toUByte(), 0xe5.toUByte(), 0xe2.toUByte(), 0xeb.toUByte(), 0xec.toUByte(), 0xc1.toUByte(), 0xc6.toUByte(), 0xcf.toUByte(), 0xc8.toUByte(), 0xdd.toUByte(), 0xda.toUByte(), 0xd3.toUByte(), 0xd4.toUByte(),
        0x69.toUByte(), 0x6e.toUByte(), 0x67.toUByte(), 0x60.toUByte(), 0x75.toUByte(), 0x72.toUByte(), 0x7b.toUByte(), 0x7c.toUByte(), 0x51.toUByte(), 0x56.toUByte(), 0x5f.toUByte(), 0x58.toUByte(), 0x4d.toUByte(), 0x4a.toUByte(), 0x43.toUByte(), 0x44.toUByte(),
        0x19.toUByte(), 0x1e.toUByte(), 0x17.toUByte(), 0x10.toUByte(), 0x05.toUByte(), 0x02.toUByte(), 0x0b.toUByte(), 0x0c.toUByte(), 0x21.toUByte(), 0x26.toUByte(), 0x2f.toUByte(), 0x28.toUByte(), 0x3d.toUByte(), 0x3a.toUByte(), 0x33.toUByte(), 0x34.toUByte(),
        0x4e.toUByte(), 0x49.toUByte(), 0x40.toUByte(), 0x47.toUByte(), 0x52.toUByte(), 0x55.toUByte(), 0x5c.toUByte(), 0x5b.toUByte(), 0x76.toUByte(), 0x71.toUByte(), 0x78.toUByte(), 0x7f.toUByte(), 0x6a.toUByte(), 0x6d.toUByte(), 0x64.toUByte(), 0x63.toUByte(),
        0x3e.toUByte(), 0x39.toUByte(), 0x30.toUByte(), 0x37.toUByte(), 0x22.toUByte(), 0x25.toUByte(), 0x2c.toUByte(), 0x2b.toUByte(), 0x06.toUByte(), 0x01.toUByte(), 0x08.toUByte(), 0x0f.toUByte(), 0x1a.toUByte(), 0x1d.toUByte(), 0x14.toUByte(), 0x13.toUByte(),
        0xae.toUByte(), 0xa9.toUByte(), 0xa0.toUByte(), 0xa7.toUByte(), 0xb2.toUByte(), 0xb5.toUByte(), 0xbc.toUByte(), 0xbb.toUByte(), 0x96.toUByte(), 0x91.toUByte(), 0x98.toUByte(), 0x9f.toUByte(), 0x8a.toUByte(), 0x8d.toUByte(), 0x84.toUByte(), 0x83.toUByte(),
        0xde.toUByte(), 0xd9.toUByte(), 0xd0.toUByte(), 0xd7.toUByte(), 0xc2.toUByte(), 0xc5.toUByte(), 0xcc.toUByte(), 0xcb.toUByte(), 0xe6.toUByte(), 0xe1.toUByte(), 0xe8.toUByte(), 0xef.toUByte(), 0xfa.toUByte(), 0xfd.toUByte(), 0xf4.toUByte(), 0xf3.toUByte()
    )

    fun calc(data: ByteArray): UByte {
        var result = 0.toUByte()
        for (byte in data) {
            result = table[(result xor byte.toUByte()).toInt()]
        }

        return result
    }
}
