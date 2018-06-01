package net.nyhm.argo

import com.google.gson.Gson
import io.netty.util.CharsetUtil
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

val log = LoggerFactory.getLogger("ArgoTestLogger")

const val port = 8888

class ArgoTest {
  @Test
  fun testArgo() {
    val parser = GsonRpcParser()
    ArgoServer(parser, port).use { server ->
      server.start()
      server.register("test", DebugHandler())
      sendRequest("test")
      server.register("default", DefaultHandler())
      sendRequest("default")
    }
  }
}

class DebugHandler: MethodHandler {
  override fun handle(request: RpcRequest): RpcResponse? {
    log.info("Handling request", request)
    val id = request.id
    return if (id == null) {
      null
    } else {
      RpcResponse.success(id, "ok")
    }
  }
}

class DefaultHandler: DefaultMethodHandler() {
  override fun handleNotification(method: RpcMethod, params: RpcParams?) {
    log.info("handleNotification method:{}, params:{}", method, params)
  }

  override fun handleRequest(method: RpcMethod, params: RpcParams?): Any? {
    log.info("handleRequest method:{}, params:{}", method, params)
    return "ok"
  }
}

private val nextId = AtomicInteger()

fun sendRequest(method: String) {
  val request = mapOf(
      "jsonrpc" to "2.0",
      "id" to nextId.getAndIncrement(),
      "method" to method
  )
  val json = Gson().toJson(request)
  log.info("Sending request: $json")
  val response = sendPost(json)
  log.info("Response: $response")
}


/**
 * Send a POST request with the given body to the serverUri()
 */
@Throws(Exception::class)
private fun sendPost(body: String): String {
  buildClient().use { client ->
    val request = HttpPost(serviceUri())
    request.entity = StringEntity(body, CharsetUtil.UTF_8)
    val response = client.execute(request)
    return EntityUtils.toString(response.getEntity())
  }
}

private fun buildClient() = HttpClientBuilder.create()
    .disableAutomaticRetries()
    .build()

private fun serviceUri() = URIBuilder()
    .setScheme("http")
    .setHost("localhost")
    .setPort(port)
    .build()