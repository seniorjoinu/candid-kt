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
            new IDLDef.Type('Key', new IDLType.Constructive.Record([createFieldType('preimage', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE)), createFieldType('image', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))])),
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

    def 'positive single service with recursive parameters'() {
        given: 'a test fixture'
        List<IDLDef.Type> types = [
            new IDLDef.Type('Version_3', new IDLType.Constructive.Variant([createFieldType('Version', IDLType.Primitive.Natural.INSTANCE)])),
            new IDLDef.Type('Version_2', new IDLType.Id('Version_3')),
            new IDLDef.Type('Version', new IDLType.Id('Version_2')),
            new IDLDef.Type('Mode_3', new IDLType.Constructive.Variant([createFieldType('Kanji', IDLType.Primitive.Null.INSTANCE), createFieldType('Numeric', IDLType.Primitive.Null.INSTANCE), createFieldType('EightBit', IDLType.Primitive.Null.INSTANCE), createFieldType('Alphanumeric', IDLType.Primitive.Null.INSTANCE)])),
            new IDLDef.Type('Mode_2', new IDLType.Id('Mode_3')),
            new IDLDef.Type('Mode', new IDLType.Id('Mode_2')),
            new IDLDef.Type('ErrorCorrection_3', new IDLType.Constructive.Variant([createFieldType('H', IDLType.Primitive.Null.INSTANCE), createFieldType('L', IDLType.Primitive.Null.INSTANCE), createFieldType('M', IDLType.Primitive.Null.INSTANCE), createFieldType('Q', IDLType.Primitive.Null.INSTANCE)])),
            new IDLDef.Type('ErrorCorrection_2', new IDLType.Id('ErrorCorrection_3')),
            new IDLDef.Type('ErrorCorrection', new IDLType.Id('ErrorCorrection_2')),
        ]
        List<IDLMethod> methods = [createMethod('encode', [new IDLType.Id('Version'), new IDLType.Id('ErrorCorrection'), new IDLType.Id('Mode'), IDLType.Primitive.Text.INSTANCE], [IDLType.Primitive.Text.INSTANCE])]
        IDLProgram program = createProgram(methods, types)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpileProgram(result)
    }

    def 'positive multiple services with parameters and annotations'() {
        given: 'a test fixture'
        List<IDLDef.Type> types = [
            new IDLDef.Type('Value', new IDLType.Constructive.Record([createFieldType('i', IDLType.Primitive.Integer.INSTANCE), createFieldType('n', IDLType.Primitive.Natural.INSTANCE)])),
            new IDLDef.Type('Sign', new IDLType.Constructive.Variant([createFieldType('Plus', IDLType.Primitive.Null.INSTANCE), createFieldType('Minus', IDLType.Primitive.Null.INSTANCE)])),
            new IDLDef.Type('Message', new IDLType.Constructive.Record([createFieldType('sender', IDLType.Primitive.Text.INSTANCE), createFieldType('message', IDLType.Primitive.Text.INSTANCE)])),
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

    private static IDLFieldType createFieldType(String name, IDLType type) {
        new IDLFieldType(name, type, UtilsKt.idlHash(name))
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
