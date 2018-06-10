package net.nyhm.argo

import net.nyhm.bitty.*

/**
 * A [BittyService] integration of Argo JSON-RPC message handling.
 *
 * This class starts a service at the given port and processes JSON-RPC requests
 * with the given [RpcHandler], using the given [RpcParser] to parse requests
 * and export responses.
 */
class ArgoBittyService(
    handler: RpcHandler,
    parser: RpcParser<String>,
    port: Int
): AutoCloseable
{
  private val logic = ArgoBittyLogic(parser, handler)
  private val service = BittyService(HttpServer(logic, port, 1, 1))

  fun start() = service.start()
  fun stop() = service.stop()
  override fun close() = stop()
}

private class ArgoBittyLogic(
    val parser: RpcParser<String>,
    val handler: RpcHandler
): ServerLogic
{
  override fun processRequest(req: ClientRequest, res: ServerResponse) {
    val request = parser.parseRequest(req.body)
    val response = handler.handle(request)
    if (response == null && request.id != null) throw JsonRpcException("Request with id must return a response")
    if (response != null) {
      res.respond(parser.export(response))
    } else {
      // TODO: do not send any response body; just close response (gap in Bitty lib)
      res.respond("")
    }
  }
}