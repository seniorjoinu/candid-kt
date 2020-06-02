package senior.joinu.candid.parser

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import org.junit.jupiter.api.Test
import senior.joinu.candid.IDLGrammar
import senior.joinu.candid.transpile.KtTranspiler

class GrammarTest {
    @Test
    fun `example works fine`() {
        val testCandid = """
            import "test.did";
            type my_type = nat8;
            type List = record { head: int; tail: opt List };
            type f = func (List, func (int32) -> (int64)) -> (opt List);
            // single line comment
            type broker = service {
              find : (name: text) ->
                (service {up:() -> (); current:() -> (nat32)});
            };
            type nested = record { nat; nat; record { nat; 0x2a:nat; nat8; }; 42:nat; 40:nat; variant{ A; 0x2a; B; C }; };
            service server : {
              f : (test: blob, opt bool) -> () oneway;
              g : (my_type, List, opt List) -> (int) query;
              h : (vec opt text, variant { A: nat; B: opt text; }, opt List) -> (record { id: nat; 0x2a: record {}; });
              i : f
            }
        """.trimIndent()

        val program = IDLGrammar.parseToEnd(testCandid)
        val ktContext = KtTranspiler.transpile(program, "", "Test.kt")

        val fileSpec = ktContext.currentSpec.build()

        fileSpec.writeTo(System.out)
        println(ktContext.typeTable.labels)
        println(ktContext.typeTable.registry.mapIndexed { index, idlType -> "$index: $idlType" })
    }
}

