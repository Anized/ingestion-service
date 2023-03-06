package fixtures

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.anized.ingestor.persistence.DocumentRepository
import com.dimafeng.testcontainers.PostgreSQLContainer
import slick.jdbc.JdbcBackend.Database

import java.sql.DriverManager
import scala.util.Using

trait DatabaseFixtures {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "TestSystem")
  val documentSchemaDDL: String = "schema/documents.ddl"
  val dbName: String = "testdb"
  val pgUsername: String = "username"
  val pgPassword: String = "password"

  lazy val container: PostgreSQLContainer = PostgreSQLContainer.apply(
    databaseName = dbName, username = pgUsername, password = pgPassword)

  lazy val documentRepo = new DocumentRepository(Database.forURL(
    container.jdbcUrl, driver = container.driverClassName, user = pgUsername, password = pgPassword))

  def executeScript(container: PostgreSQLContainer, script: String): Unit =
    Using.Manager { _ =>
      Class.forName(container.driverClassName)
      val conn = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
      val stmt = conn.createStatement
      stmt.executeUpdate(script)
    }
}
