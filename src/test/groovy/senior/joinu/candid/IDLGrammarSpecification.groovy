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

    @Unroll def 'positive single service #methodName'() {
        given: 'a test fixture'
        IDLProgram program = createProgram([createMethod(methodName, arguments, results)])

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        noExceptionThrown()
        result == program

        and: 'printed to standard output for no good reason'
        transpileProgram(program)

        where: 'the service signature is as defined here'
        methodName   | arguments                                                            | results
        'initialize' | []                                                                   | []
        'ping'       | []                                                                   | []
        'size'       | []                                                                   | [IDLType.Primitive.Natural.INSTANCE]
        'whoami'     | []                                                                   | [IDLType.Primitive.Natural.INSTANCE]
        'greet'      | [IDLType.Primitive.Text.INSTANCE]                                    | [IDLType.Primitive.Text.INSTANCE]
        'configure'  | [IDLType.Primitive.Text.INSTANCE]                                    | []
        'getInHex'   | [IDLType.Primitive.Text.INSTANCE]                                    | [new IDLType.Constructive.Opt(IDLType.Primitive.Nat8.INSTANCE)]
        'putInHex'   | [IDLType.Primitive.Text.INSTANCE, IDLType.Primitive.Text.INSTANCE]   | [IDLType.Primitive.Bool.INSTANCE]
        'get'        | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE)]      | [new IDLType.Constructive.Opt(new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))]
        'put'        | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE)] | [IDLType.Primitive.Bool.INSTANCE]
    }

    @Unroll def 'positive single service with parameters #methodName'() {
        given: 'a test fixture'
        List<IDLDef.Type> types = [
            new IDLDef.Type('Key', new IDLType.Constructive.Record([new IDLFieldType('preimage', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), UtilsKt.idlHash('preimage')), new IDLFieldType('image', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), UtilsKt.idlHash('image'))])),
            new IDLDef.Type('List', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([new IDLFieldType(null, new IDLType.Id('Key'), 0), new IDLFieldType(null, new IDLType.Id('List'), 1)]))),
            new IDLDef.Type('Bucket', new IDLType.Id('List')),
            new IDLDef.Type('List_2', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([new IDLFieldType(null, IDLType.Primitive.Text.INSTANCE, 0), new IDLFieldType(null, new IDLType.Id('List_2'), 1)]))),
        ]
        IDLProgram program = createProgram([createMethod(methodName, arguments, results)], types)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpileProgram(result)

        where: 'the service signature is as defined here'
        methodName      | arguments     | results
        'peers'         | []            | [new IDLType.Id('List_2')]
        'getWithTrace'  | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Id('Bucket')] | [new IDLType.Constructive.Opt(new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))]
        'putWithTrace'  | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Id('Bucket')] | [IDLType.Primitive.Bool.INSTANCE]
    }

    def 'positive multiple services with parameters #methodName'() {
        given: 'a test fixture'
        List<IDLDef.Type> types = [
            new IDLDef.Type('Value', new IDLType.Constructive.Record([new IDLFieldType('i', IDLType.Primitive.Integer.INSTANCE, UtilsKt.idlHash('i')), new IDLFieldType('n', IDLType.Primitive.Natural.INSTANCE, UtilsKt.idlHash('n'))])),
            new IDLDef.Type('Sign', new IDLType.Constructive.Variant([new IDLFieldType('Plus', IDLType.Primitive. Null.INSTANCE, UtilsKt.idlHash('Plus')), new IDLFieldType('Minus', IDLType.Primitive. Null.INSTANCE, UtilsKt.idlHash('Minus'))])),
            new IDLDef.Type('Message', new IDLType.Constructive.Record([new IDLFieldType('sender', IDLType.Primitive.Text.INSTANCE, UtilsKt.idlHash('sender')), new IDLFieldType('message', IDLType.Primitive.Text.INSTANCE, UtilsKt.idlHash('message'))])),
            new IDLDef.Type('Chat', new IDLType.Constructive.Vec(new IDLType.Id('Message'))),
        ]
        List<IDLMethod> methods = [
            createMethod('addMessageAndReturnChat', [new IDLType.Id('Message')], [new IDLType.Id('Chat')]),
            createMethod('getValue', [new IDLType.Id('Sign')], [new IDLType.Id('Value')], [IDLFuncAnn.valueOf('Query')]),
            createMethod('returnChat', [], [new IDLType.Id('Chat')], [IDLFuncAnn.valueOf('Query')]),
        ]
        IDLProgram program = createProgram(methods, types)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpileProgram(result)
    }

    private static IDLMethod createMethod(String methodName, List<IDLType> arguments, List<IDLType> results) {
        createMethod(methodName, arguments, results, [])
    }

    private static IDLMethod createMethod(String methodName, List<IDLType> arguments, List<IDLType> results, List<IDLFuncAnn> annotations) {
        IDLMethodType methodType = new IDLType.Reference.Func(arguments.collect { new IDLArgType(null, it) }, results.collect { new IDLArgType(null, it) }, annotations)
        new IDLMethod(methodName, methodType)
    }
    private static IDLProgram createProgram(List<IDLMethod> methods) {
        createProgram(methods, [])
    }

    private static IDLProgram createProgram(List<IDLMethod> methods, List<IDLDef.Type> types) {
        List<IDLDef.Import> imports = []
        IDLType.Reference reference = new IDLType.Reference.Service(methods)
        IDLActorDef actor = new IDLActorDef(null, reference)
        IDLProgram program = new IDLProgram(imports, types, actor)
        println(">>> IDL")
        println(program)
        program
    }

    private static void transpileProgram(IDLProgram program) {
        TranspileContext context = KtTranspiler.INSTANCE.transpile(program, "tld.d.etc", "Test.kt")
        FileSpec spec = context.currentSpec.build()
        println("<<< Kotlin")
        spec.writeTo(System.out)
    }
}
