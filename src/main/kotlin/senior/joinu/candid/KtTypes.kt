package senior.joinu.candid


open class SimpleIDLService(
    var host: String?,
    val id: ByteArray?
)

open class SimpleIDLFunc(
    val funcName: String?,
    val service: SimpleIDLService?
)

open class SimpleIDLPrincipal(
    val id: ByteArray?
)

object Null
object Reserved
object Empty