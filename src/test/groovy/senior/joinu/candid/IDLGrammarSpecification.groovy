package senior.joinu.candid

import com.github.h0tk3y.betterParse.grammar.GrammarKt
import spock.lang.Unroll

/**
 * System under specification: {@link IDLGrammar}.
 * @author tglaeser
 */
class IDLGrammarSpecification extends IDLGrammarSpecificationBase {

    def 'positive multiple service with multiple nested parameters'() {
        given: 'a test fixture'
        String serviceName = 'server'
        List<IDLDef.Import> imports = [
            new IDLDef.Import(new IDLToken.TextVal('test.did'))
        ]
        List<IDLDef.Type> types = [
            new IDLDef.Type('my_type', IDLType.Primitive.Nat8.INSTANCE),
            new IDLDef.Type('List', new IDLType.Constructive.Record([fieldType('head', IDLType.Primitive.Integer.INSTANCE), fieldType('tail', new IDLType.Constructive.Opt(new IDLType.Id('List')))]) ),
            new IDLDef.Type('f', functionAsReference([new IDLType.Id('List'), functionAsReference([IDLType.Primitive.Int32.INSTANCE], [IDLType.Primitive.Int64.INSTANCE], [])], [new IDLType.Constructive.Opt(new IDLType.Id('List'))], [])),
            new IDLDef.Type('broker', serviceAsReference([methodWithNamedArgTypes(new IDLType.Id('find'), [new Tuple2<>('name', IDLType.Primitive.Text.INSTANCE)], [new Tuple2<>(null, serviceAsReference([method(new IDLType.Id('current'), [], [IDLType.Primitive.Nat32.INSTANCE]), method(new IDLType.Id('up'), [], [])]))])])),
            new IDLDef.Type('nested', new IDLType.Constructive.Record([fieldType(0, IDLType.Primitive.Natural.INSTANCE), fieldType(1, IDLType.Primitive.Natural.INSTANCE), fieldType(2, new IDLType.Constructive.Record([fieldType(0, IDLType.Primitive.Natural.INSTANCE), fieldType(1, IDLType.Primitive.Nat8.INSTANCE), fieldType("0x2a", IDLType.Primitive.Natural.INSTANCE)])), fieldType(3, new IDLType.Constructive.Variant([fieldType(0, new IDLType.Id('A')), fieldType(1, new IDLType.Id('B')), fieldType(2, new IDLType.Id('C')), fieldType("0x2a", IDLType.Primitive.Null.INSTANCE)])), fieldType("40", IDLType.Primitive.Natural.INSTANCE), fieldType("42", IDLType.Primitive.Natural.INSTANCE)])),
        ]
        List<IDLMethod> methods = [
            methodWithNamedArgTypes(new IDLType.Id('f'), [new Tuple2<>('test', IDLType.Constructive.Blob.INSTANCE), new Tuple2<>(null, new IDLType.Constructive.Opt(IDLType.Primitive.Bool.INSTANCE))], [], [IDLFuncAnn.valueOf('Oneway')]),
            method(new IDLType.Id('g'), [new IDLType.Id('my_type'), new IDLType.Id('List'), new IDLType.Constructive.Opt(new IDLType.Id('List'))], [IDLType.Primitive.Integer.INSTANCE], [IDLFuncAnn.valueOf('Query')]),
            method(new IDLType.Id('h'), [new IDLType.Constructive.Vec(new IDLType.Constructive.Opt(IDLType.Primitive.Text.INSTANCE)), new IDLType.Constructive.Variant([fieldType('A', IDLType.Primitive.Natural.INSTANCE), fieldType('B', new IDLType.Constructive.Opt(IDLType.Primitive.Text.INSTANCE))]), new IDLType.Constructive.Opt(new IDLType.Id('List'))], [new IDLType.Constructive.Record([fieldType('0x2a', new IDLType.Constructive.Record([])), fieldType('id', IDLType.Primitive.Natural.INSTANCE)])], []),
            new IDLMethod(new IDLType.Id('i'), new IDLType.Id('f')),
        ]
        IDLProgram program = program(serviceName, methods, types, imports)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpile(result)
    }

