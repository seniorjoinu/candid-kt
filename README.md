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
