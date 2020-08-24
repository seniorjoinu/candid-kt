package senior.joinu.candid

import com.squareup.kotlinpoet.FileSpec
import senior.joinu.candid.transpile.KtTranspiler
import senior.joinu.candid.transpile.TranspileContext
import spock.lang.Specification

/**
 * A base specification providing common IDL grammar support.
 */
class IDLGrammarSpecificationBase extends Specification {

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

    static IDLFieldType createFieldType(int idx, IDLType type) {
        new IDLFieldType(null, type, idx)
    }

    static IDLFieldType createFieldType(String name, IDLType type) {
        new IDLFieldType(name, type, getIndex(name))
    }

    static IDLMethod createMethod(IDLName methodName, List<IDLType> arguments, List<IDLType> results) {
        createMethod(methodName, arguments, results, [])
    }

    static IDLMethod createMethod(IDLName methodName, List<IDLType> arguments, List<IDLType> results, List<IDLFuncAnn> annotations) {
        createMethodWithNamedParameters(methodName, arguments.collect { new Tuple2<String, IDLType>(null, it)} as List<Tuple2<String, IDLType>>, results.collect { new Tuple2<String, IDLType>(null, it) } as List<Tuple2<String, IDLType>>, annotations)
    }

    static IDLMethod createMethodWithNamedParameters(IDLName methodName, List<Tuple2<String,IDLType>> arguments, List<Tuple2<String,IDLType>> results) {
        createMethodWithNamedParameters(methodName, arguments, results, [])
    }

    static IDLMethod createMethodWithNamedParameters(IDLName methodName, List<Tuple2<String,IDLType>> arguments, List<Tuple2<String,IDLType>> results, List<IDLFuncAnn> annotations) {
        IDLMethodType methodType = createFunctionWithParameters(arguments, results, annotations)
        new IDLMethod(methodName, methodType)
    }

    static IDLType.Reference.Func createFunctionWithParameters(List<Tuple2<String, IDLType>> arguments, List<Tuple2<String, IDLType>> results, List<IDLFuncAnn> annotations) {
        new IDLType.Reference.Func(arguments.collect { new IDLArgType(it.v1, it.v2) }, results.collect { new IDLArgType(it.v1, it.v2) }, annotations)
    }

    static IDLType.Reference createReferenceFunction(List<IDLType> arguments, List<IDLType> results, List<IDLFuncAnn> annotations) {
        new IDLType.Reference.Func(arguments.collect { new IDLArgType(null, it) }, results.collect { new IDLArgType(null, it) }, annotations)
    }

    static IDLType.Reference createReferenceService(List<IDLMethod> methods) {
        new IDLType.Reference.Service(methods)
    }

    static IDLProgram createProgram(List<IDLMethod> methods) {
        createProgram(methods, [])
    }

    static IDLProgram createProgram(List<IDLMethod> methods, List<IDLDef.Type> types) {
        createProgram(null, methods, types)
    }

    static IDLProgram createProgram(String serviceName, List<IDLMethod> methods, List<IDLDef.Type> types) {
        createProgram(serviceName, methods, types, [])
    }

    static IDLProgram createProgram(String serviceName, List<IDLMethod> methods, List<IDLDef.Type> types, List<IDLDef.Import> imports) {
        IDLType.Reference reference = new IDLType.Reference.Service(methods)
        IDLActor actor = new IDLActor(serviceName, reference)
        IDLProgram program = new IDLProgram(imports, types, actor)
        println("```candid")
        println(program)
        println("```")
        program
    }

    static void transpileProgram(IDLProgram program) {
        TranspileContext context = KtTranspiler.INSTANCE.transpile(program, "tld.d.etc", "Test.kt")
        FileSpec spec = context.currentSpec.build()
        println("```kotlin")
        spec.writeTo(System.out)
        println("```")
    }
}
