package senior.joinu.candid

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import senior.joinu.candid.transpile.KtTranspiler
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object CandidCodeGenerator {
    fun generateFor(
        didPath: Path,
        genPath: Path,
        genPackage: String,
        didEncoding: Charset = StandardCharsets.UTF_8
    ) {
        val did = didPath.toFile().readText(didEncoding)

        val program = IDLGrammar.parseToEnd(did)
        val ktContext = KtTranspiler.transpile(program, genPackage, genPath.fileName.toString())
        val spec = ktContext.currentSpec.build()

        spec.writeTo(genPath)
    }
}