    @Unroll def 'positive single service #methodName'() {
        given: 'a test fixture'
        IDLProgram program = program([method(new IDLToken.TextVal(methodName), arguments, results)])

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        noExceptionThrown()
        result == program

        and: 'printed to standard output for no good reason'
        transpile(program)

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
            new IDLDef.Type('Key', new IDLType.Constructive.Record([fieldType('preimage', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE)), fieldType('image', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))])),
            new IDLDef.Type('List', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([fieldType(0, new IDLType.Id('Key')), fieldType(1, new IDLType.Id('List'))]))),
            new IDLDef.Type('Bucket', new IDLType.Id('List')),
            new IDLDef.Type('List_2', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([fieldType(0, IDLType.Primitive.Text.INSTANCE), fieldType(1, new IDLType.Id('List_2'))]))),
        ]
        IDLProgram program = program([method(new IDLToken.TextVal(methodName), arguments, results)], types)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpile(result)

        where: 'the service signature is as defined here'
        methodName      | arguments     | results
        'peers'         | []            | [new IDLType.Id('List_2')]
        'getWithTrace'  | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Id('Bucket')] | [new IDLType.Constructive.Opt(new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))]
        'putWithTrace'  | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Id('Bucket')] | [IDLType.Primitive.Bool.INSTANCE]
    }

    def 'positive single service with recursive parameters'() {
        given: 'a test fixture'
        List<IDLDef.Type> types = [
            new IDLDef.Type('Version_3', new IDLType.Constructive.Variant([fieldType('Version', IDLType.Primitive.Natural.INSTANCE)])),
            new IDLDef.Type('Version_2', new IDLType.Id('Version_3')),
            new IDLDef.Type('Version', new IDLType.Id('Version_2')),
            new IDLDef.Type('Mode_3', new IDLType.Constructive.Variant([fieldType('Kanji', IDLType.Primitive.Null.INSTANCE), fieldType('Numeric', IDLType.Primitive.Null.INSTANCE), fieldType('EightBit', IDLType.Primitive.Null.INSTANCE), fieldType('Alphanumeric', IDLType.Primitive.Null.INSTANCE)])),
            new IDLDef.Type('Mode_2', new IDLType.Id('Mode_3')),
            new IDLDef.Type('Mode', new IDLType.Id('Mode_2')),
            new IDLDef.Type('ErrorCorrection_3', new IDLType.Constructive.Variant([fieldType('H', IDLType.Primitive.Null.INSTANCE), fieldType('L', IDLType.Primitive.Null.INSTANCE), fieldType('M', IDLType.Primitive.Null.INSTANCE), fieldType('Q', IDLType.Primitive.Null.INSTANCE)])),
            new IDLDef.Type('ErrorCorrection_2', new IDLType.Id('ErrorCorrection_3')),
            new IDLDef.Type('ErrorCorrection', new IDLType.Id('ErrorCorrection_2')),
        ]
        List<IDLMethod> methods = [method(new IDLToken.TextVal('encode'), [new IDLType.Id('Version'), new IDLType.Id('ErrorCorrection'), new IDLType.Id('Mode'), IDLType.Primitive.Text.INSTANCE], [IDLType.Primitive.Text.INSTANCE])]
        IDLProgram program = program(methods, types)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpile(result)
    }

    def 'positive multiple services with parameters and annotations'() {
        given: 'a test fixture'
        List<IDLDef.Type> types = [
            new IDLDef.Type('Value', new IDLType.Constructive.Record([fieldType('i', IDLType.Primitive.Integer.INSTANCE), fieldType('n', IDLType.Primitive.Natural.INSTANCE)])),
            new IDLDef.Type('Sign', new IDLType.Constructive.Variant([fieldType('Plus', IDLType.Primitive.Null.INSTANCE), fieldType('Minus', IDLType.Primitive.Null.INSTANCE)])),
            new IDLDef.Type('Message', new IDLType.Constructive.Record([fieldType('sender', IDLType.Primitive.Text.INSTANCE), fieldType('message', IDLType.Primitive.Text.INSTANCE)])),
            new IDLDef.Type('Chat', new IDLType.Constructive.Vec(new IDLType.Id('Message'))),
        ]
        List<IDLMethod> methods = [
            method(new IDLToken.TextVal('addMessageAndReturnChat'), [new IDLType.Id('Message')], [new IDLType.Id('Chat')]),
            method(new IDLToken.TextVal('getValue'), [new IDLType.Id('Sign')], [new IDLType.Id('Value')], [IDLFuncAnn.valueOf('Query')]),
            method(new IDLToken.TextVal('returnChat'), [], [new IDLType.Id('Chat')], [IDLFuncAnn.valueOf('Query')]),
        ]
        IDLProgram program = program(methods, types)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, program.toString())

        then: 'the program matches the expectation'
        result == program
        noExceptionThrown()

        and: 'printed to standard output for no good reason'
        transpile(result)
    }
}
