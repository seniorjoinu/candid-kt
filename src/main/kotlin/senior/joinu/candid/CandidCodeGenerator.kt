package senior.joinu.candid

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.squareup.kotlinpoet.FileSpec
import senior.joinu.candid.idl.IDLGrammar
import senior.joinu.candid.transpile.KtTranspiler
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path


object CandidCodeGenerator {
    sealed class Source {
        data class Str(
            val data: String,
            val generatedFileName: String
        ) : Source()

        data class File(
            val path: Path,
            val generatedFileName: String? = null,
            val encoding: Charset = StandardCharsets.UTF_8
        ) : Source()
    }

    fun generate(
        input: Source,
        file: File,
        genPackage: String = ""
    ) {
        generateFor(input, genPackage).writeTo(file)
    }

    fun generate(
        input: Source,
        file: Path,
        genPackage: String = ""
    ) {
        generateFor(input, genPackage).writeTo(file)
    }

    fun generate(
        input: Source,
        genPackage: String = ""
    ): String {
        val sb = StringBuilder()
        generateFor(input, genPackage).writeTo(sb)

        return sb.toString()
    }

    internal fun generateFor(
        input: Source,
        genPackage: String = ""
    ): FileSpec {
        val ktContext = when (input) {
            is Source.Str -> {
                val did = input.data
                val program = IDLGrammar.parseToEnd(did)

                KtTranspiler.transpile(
                    program = program,
                    packageName = genPackage,
                    fileName = input.generatedFileName
                )
            }
            is Source.File -> {
                val did = input.path.toFile().readText(input.encoding)
                val program = IDLGrammar.parseToEnd(did)

                KtTranspiler.transpile(
                    program = program,
                    packageName = genPackage,
                    fileName = input.generatedFileName ?: input.path.fileName.toString()
                )
            }
        }


        return ktContext.currentSpec.build()
    }
}
