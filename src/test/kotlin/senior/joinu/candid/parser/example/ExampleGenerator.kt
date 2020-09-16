package senior.joinu.candid.parser.example

import senior.joinu.candid.CandidCodeGenerator

object ExampleGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = CandidCodeGenerator.Source.Str(
            data = """
                type Phone = nat;
                type Name = text;
                type Entry = 
                 record {
                   description: text;
                   name: Name;
                   phone: Phone;
                 };
                service : {
                  insert: (Name, text, Phone) -> ();
                  lookup: (Name) -> (opt Entry) query;
                }
            """.trimIndent(),
            generatedFileName = "phonebook.did"
        )
        val fileSpec = CandidCodeGenerator.generateFor(src)

        fileSpec.writeTo(System.out)
    }
}
