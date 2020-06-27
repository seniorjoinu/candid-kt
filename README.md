### Work in progress
You can inspect currently generated code in the `test` dir - result of input `.did` from `GrammarTest.kt` is saved into `Test.kt`

### What's missing
* imports
* future types
* tests

### Type conversion rules
| IDL | Kotlin |
| --- | --- |
| type T = "existing type" | typealias T = "existing type" |
| int, nat | `BigInteger` with signum |
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
