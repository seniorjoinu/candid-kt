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

        val testCandid1 = """
            service : {
              "greet": (text) -> (text);
            }
        """.trimIndent()

        val testCandid2 = """
            type List_2 = 
             opt record {
                   text;
                   List_2;
                 };
            type List = 
             opt record {
                   Key;
                   List;
                 };
            type Key = 
             record {
               "image": vec nat8;
               "preimage": vec nat8;
             };
            type Bucket = List;
            service : {
              "configure": (text) -> ();
              "get": (vec nat8) -> (opt vec nat8);
              "getInHex": (text) -> (opt text);
              "getWithTrace": (vec nat8, Bucket) -> (opt vec nat8);
              "initialize": () -> ();
              "peers": () -> (List_2);
              "ping": () -> ();
              "put": (vec nat8, vec nat8) -> (bool);
              "putInHex": (text, text) -> (bool);
              "putWithTrace": (vec nat8, vec nat8, Bucket) -> (bool);
              "size": () -> (nat);
              "whoami": () -> (text);
            }
        """.trimIndent()

        val testCandid3 = """
            type Version_3 = variant {"Version": nat;};
            type Version_2 = Version_3;
            type Version = Version_2;
            type Mode_3 = 
             variant {
               "Alphanumeric": null;
               "EightBit": null;
               "Kanji": null;
               "Numeric": null;
             };
            type Mode_2 = Mode_3;
            type Mode = Mode_2;
            type ErrorCorrection_3 = 
             variant {
               "H": null;
               "L": null;
               "M": null;
               "Q": null;
             };
            type ErrorCorrection_2 = ErrorCorrection_3;
            type ErrorCorrection = ErrorCorrection_2;
            service : {
              "encode": (Version, ErrorCorrection, Mode, text) -> (text);
            }
        """.trimIndent()

        val program = IDLGrammar.parseToEnd(testCandid3)
        val ktContext = KtTranspiler.transpile(program, "", "Test.kt")

        val fileSpec = ktContext.currentSpec.build()

        fileSpec.writeTo(System.out)
    }
}

