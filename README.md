<img src="https://github.com/seniorjoinu/candid-kt/blob/master/logo.png?raw=true" align="left" width="100%" >

[![Release](https://jitpack.io/v/seniorjoinu/candid-kt.svg?style=flat-square)](https://jitpack.io/#seniorjoinu/candid-kt)
<img src="https://img.shields.io/badge/dfx-0.6.7-blue?style=flat-square"/>
<img src="https://img.shields.io/badge/kotlin-1.4-blue?style=flat-square"/>

### Candid-kt
Generates client code for your canisters

### Usage

Use [the gradle plugin](https://github.com/seniorjoinu/candid-kt-gradle-plugin) to generate Kotlin code out of candid code.

For example, this candid code
```
service : {
  "greet": (text) -> (text);
}
```
would generate this Kotlin code
```kotlin
typealias MainActorValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    suspend operator fun invoke(arg0: String): String {
        val arg0ValueSer = senior.joinu.candid.serialize.TextValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        val sendBytes = sendBuf.array()

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.wrap(receiveBytes)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.TextValueSer.deser(receiveBuf) as kotlin.String
    }

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")
    }
}

class MainActor(
    host: String,
    canisterId: SimpleIDLPrincipal?,
    keyPair: EdDSAKeyPair?,
    apiVersion: String = "v1"
) : SimpleIDLService(host, canisterId, keyPair, apiVersion) {
    val greet: AnonFunc0 = AnonFunc0("greet", this)
}
```
which we then can use to interact with our deployed canister
```kotlin
val host = "http://localhost:8000"
val canisterId = SimpleIDLPrincipal.fromText("75hes-oqbaa-aaaaa-aaaaa-aaaaa-aaaaa-aaaaa-q")
val keyPair = EdDSAKeyPair.generateInsecure()

val helloWorld = MainActor(host, canisterId, keyPair)

val response = helloWorld.greet("World")
println(response) // Hello, World!    
```


### Pros
* Idiomatic Kotlin
* Complete type-safety
* Asynchronous io with coroutines
* Reflectionless single-allocation (de)serialization

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
