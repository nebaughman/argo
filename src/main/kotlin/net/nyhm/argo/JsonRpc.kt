package net.nyhm.argo

data class RpcId(val id: Any)

data class RpcMethod(val method: String)

data class RpcVersion(val version: String)

/**
 * Constant for RpcVersion 2.0 (current expected version)
 */
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

class RpcError(
    val code: Int,
    val message: String,
    val data: Any?
)

class RpcResponse(
    val version: RpcVersion = RPC_VERSION_2_0,
    val id: RpcId,
    val result: Any?,
    val error: RpcError? = null
) {
  companion object {
    fun success(id: RpcId, result: Any) = RpcResponse(id = id, result = result)
    fun success(source: RpcRequest, result: Any) = success(source.id!!, result)
  }
}

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
