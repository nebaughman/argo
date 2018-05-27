package net.nyhm.argo

data class RpcId(val id: Any)

data class RpcMethod(val method: String)

class RpcVersion(val version: String) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RpcVersion

    if (version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    return version.hashCode()
  }
}

val RPC_VERSION_2_0 = RpcVersion("2.0")

interface RpcParams {
  /**
   * Whether these are positional array-based versus map-based params.
   * If positional, must call get(Int,Class), else must call get(String,Class)
   */
  fun isPositional(): Boolean

  /**
   * Only for positional params
   */
  @Throws(RpcParseError::class)
  fun <T> get(index: Int, type: Class<T>): T?

  /**
   * Only for non-positional params
   */
  @Throws(RpcParseError::class)
  fun <T> get(name: String, type: Class<T>): T?
}

data class RpcRequest(
    val version: RpcVersion,
    val method: RpcMethod,
    val params: RpcParams?,
    val id: RpcId?
)

class RpcResult(val result: Any?)

data class RpcErrorCode(val code: Int)
data class RpcErrorMessage(val message: String)
data class RpcErrorData(val data: Any)

class RpcError(
    val code: RpcErrorCode,
    val message: RpcErrorMessage,
    val data: RpcErrorData?
)

class RpcResponse(
    val version: RpcVersion,
    val result: RpcResult?,
    val error: RpcError?,
    val id: RpcId
)

class RpcParseError(
    override val message: String,
    override val cause: Throwable? = null,
    val json: Any? = null
): Exception(message, cause)

interface RpcParser<T> {
  @Throws(RpcParseError::class)
  fun parseRequest(json: T): RpcRequest

  @Throws(RpcParseError::class)
  fun export(response: RpcResponse): T
}
