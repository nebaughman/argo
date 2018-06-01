package net.nyhm.argo

import net.nyhm.bitty.*

class ArgoServer(parser: RpcParser<String>, port: Int): AutoCloseable
{
  private val processor = RpcProcessor()
  private val logic = ArgoLogic(parser, processor)
  private val service = BittyService(HttpServer(logic, port, 1, 1))

  fun register(method: String, handler: MethodHandler) {
    processor.register(RpcMethod(method), handler)
  }

  fun start() = service.start()
  fun stop() = service.stop()
  override fun close() = stop()
}

class ArgoLogic(
    val parser: RpcParser<String>,
    val processor: RpcProcessor
): ServerLogic {
  override fun processRequest(req: ClientRequest, res: ServerResponse) {
    val request = parser.parseRequest(req.body)
    val response = processor.handle(request)
    if (response == null && request.id != null) throw Exception("Request with id must return a response")
    if (response != null) {
      res.respond(parser.export(response))
    } else {
      // TODO: do not send any response body; just close response (gap in Bitty lib)
      res.respond("")
    }
  }
}

interface MethodHandler {
  fun handle(request: RpcRequest): RpcResponse?
}

class RpcProcessor
{
  private val handlers = mutableMapOf<RpcMethod,MethodHandler>()

  fun register(method: RpcMethod, handler: MethodHandler) = handlers.put(method, handler)

  fun handle(request: RpcRequest): RpcResponse? = handlers[request.method]?.handle(request)
      ?: throw RpcParseError("No such method: ${request.method}") // TODO: proper rpc error response
}