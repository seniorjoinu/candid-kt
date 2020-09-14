package senior.joinu.candid.parser

import org.junit.jupiter.api.Test
import senior.joinu.candid.transpile.SimpleIDLPrincipal

class PrincipalTest {
    @Test
    fun `principal can be calculated from text and vice-versa`() {
        val principalText = "7kncf-oidaa-aaaaa-aaaaa-aaaaa-aaaaa-aaaaa-q"
        val p = SimpleIDLPrincipal.fromText(principalText)

        assert(p.toText() == principalText) { "It is not" }
    }
}
