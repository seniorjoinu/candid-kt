package senior.joinu.candid.idl

import senior.joinu.candid.transpile.KtTranspiler
import senior.joinu.candid.utils.idlHash

/**
 * Helper (extension) functions used by the sibling specifications.
 */
fun IDLType.asArg(): IDLArgType {
    return IDLArgType(null, this)
}

fun program(
    methods: List<IDLMethod>,
    types: List<IDLDef.Type> = emptyList(),
    imports: List<IDLDef.Import> = emptyList(),
    serviceName: String? = null
) = IDLProgram(
    imports = imports,
    types = types,
    actor = IDLActor(serviceName, IDLType.Reference.Service(methods))
)
fun IDLProgram.println(): IDLProgram {
    println("```candid")
    println(this)
    println("```")
    return this
}
fun IDLProgram.transpile(): IDLProgram {
    val context = KtTranspiler.transpile(this, "tld.d.etc", "Test.kt")
    val spec = context.currentSpec.build()
    println("```kotlin")
    spec.writeTo(System.out)
    println("```")
    return this
}

fun method(
    methodName: IDLName,
    arguments: List<IDLArgType> = emptyList(),
    results: List<IDLArgType> = emptyList(),
    annotations: List<IDLFuncAnn> = emptyList()
) = IDLMethod (
    name = methodName,
    type = function(arguments, results, annotations)
)

fun function(
    arguments: List<IDLArgType> = emptyList(),
    results: List<IDLArgType> = emptyList(),
    annotations: List<IDLFuncAnn> = emptyList()
) = IDLType.Reference.Func(
    arguments = arguments,
    results = results,
    annotations = annotations
)

fun service(methods: List<IDLMethod>) = IDLType.Reference.Service(methods)

fun fieldType(idx: Int, type: IDLType): IDLFieldType {
    return IDLFieldType(null, type, idx)
}

fun fieldType(name: String, type: IDLType): IDLFieldType {
    return IDLFieldType(name, type, getIndex(name))
}

private fun getIndex(name: String): Int {
    val idx: Int?
    idx = if (containsPrefix(name)) {
        val value = stripPrefix(name)
        if (value.isEmpty()) 0 else value.toInt(16)
    } else {
        name.toIntOrNull()
    }
    return idx ?: idlHash(name)
}

private fun containsPrefix(input: String): Boolean {
    return input.length > 1 && input[0] == '0' && input[1] == 'x'
}

private fun stripPrefix(input: String): String {
    return if (containsPrefix(input)) {
        input.substring(2)
    } else {
        input
    }
}
