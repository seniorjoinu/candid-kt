package senior.joinu.candid

import com.github.h0tk3y.betterParse.grammar.GrammarKt
import com.squareup.kotlinpoet.FileSpec
import senior.joinu.candid.transpile.KtTranspiler
import senior.joinu.candid.transpile.TranspileContext
import spock.lang.Specification

/**
 * System under specification: {@link IDLGrammar}.
 * @author tglaeser
 */
class IDLGrammarSpecification extends Specification {
    def 'test'() {
        given: 'a test fixture'
        String idl = """
            service : {
              "greet": (text) -> (text);
            }
        """

        when: 'the program source is generated from the IDL'
        IDLProgram program = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, idl)

        and: 'printed to standard out for no good reason'
        println(">>> IDL $idl")
        TranspileContext context = KtTranspiler.INSTANCE.transpile(program, "tdl.domain.etc", "Test.kt")
        FileSpec spec = context.currentSpec.build()
        println("<<< Kotlin")
        spec.writeTo(System.out)

        then: 'the program matches the expectation'
        program.actor.type in IDLType.Reference.Service
        noExceptionThrown()
    }
}
