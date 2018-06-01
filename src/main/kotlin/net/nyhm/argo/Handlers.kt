package net.nyhm.argo

import org.slf4j.LoggerFactory

/**
 * Utility implementation of a [MethodHandler] that interprets requests, requiring
 * a response to non-notification requests, and handling errors.
 *
 * Instead of directly interacting with the request, implementations interact
 * only with the method and params via [handleRequest] and/or [handleNotification] methods.
 *
 * Responses are also interpreted and formed into full [RpcResponse] objects.
 * Implementations may return an [RpcError] or any other data type as the result payload.
 *
 * Exceptions are handled by producing an [RpcError]. Non-notification requests will
 * receive a JSON-RPC "Internal error" response. The contents of the exception are *not*
 * included in the [RpcResponse] (to avoid inadvertently exposing internal state to clients).
 */
abstract class DefaultMethodHandler: MethodHandler
{
  companion object {
    val log = LoggerFactory.getLogger(DefaultMethodHandler::class.java)
  }

  override fun handle(request: RpcRequest): RpcResponse? {
    try {
      return if (request.id == null) {
        handleNotification(request.method, request.params)
        null
      } else {
        val result = handleRequest(request.method, request.params)
        // TODO: If request has id but result == null, make sure response includes "result: null" field (check json-rpc spec)
        RpcResponse(
            id = request.id,
            result = if (result is RpcError) null else result,
            error = if (result is RpcError) result else null
        )
      }
    } catch (e: Exception) { // TODO: catch Throwable?
      log.warn("Error processing notification", e)
      // TODO: allow user to provide an exception handler callback? (for notification and non-notification requests)
      return if (request.id != null) {
        RpcResponse.error(id = request.id, error = RpcError.internalError())
      } else {
        null // notification requests cannot have a return
      }
    }
  }

  // TODO: Single handler method with isNotification boolean; disregard return value if notification?
  /**
   * Handle a notification request (which has no return value)
   */
  abstract fun handleNotification(method: RpcMethod, params: RpcParams?)

  /**
   * Return an [RpcError] for error or any other result (including null) for success.
   *
   * If this method throws an exception an [RpcError] is returned to the RPC caller.
   * Notice that the message of the exception is _not_ returned to the caller, since
   * it may unintentionally reveal internal state. Implementations should catch their
   * own exceptions and return [RpcError] objects to include application-specific
   * error types and messages.
   */
  abstract fun handleRequest(method: RpcMethod, params: RpcParams?): Any?
}