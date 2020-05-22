package senior.joinu.candid.parser

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import org.junit.jupiter.api.Test
import senior.joinu.candid.transpiler.kt.IRToKtTranspiler

class GrammarTest {
    @Test
    fun `example works fine`() {
        val testCandid = """
            import "test.did";
            type my_type = nat8;
            type List = record { head: int; tail: List };
            type f = func (List, func (int32) -> (int64)) -> (opt List);
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

        val ast = IDLGrammar.parseToEnd(testCandid)
        val ir = AstToIRCompiler.compile(ast)

        println(ir)

        val kt = IRToKtTranspiler.transpile(ir)
        kt.writeTo(System.out)
    }
}

