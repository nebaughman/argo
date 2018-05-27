package net.nyhm.argo

import net.nyhm.bitty.*
import org.junit.Test
import java.util.*
import java.util.concurrent.Executors

/**
 * A simple driver to test the bitty (http server) dependency
 */
class BittyTest {

  companion object {
    const val port = 8888
  }

  @Test
  fun testBitty() {
    val server = HttpServer(BittyLogic, port, 1, 1)
    val future = Executors.newSingleThreadExecutor().submit({ server.start() })
    Thread.sleep(16000)
    future.cancel(true)
  }
}

/**
 * An echo & time service
 */
object BittyLogic: ServerLogic {
  override fun processRequest(req: ClientRequest, res: ServerResponse) {
    val msg = req.args["msg"] ?: "The current time is " + Date()
    res.setContentType(ContentType(MimeType("text", "plain"), Charsets.UTF_8))
    res.respond(msg)
  }
}