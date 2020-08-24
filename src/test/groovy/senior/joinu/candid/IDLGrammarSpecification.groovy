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

    def 'positive multiple service with multiple nested parameters'() {
        given: 'a test fixture'
        String serviceName = 'server'
        List<IDLDef.Import> imports = [
            new IDLDef.Import(new IDLToken.TextVal('test.did'))
        ]
        List<IDLDef.Type> types = [
            new IDLDef.Type('my_type', IDLType.Primitive.Nat8.INSTANCE),
            new IDLDef.Type('List', new IDLType.Constructive.Record([createFieldType('head', IDLType.Primitive.Integer.INSTANCE), createFieldType('tail', new IDLType.Constructive.Opt(new IDLType.Id('List')))]) ),
            new IDLDef.Type('f', createReferenceFunction([new IDLType.Id('List'), createReferenceFunction([IDLType.Primitive.Int32.INSTANCE], [IDLType.Primitive.Int64.INSTANCE], [])], [new IDLType.Constructive.Opt(new IDLType.Id('List'))], [])),
            new IDLDef.Type('broker', createReferenceService([createMethodWithNamedParameters(new IDLType.Id('find'), [new Tuple2<>('name', IDLType.Primitive.Text.INSTANCE)], [new Tuple2<>(null, createReferenceService([createMethod(new IDLType.Id('current'), [], [IDLType.Primitive.Nat32.INSTANCE]), createMethod(new IDLType.Id('up'), [], [])]))])])),
            new IDLDef.Type('nested', new IDLType.Constructive.Record([createFieldType(0, IDLType.Primitive.Natural.INSTANCE), createFieldType(1, IDLType.Primitive.Natural.INSTANCE), createFieldType(2, new IDLType.Constructive.Record([createFieldType(0, IDLType.Primitive.Natural.INSTANCE), createFieldType(1, IDLType.Primitive.Nat8.INSTANCE), createFieldType("0x2a", IDLType.Primitive.Natural.INSTANCE)])), createFieldType(3, new IDLType.Constructive.Variant([createFieldType(0, new IDLType.Id('A')), createFieldType(1, new IDLType.Id('B')), createFieldType(2, new IDLType.Id('C')), createFieldType("0x2a", IDLType.Primitive.Null.INSTANCE)])), createFieldType("40", IDLType.Primitive.Natural.INSTANCE), createFieldType("42", IDLType.Primitive.Natural.INSTANCE)])),
        ]
        List<IDLMethod> methods = [
            createMethodWithNamedParameters(new IDLType.Id('f'), [new Tuple2<>('test', IDLType.Constructive.Blob.INSTANCE), new Tuple2<>(null, new IDLType.Constructive.Opt(IDLType.Primitive.Bool.INSTANCE))], [], [IDLFuncAnn.valueOf('Oneway')]),
            createMethod(new IDLType.Id('g'), [new IDLType.Id('my_type'), new IDLType.Id('List'), new IDLType.Constructive.Opt(new IDLType.Id('List'))], [IDLType.Primitive.Integer.INSTANCE], [IDLFuncAnn.valueOf('Query')]),
            createMethod(new IDLType.Id('h'), [new IDLType.Constructive.Vec(new IDLType.Constructive.Opt(IDLType.Primitive.Text.INSTANCE)), new IDLType.Constructive.Variant([createFieldType('A', IDLType.Primitive.Natural.INSTANCE), createFieldType('B', new IDLType.Constructive.Opt(IDLType.Primitive.Text.INSTANCE))]), new IDLType.Constructive.Opt(new IDLType.Id('List'))], [new IDLType.Constructive.Record([createFieldType('0x2a', new IDLType.Constructive.Record([])), createFieldType('id', IDLType.Primitive.Natural.INSTANCE)])], []),
            new IDLMethod(new IDLType.Id('i'), new IDLType.Id('f')),
        ]
        IDLProgram program = createProgram(serviceName, methods, types, imports)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpileProgram(result)
    }

    @Unroll def 'positive single service #methodName'() {
        given: 'a test fixture'
        IDLProgram program = createProgram([createMethod(new IDLToken.TextVal(methodName), arguments, results)])

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
            new IDLDef.Type('List', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([createFieldType(0, new IDLType.Id('Key')), createFieldType(1, new IDLType.Id('List'))]))),
            new IDLDef.Type('Bucket', new IDLType.Id('List')),
            new IDLDef.Type('List_2', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([createFieldType(0, IDLType.Primitive.Text.INSTANCE), createFieldType(1, new IDLType.Id('List_2'))]))),
        ]
        IDLProgram program = createProgram([createMethod(new IDLToken.TextVal(methodName), arguments, results)], types)

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
        List<IDLMethod> methods = [createMethod(new IDLToken.TextVal('encode'), [new IDLType.Id('Version'), new IDLType.Id('ErrorCorrection'), new IDLType.Id('Mode'), IDLType.Primitive.Text.INSTANCE], [IDLType.Primitive.Text.INSTANCE])]
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
            createMethod(new IDLToken.TextVal('addMessageAndReturnChat'), [new IDLType.Id('Message')], [new IDLType.Id('Chat')]),
            createMethod(new IDLToken.TextVal('getValue'), [new IDLType.Id('Sign')], [new IDLType.Id('Value')], [IDLFuncAnn.valueOf('Query')]),
            createMethod(new IDLToken.TextVal('returnChat'), [], [new IDLType.Id('Chat')], [IDLFuncAnn.valueOf('Query')]),
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

    private static int getIndex(String name) {
        int idx
        if (containsPrefix(name)) {
            String value = stripPrefix(name)
            idx = value.length() == 0 ? 0 : Integer.parseInt(value, 16)
        } else if(name.isInteger()) {
            idx = name.toInteger()
        } else {
            idx = UtilsKt.idlHash(name)
        }
        return idx
    }

    private static boolean containsPrefix(String input) {
        return input.length() > 1 && input[0] == '0' && input[1] == 'x'
    }

    private static String stripPrefix(String input) {
        if (containsPrefix(input)) {
            return input.substring(2)
        } else {
            return input
        }
    }

    private static IDLFieldType createFieldType(int idx, IDLType type) {
        new IDLFieldType(null, type, idx)
    }

    private static IDLFieldType createFieldType(String name, IDLType type) {
        new IDLFieldType(name, type, getIndex(name))
    }

    private static IDLMethod createMethod(IDLName methodName, List<IDLType> arguments, List<IDLType> results) {
        createMethod(methodName, arguments, results, [])
    }

    private static IDLMethod createMethod(IDLName methodName, List<IDLType> arguments, List<IDLType> results, List<IDLFuncAnn> annotations) {
        createMethodWithNamedParameters(methodName, arguments.collect { new Tuple2<String, IDLType>(null, it)} as List<Tuple2<String, IDLType>>, results.collect { new Tuple2<String, IDLType>(null, it) } as List<Tuple2<String, IDLType>>, annotations)
    }

    private static IDLMethod createMethodWithNamedParameters(IDLName methodName, List<Tuple2<String,IDLType>> arguments, List<Tuple2<String,IDLType>> results) {
        createMethodWithNamedParameters(methodName, arguments, results, [])
    }

    private static IDLMethod createMethodWithNamedParameters(IDLName methodName, List<Tuple2<String,IDLType>> arguments, List<Tuple2<String,IDLType>> results, List<IDLFuncAnn> annotations) {
        IDLMethodType methodType = createFunctionWithParameters(arguments, results, annotations)
        new IDLMethod(methodName, methodType)
    }

    private static IDLType.Reference.Func createFunctionWithParameters(List<Tuple2<String, IDLType>> arguments, List<Tuple2<String, IDLType>> results, List<IDLFuncAnn> annotations) {
        new IDLType.Reference.Func(arguments.collect { new IDLArgType(it.v1, it.v2) }, results.collect { new IDLArgType(it.v1, it.v2) }, annotations)
    }

    private static IDLType.Reference createReferenceFunction(List<IDLType> arguments, List<IDLType> results, List<IDLFuncAnn> annotations) {
        new IDLType.Reference.Func(arguments.collect { new IDLArgType(null, it) }, results.collect { new IDLArgType(null, it) }, annotations)
    }

    private static IDLType.Reference createReferenceService(List<IDLMethod> methods) {
        new IDLType.Reference.Service(methods)
    }

    private static IDLProgram createProgram(List<IDLMethod> methods) {
        createProgram(methods, [])
    }

    private static IDLProgram createProgram(List<IDLMethod> methods, List<IDLDef.Type> types) {
        createProgram(null, methods, types)
    }

    private static IDLProgram createProgram(String serviceName, List<IDLMethod> methods, List<IDLDef.Type> types) {
        createProgram(serviceName, methods, types, [])
    }

    private static IDLProgram createProgram(String serviceName, List<IDLMethod> methods, List<IDLDef.Type> types, List<IDLDef.Import> imports) {
        IDLType.Reference reference = new IDLType.Reference.Service(methods)
        IDLActor actor = new IDLActor(serviceName, reference)
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
