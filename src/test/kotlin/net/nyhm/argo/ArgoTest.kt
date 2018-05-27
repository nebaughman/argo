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

val log = LoggerFactory.getLogger("ArgoTestLogger")

const val port = 8888

class ArgoTest {
  @Test
  fun testArgo() {
    val parser = GsonRpcParser()
    ArgoServer(parser, port).use { server ->
      server.start()
      server.register("test", DebugHandler())
      sendRequest()
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
      RpcResponse(
          RPC_VERSION_2_0,
          RpcResult("ok"),
          null, // TODO: proper rpc error handling
          id // same as request
      )
    }
  }
}

fun sendRequest() {
  val request = mapOf(
      "jsonrpc" to "2.0",
      "id" to 1,
      "method" to "test"
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