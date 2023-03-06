package fixtures

import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import java.net.{HttpURLConnection, InetSocketAddress, URI}

case class Response(method: String, context: String, body: Option[String], code: Int, contentType: String)

class TestHttpServer(responses: Seq[Response], usePort: Option[Int] = None) {
  val port: Int = usePort.getOrElse(RandomPort.newPort)
  private val server = HttpServer.create(new InetSocketAddress(port), 0)

  val httpBase: URI = URI.create(s"http://localhost:$port")

  def start(): Unit = {
    server.setExecutor(null)
    server.start()
    responses.foreach { response =>
      server.createContext(response.context, handler)
    }
  }

  def shutdown(): Unit = server.stop(0)

  private val handler = new HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      responses.find(r => r.method == exchange.getRequestMethod &&
        r.context == exchange.getHttpContext.getPath)
        .flatMap { response =>
          exchange.getResponseHeaders.add("Content-Type", response.contentType)
          exchange.sendResponseHeaders(response.code, response.body.map(_.length.longValue()).getOrElse(0L))
          response.body.map { body =>
            exchange.getResponseBody.write(body.getBytes)
            exchange.getResponseBody.flush()
            exchange.close()
          }
        }
        .getOrElse { () =>
          exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0)
          exchange.close()
        }
    }
  }

}
