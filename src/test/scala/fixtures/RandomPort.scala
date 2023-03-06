package fixtures

import org.awaitility.Awaitility.await

import java.net.ServerSocket
import scala.util.Using

object RandomPort {

  def newPort: Int =
    Using(new ServerSocket(0)) { socket =>
      await().until(() => socket.isBound)
      socket.getLocalPort
    }.get
}
