[![Release](https://jitpack.io/v/seniorjoinu/candid-kt.svg?style=flat-square)](https://jitpack.io/#seniorjoinu/candid-kt)

### Candid-kt
Generates client code for your canisters

### Install
Use Jitpack
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
```groovy
dependencies {
    implementation 'com.github.seniorjoinu:candid-kt:Tag'
}
```

### Usage

Use code generator to generate everything you need to interact with the IC
```kotlin
CandidCodeGenerator.generateFor(
    "path to .did file, e.g. ~/project/canisters/proj/main.did",
    "path where you want to save generated .kt file, e.g. ~/kt-project/src/main/kotlin/com.org.generated/Proj.kt",
    "package of the generated .kt file, e.g. com.org.generated",
    "optional, encoding of the .did file"
)
```

For example, this candid code
```
type Message = 
 record {
   "message": text;
   "sender": text;
 };
type Chat = vec Message;
service : {
  "addMessageAndReturnChat": (Message) -> (Chat);
  "returnChat": () -> (Chat) query;
}
```
would generate this Kotlin code
```kotlin
import com.squareup.kotlinpoet.CodeBlock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.ByteArray
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import senior.joinu.candid.CanisterId
import senior.joinu.candid.EdDSAKeyPair
import senior.joinu.candid.SimpleIDLFunc
import senior.joinu.candid.SimpleIDLService
import senior.joinu.candid.serialize.FuncValueSer
import senior.joinu.candid.serialize.ServiceValueSer
import senior.joinu.candid.serialize.TextValueSer
import senior.joinu.candid.serialize.TypeDeser
import senior.joinu.candid.serialize.ValueSer
import senior.joinu.candid.serialize.VecValueSer

data class Message(
  val sender: String,
  val message: String
)

object MessageValueSer : ValueSer<Message> {
  val senderValueSer: ValueSer<String> = TextValueSer

  val messageValueSer: ValueSer<String> = TextValueSer

  override fun calcSizeBytes(value: Message): Int =
      this.senderValueSer.calcSizeBytes(value.sender) +
      this.messageValueSer.calcSizeBytes(value.message)

  override fun ser(buf: ByteBuffer, value: Message) {
    this.senderValueSer.ser(buf, value.sender)
    this.messageValueSer.ser(buf, value.message)
  }

  override fun deser(buf: ByteBuffer): Message = Message(this.senderValueSer.deser(buf),
      this.messageValueSer.deser(buf))

  override fun poetize(): String = CodeBlock.of("%T", MessageValueSer::class).toString()
}

typealias Chat = List<Message>

val ChatValueSer: ValueSer<List<Message>> = VecValueSer( MessageValueSer )

typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
  funcName: String?,
  service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
  suspend operator fun invoke(arg0: Message): Chat {
    val arg0ValueSer = MessageValueSer
    val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
    val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
    sendBuf.order(ByteOrder.LITTLE_ENDIAN)
    sendBuf.put(staticPayload)
    arg0ValueSer.ser(sendBuf, arg0)
    val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
    sendBuf.rewind()
    sendBuf.get(sendBytes)

    val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
    val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
    receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
    receiveBuf.put(receiveBytes)
    receiveBuf.rewind()
    val deserContext = TypeDeser.deserUntilM(receiveBuf)
    return ChatValueSer.deser(receiveBuf) as Chat
  }

  companion object {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAFsArWPk9wGccfrxNAJcQEA")
  }
}

typealias AnonFunc1ValueSer = FuncValueSer

class AnonFunc1(
  funcName: String?,
  service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
  suspend operator fun invoke(): Chat {
    val valueSizeBytes = 0
    val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
    sendBuf.order(ByteOrder.LITTLE_ENDIAN)
    sendBuf.put(staticPayload)
    val sendBytes = ByteArray(staticPayload.size + valueSizeBytes)
    sendBuf.rewind()
    sendBuf.get(sendBytes)

    val receiveBytes = this.service!!.query(this.funcName!!, sendBytes)
    val receiveBuf = ByteBuffer.allocate(receiveBytes.size)
    receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
    receiveBuf.put(receiveBytes)
    receiveBuf.rewind()
    val deserContext = TypeDeser.deserUntilM(receiveBuf)
    return ChatValueSer.deser(receiveBuf) as Chat
  }

  companion object {
    val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAAA")
  }
}

class MainActor(
  host: String,
  id: CanisterId?,
  keyPair: EdDSAKeyPair?,
  apiVersion: String = "v1"
) : SimpleIDLService(host, id, keyPair, apiVersion) {
  val addMessageAndReturnChat: AnonFunc0 = AnonFunc0("addMessageAndReturnChat", this)

  val returnChat: AnonFunc1 = AnonFunc1("returnChat", this)
}
```
Now you can use this code to interact with your canister. Write something like this:
```kotlin
val id = CanisterId.fromCanonical("ic:9F74DCB2E416E3710E")
val keyPair = EdDSAKeyPair.generateInsecure()

val actor = MainActor("http://localhost:8000", id, keyPair)

runBlocking {
    val message = Message("Hello, chat!", "Sasha Vtyurin")
    val chat = actor.addMessageAndReturnChat(message)
    println(chat)

    val chat1 = actor.returnChat()
    println(chat1)
}
```

### Pros
* Idiomatic Kotlin
* Asynchronous http with coroutines
* Reflectionless (almost)single-allocation (de)serialization

### Cons
* Unstable

### Type conversion rules
| IDL | Kotlin |
| --- | --- |
| type T = "existing type" | typealias T = "existing type" |
| int, nat | `BigInteger` |
| int8, nat8 | `Byte` |
| int16, nat16 | `Short` |
| int32, nat32 | `Int` |
| int64, nat64 | `Long` |
| float32 | `Float` |
| float64 | `Double` |
| bool | `Boolean` |
| text | `String` |
| null | `Null` object |
| reserved | `Reserved` object |
| empty | `Empty` object |
| opt T | `T?` |
| vec T | `List<T>` |
| type T = record { a: T1, b: T2 } | `data class T(val a: T1, val b: T2)` |
| type T = variant { A, B: T1 } | `sealed class T { data class A: T(); data class B(val value: T1): T() }` |
| type T = func (T1) -> T2 | `class T { suspend operator fun invoke(arg0: T1): T2 }` |
| type T = service { a: SomeFunc } | `class T { val a: SomeFunc }` |
| principal | `Principal` class |
Unnamed IDL types are transpiled into anonymous Kotlin types.
