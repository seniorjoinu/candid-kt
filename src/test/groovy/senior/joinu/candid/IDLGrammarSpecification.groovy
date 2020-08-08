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
        String idl = """
            ${getTypes(types)}
            service : {
               "$methodName": (${getVarargs(arguments)}) -> (${getVarargs(results)});
            }
        """.stripIndent()
        println(">>> IDL $idl")
        List<IDLFuncAnn> annotations = []
        IDLMethodType methodType = new IDLType.Reference.Func(arguments.collect { new IDLArgType(null, it) }, results.collect { new IDLArgType(null, it) }, annotations)
        IDLMethod method = new IDLMethod(methodName, methodType)
        List<IDLDef.Import> imports = []
        List<IDLMethod> methods = [method]
        IDLActorDef actor = new IDLActorDef(null, new IDLType.Reference.Service(methods))
        IDLProgram program = new IDLProgram(imports, types, actor)

        when: 'the Kotlin source is generated from the IDL'
        IDLProgram result = GrammarKt.parseToEnd(IDLGrammar.INSTANCE, idl)

        then: 'the program matches the expectation'
        noExceptionThrown()
        result == program

        and: 'printed to standard output for no good reason'
        TranspileContext context = KtTranspiler.INSTANCE.transpile(result, "tld.d.etc", "Test.kt")
        FileSpec spec = context.currentSpec.build()
        println("<<< Kotlin")
        spec.writeTo(System.out)

        where: 'the service signature is as defined here'
        methodName      | arguments                                                                                     | results
        'greet'         | [IDLType.Primitive.Text.INSTANCE]                                                             | [IDLType.Primitive.Text.INSTANCE]
        'configure'     | [IDLType.Primitive.Text.INSTANCE]                                                             | []
        'get'           | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE)]                               | [new IDLType.Constructive.Opt(new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))]
        'put'           | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE)] | [IDLType.Primitive.Bool.INSTANCE]
        'getInHex'      | [IDLType.Primitive.Text.INSTANCE]                                                             | [new IDLType.Constructive.Opt(IDLType.Primitive.Nat8.INSTANCE)]
        'putInHex'      | [IDLType.Primitive.Text.INSTANCE, IDLType.Primitive.Text.INSTANCE]                            | [IDLType.Primitive.Bool.INSTANCE]
        'getWithTrace'  | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Id('Bucket')]     | [new IDLType.Constructive.Opt(new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE))]
        'putWithTrace'  | [new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), new IDLType.Id('Bucket')]     | [IDLType.Primitive.Bool.INSTANCE]
        'initialize'    | []                                                                                            | []
        'peers'         | []                                                                                            | [new IDLType.Id('List_2')]
        'ping'          | []                                                                                            | []
        'size'          | []                                                                                            | [IDLType.Primitive.Natural.INSTANCE]
        'whoami'        | []                                                                                            | [IDLType.Primitive.Natural.INSTANCE]
        types << [
                [],
                [],
                [],
                [],
                [],
                [],
                [
                    new IDLDef.Type('Key', new IDLType.Constructive.Record([new IDLFieldType('preimage', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), UtilsKt.idlHash('preimage')), new IDLFieldType('image', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), UtilsKt.idlHash('image'))])),
                    new IDLDef.Type('List', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([new IDLFieldType(null, new IDLType.Id('Key'), 0), new IDLFieldType(null, new IDLType.Id('List'), 1)]))),
                    new IDLDef.Type('Bucket', new IDLType.Id('List'))
                ],
                [
                    new IDLDef.Type('Key', new IDLType.Constructive.Record([new IDLFieldType('preimage', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), UtilsKt.idlHash('preimage')), new IDLFieldType('image', new IDLType.Constructive.Vec(IDLType.Primitive.Nat8.INSTANCE), UtilsKt.idlHash('image'))])),
                    new IDLDef.Type('List', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([new IDLFieldType(null, new IDLType.Id('Key'), 0), new IDLFieldType(null, new IDLType.Id('List'), 1)]))),
                    new IDLDef.Type('Bucket', new IDLType.Id('List'))
                ],
                [],
                [
                    new IDLDef.Type('List_2', new IDLType.Constructive.Opt(new IDLType.Constructive.Record([new IDLFieldType(null, IDLType.Primitive.Text.INSTANCE, 0), new IDLFieldType(null, new IDLType.Id('List_2'), 1)]))),
                ],
                [],
                [],
                [],
        ]
    }

    private static String getVarargs(List<IDLType> args) {
        return args.collect { it.toString() }.join(', ')
    }

    private static String getTypes(List<IDLDef.Type> types) {
        return types.collect { it.toString() }.join(System.getProperty('line.separator'))
    }
}
