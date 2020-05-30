package senior.joinu.candid

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeAliasSpec

object KtTranspiler {
    fun transpile(program: IDLProgram, packageName: String, fileName: String): FileSpec {
        val currentSpec = FileSpec.builder(packageName, fileName)
        // TODO: handle imports

        program.types.forEach { (name, value) ->
            val typeAliasSpec = TypeAliasSpec
                .builder(name, value.transpile(currentSpec, packageName))
                .build()

            currentSpec.addTypeAlias(typeAliasSpec)
        }

        if (program.actor != null) {
            if (program.actor.name != null) {
                val typeAliasSpec = TypeAliasSpec
                    .builder(
                        program.actor.name,
                        (program.actor.type as IDLType).transpile(currentSpec, packageName)
                    )
                    .build()

                currentSpec.addTypeAlias(typeAliasSpec)
            }
        }

        return currentSpec.build()
    }
}