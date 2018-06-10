package net.nyhm.argo

data class RpcId(val id: Any)

// TODO: Instead of simple string matching, allow regex (or define simple wildcard syntax);
// This could just be a string; matching/routing logic can be handled by an RpcHandler
//
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
  @Throws(JsonRpcException::class)
  fun <T> get(index: Int, type: Class<T>): T?

  /**
   * Only for non-positional params
   */
  @Throws(JsonRpcException::class)
  fun <T> get(name: String, type: Class<T>): T?
}

data class RpcRequest(
    val version: RpcVersion,
    val method: RpcMethod,
    val params: RpcParams?,
    val id: RpcId?
) {
  val isNotification = id == null
}

/**
 * This class represents a structured RPC error message.
 * Factory methods produce specification-defined errors.
 *
 * These errors (with codes in the -32000 number space) should generally be reserved for
 * the framework, while applications should define their own error codes outside this
 * number space. A safe bet is to define application error codes in the positive range.
 */
class RpcError(
    val code: Int,
    val message: String,
    val data: Any?
) {
  companion object {
    fun parseError(data: Any? = null) = RpcError(-32700, "Parse error", data)
    fun invalidRequest(data: Any? = null) = RpcError(-32600, "Invalid request", data)
    fun methodNotFound(data: Any? = null) = RpcError(-32601, "Method not found", data)
    fun invalidParams(data: Any? = null) = RpcError(-32602, "Invalid params", data)
    fun internalError(data: Any? = null) = RpcError(-32603, "Internal error", data)

    /**
     * This method enforces that "Server error" messages must have a code within the
     * inclusive range: -32099 to -32000
     */
    fun serverError(code: Int, data: Any? = null): RpcError {
      if (code !in -32099..-32000) throw JsonRpcException("Invalid serverError code (outside -32099..-32000): $code")
      return RpcError(code, "Server error", data)
    }
  }
}

class RpcResponse(
    val version: RpcVersion = RPC_VERSION_2_0,
    val id: RpcId,
    val result: Any?,
    val error: RpcError? = null
) {
  companion object {
    fun success(id: RpcId, result: Any) = RpcResponse(id = id, result = result)
    fun success(source: RpcRequest, result: Any) = success(source.id!!, result)
    fun error(id: RpcId, error: RpcError) = RpcResponse(id = id, error = error, result = null)
  }
}

class JsonRpcException(
    override val message: String,
    override val cause: Throwable? = null,
    val json: Any? = null
): Exception(message, cause)

interface RpcParser<T> {
  @Throws(JsonRpcException::class)
  fun parseRequest(json: T): RpcRequest

  @Throws(JsonRpcException::class)
  fun export(response: RpcResponse): T
}