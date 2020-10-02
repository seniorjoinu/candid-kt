package senior.joinu.candid.idl

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

/**
 * System under specification: {@link IDLGrammar}.
 */
class IDLGrammarSpec : FreeSpec({
    "positive multiple service with multiple nested parameters" - {
        val serviceName = "server"
        val imports = listOf(
            IDLDef.Import(IDLToken.TextVal("test.did"))
        )
        val types = listOf(
            IDLDef.Type("my_type", IDLType.Primitive.Nat8),
            IDLDef.Type(
                "List",
                IDLType.Constructive.Record(
                    listOf(
                        fieldType("head", IDLType.Primitive.Integer),
                        fieldType("tail", IDLType.Constructive.Opt(IDLType.Id("List")))
                    )
                )
            ),
            IDLDef.Type(
                "f",
                function(
                    listOf<IDLType>(
                        IDLType.Id("List"),
                        function(listOf(IDLType.Primitive.Int32.asArg()), listOf(IDLType.Primitive.Int64.asArg()))
                    ).map { it.asArg() }, listOf(IDLType.Constructive.Opt(IDLType.Id("List")).asArg())
                )
            ),
            IDLDef.Type(
                "broker",
                service(
                    listOf(
                        method(
                            IDLType.Id("find"),
                            listOf(IDLArgType("name", IDLType.Primitive.Text)),
                            listOf(
                                service(
                                    listOf(
                                        method(
                                            IDLType.Id("current"),
                                            emptyList(),
                                            listOf(IDLType.Primitive.Nat32.asArg())
                                        ), method(IDLType.Id("up"), emptyList(), emptyList())
                                    )
                                ).asArg()
                            )
                        )
                    )
                )
            ),
            IDLDef.Type(
                "nested",
                IDLType.Constructive.Record(
                    listOf(
                        fieldType(0, IDLType.Primitive.Natural),
                        fieldType(1, IDLType.Primitive.Natural),
                        fieldType(
                            2,
                            IDLType.Constructive.Record(
                                listOf(
                                    fieldType(0, IDLType.Primitive.Natural),
                                    fieldType(1, IDLType.Primitive.Nat8),
                                    fieldType("0x2a", IDLType.Primitive.Natural)
                                )
                            )
                        ),
                        fieldType(
                            3,
                            IDLType.Constructive.Variant(
                                listOf(
                                    fieldType("0x2a", IDLType.Primitive.Null),
                                    fieldType("A", IDLType.Primitive.Null),
                                    fieldType("B", IDLType.Primitive.Null),
                                    fieldType("C", IDLType.Primitive.Null)
                                )
                            )
                        ),
                        fieldType("40", IDLType.Primitive.Natural),
                        fieldType("42", IDLType.Primitive.Natural)
                    )
                )
            )
        )
        val methods = listOf(
            method(
                IDLType.Id("f"),
                listOf(
                    IDLArgType("test", IDLType.Constructive.Blob),
                    IDLType.Constructive.Opt(IDLType.Primitive.Bool).asArg()
                ),
                emptyList(),
                listOf(IDLFuncAnn.Oneway)
            ),
            method(
                IDLType.Id("g"),
                listOf(
                    IDLType.Id("my_type"),
                    IDLType.Id("List"),
                    IDLType.Constructive.Opt(IDLType.Id("List"))
                ).map { it.asArg() },
                listOf(IDLType.Primitive.Integer.asArg()),
                listOf(IDLFuncAnn.Query)
            ),
            method(
                IDLType.Id("h"),
                listOf(
                    IDLType.Constructive.Vec(IDLType.Constructive.Opt(IDLType.Primitive.Text)),
                    IDLType.Constructive.Variant(
                        listOf(
                            fieldType("A", IDLType.Primitive.Natural),
                            fieldType("B", IDLType.Constructive.Opt(IDLType.Primitive.Text))
                        )
                    ),
                    IDLType.Constructive.Opt(IDLType.Id("List"))
                ).map { it.asArg() },
                listOf(
                    IDLType.Constructive.Record(
                        listOf(
                            fieldType("0x2a", IDLType.Constructive.Record(emptyList())),
                            fieldType("id", IDLType.Primitive.Natural)
                        )
                    )
                ).map { it.asArg() },
                emptyList()
            ),
            IDLMethod(IDLType.Id("i"), IDLType.Id("f"))
        )
        val program = program(methods, types, imports, serviceName)
        IDLGrammar.parseToEnd(program.println().transpile().toString()) shouldBe program
    }
    "positive single service" - {
        listOf(
            row("greet", listOf(IDLType.Primitive.Text.asArg()), listOf(IDLType.Primitive.Text.asArg())),
            row("configure", listOf(IDLType.Primitive.Text.asArg()), emptyList()),
            row(
                "getInHex",
                listOf(IDLType.Primitive.Text.asArg()),
                listOf(IDLType.Constructive.Opt(IDLType.Primitive.Nat8).asArg())
            ),
            row("putInHex", listOf(IDLType.Primitive.Text, IDLType.Primitive.Text).map { it.asArg() }, emptyList()),
            row(
                "get",
                listOf(IDLType.Constructive.Vec(IDLType.Primitive.Nat8).asArg()),
                listOf(IDLType.Constructive.Opt(IDLType.Constructive.Vec(IDLType.Primitive.Nat8)).asArg())
            ),
            row(
                "put",
                listOf(
                    IDLType.Constructive.Vec(IDLType.Primitive.Nat8),
                    IDLType.Constructive.Vec(IDLType.Primitive.Nat8)
                ).map { it.asArg() },
                listOf(IDLType.Primitive.Bool.asArg())
            ),
            row("whoami", emptyList(), listOf(IDLType.Primitive.Natural.asArg())),
            row("size", emptyList(), listOf(IDLType.Primitive.Natural.asArg())),
            row("ping", emptyList(), emptyList()),
            row("initialize", emptyList(), emptyList())
        ).map { (methodName: String, arguments: List<IDLArgType>, results: List<IDLArgType>) ->
            val program = program(listOf(method(IDLType.Id(methodName), arguments, results)))
            "positive single service $methodName" {
                IDLGrammar.parseToEnd(program.println().transpile().toString()) shouldBe program
            }
        }
    }
    "!positive single service with parameters" - {
        listOf(
            row("peers", emptyList(), listOf(IDLType.Id("List_2").asArg())),
            row(
                "getWithTrace",
                listOf(IDLType.Constructive.Vec(IDLType.Primitive.Nat8), IDLType.Id("Bucket")).map { it.asArg() },
                listOf(IDLType.Constructive.Opt(IDLType.Constructive.Vec(IDLType.Primitive.Nat8)).asArg())
            ),
            row(
                "putWithTrace",
                listOf(
                    IDLType.Constructive.Vec(IDLType.Primitive.Nat8),
                    IDLType.Constructive.Vec(IDLType.Primitive.Nat8),
                    IDLType.Id("Bucket")
                ).map { it.asArg() },
                listOf(IDLType.Primitive.Bool.asArg())
            )
        ).map { (methodName: String, arguments: List<IDLArgType>, results: List<IDLArgType>) ->
            val types = listOf(
                IDLDef.Type(
                    "Key",
                    IDLType.Constructive.Record(
                        listOf(
                            fieldType(
                                "preimage",
                                IDLType.Constructive.Vec(IDLType.Primitive.Nat8)
                            ), fieldType("image", IDLType.Constructive.Vec(IDLType.Primitive.Nat8))
                        )
                    )
                ),
                IDLDef.Type(
                    "List",
                    IDLType.Constructive.Opt(
                        IDLType.Constructive.Record(
                            listOf(
                                fieldType(0, IDLType.Id("Key")),
                                fieldType(1, IDLType.Id("List"))
                            )
                        )
                    )
                ),
                IDLDef.Type("Bucket", IDLType.Id("List")),
                IDLDef.Type(
                    "List_2",
                    IDLType.Constructive.Opt(
                        IDLType.Constructive.Record(
                            listOf(
                                fieldType(0, IDLType.Primitive.Text),
                                fieldType(1, IDLType.Id("List_2"))
                            )
                        )
                    )
                )
            )
            val program = program(listOf(method(IDLType.Id(methodName), arguments, results)), types)
            "positive single service with parameters $methodName" {
                IDLGrammar.parseToEnd(program.println().transpile().toString()) shouldBe program
            }
        }
    }
    "positive single service with recursive parameters" - {
        val types = listOf(
            IDLDef.Type(
                "Version_3",
                IDLType.Constructive.Variant(listOf(fieldType("Version", IDLType.Primitive.Natural)))
            ),
            IDLDef.Type("Version_2", IDLType.Id("Version_3")),
            IDLDef.Type("Version", IDLType.Id("Version_2")),
            IDLDef.Type(
                "Mode_3",
                IDLType.Constructive.Variant(
                    listOf(
                        fieldType("Kanji", IDLType.Primitive.Null),
                        fieldType("Numeric", IDLType.Primitive.Null),
                        fieldType("EightBit", IDLType.Primitive.Null),
                        fieldType("Alphanumeric", IDLType.Primitive.Null)
                    )
                )
            ),
            IDLDef.Type("Mode_2", IDLType.Id("Mode_3")),
            IDLDef.Type("Mode", IDLType.Id("Mode_2")),
            IDLDef.Type(
                "ErrorCorrection_3",
                IDLType.Constructive.Variant(
                    listOf(
                        fieldType("H", IDLType.Primitive.Null),
                        fieldType("L", IDLType.Primitive.Null),
                        fieldType("M", IDLType.Primitive.Null),
                        fieldType("Q", IDLType.Primitive.Null)
                    )
                )
            ),
            IDLDef.Type("ErrorCorrection_2", IDLType.Id("ErrorCorrection_3")),
            IDLDef.Type("ErrorCorrection", IDLType.Id("ErrorCorrection_2"))
        )
        val methods = listOf(
            method(
                IDLToken.TextVal("encode"),
                listOf(
                    IDLType.Id("Version"),
                    IDLType.Id("ErrorCorrection"),
                    IDLType.Id("Mode"),
                    IDLType.Primitive.Text
                ).map { it.asArg() },
                listOf(IDLType.Primitive.Text.asArg())
            )
        )
        val program = program(methods, types)
        IDLGrammar.parseToEnd(program.println().transpile().toString()) shouldBe program
    }
    "positive multiple services with parameters and annotations" - {
        val types = listOf(
            IDLDef.Type(
                "Value",
                IDLType.Constructive.Record(
                    listOf(
                        fieldType("i", IDLType.Primitive.Integer),
                        fieldType("n", IDLType.Primitive.Natural)
                    )
                )
            ),
            IDLDef.Type(
                "Sign",
                IDLType.Constructive.Variant(
                    listOf(
                        fieldType("Plus", IDLType.Primitive.Null),
                        fieldType("Minus", IDLType.Primitive.Null)
                    )
                )
            ),
            IDLDef.Type(
                "Message",
                IDLType.Constructive.Record(
                    listOf(
                        fieldType("sender", IDLType.Primitive.Text),
                        fieldType("message", IDLType.Primitive.Text)
                    )
                )
            ),
            IDLDef.Type("Chat", IDLType.Constructive.Vec(IDLType.Id("Message")))
        )
        val methods = listOf(
            method(
                IDLToken.TextVal("addMessageAndReturnChat"),
                listOf(IDLType.Id("Message").asArg()),
                listOf(IDLType.Id("Chat").asArg())
            ),
            method(
                IDLToken.TextVal("getValue"),
                listOf(IDLType.Id("Sign").asArg()),
                listOf(IDLType.Id("Value").asArg()),
                listOf(IDLFuncAnn.Query)
            ),
            method(
                IDLToken.TextVal("returnChat"),
                emptyList(),
                listOf(IDLType.Id("Chat").asArg()),
                listOf(IDLFuncAnn.Query)
            )
        )
        val program = program(methods, types)
        IDLGrammar.parseToEnd(program.println().transpile().toString()) shouldBe program
    }
})
