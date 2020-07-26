package senior.joinu.candid

import com.github.h0tk3y.betterParse.grammar.GrammarKt
import com.squareup.kotlinpoet.FileSpec
import senior.joinu.candid.transpile.KtTranspiler
import senior.joinu.candid.transpile.TranspileContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * System under specification: {@link IDLGrammar}.
 * @author tglaeser
 */
class IDLGrammarSpecification extends Specification {
    @Unroll def 'test service method #methodName'() {
        given: 'a test fixture'
        String idl = "service : { $methodName: (${getVarargs(arguments)}) -> (${getVarargs(results)}); }"
        println(">>> IDL"); println(idl); println('')
        List<IDLFuncAnn> annotations = []
        IDLMethodType methodType = new IDLType.Reference.Func(arguments.collect { new IDLArgType(null, it) }, results.collect { new IDLArgType(null, it) }, annotations)
        IDLMethod method = new IDLMethod(methodName, methodType)
        List<IDLDef.Import> imports = []
        List<IDLDef.Type> types = []
        List<IDLMethod> methods = [method]
        IDLActorDef actor = new IDLActorDef(null, new IDLType.Reference.Service(methods))
        IDLProgram program = new IDLProgram(imports, types, actor)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, idl)

        and: 'printed to standard output for no good reason'
        TranspileContext context = KtTranspiler.INSTANCE.transpile(result, "tld.d.etc", "Test.kt")
        FileSpec spec = context.currentSpec.build()
        println("<<< Kotlin")
        spec.writeTo(System.out)

        then: 'the program matches the expectation'
        noExceptionThrown()
        result == program

        where: 'the service signature is as defined here'
        methodName | arguments                         | results
        'greet'    | [IDLType.Primitive.Text.INSTANCE] | [IDLType.Primitive.Text.INSTANCE]
    }

    static String getVarargs(List<IDLType> args) {
        return args.collect { it.toString().toLowerCase() }.join(' ,')
    }
}
