package org.anized.ingestor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import org.anized.ingestor.persistence.DocumentRepository
import org.anized.ingestor.routes.{DocumentRoutes, IngestorAPI, UploadRoutes}
import org.anized.ingestor.service.{VendorService, DocumentService, StorageService}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.{ApplicationArguments, ApplicationRunner}
import org.springframework.cloud.client.discovery.DiscoveryClient
import slick.jdbc.JdbcBackend.Database

import java.net.URI
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@SpringBootApplication
class IngestionServer(discoveryClient: DiscoveryClient) extends ApplicationRunner {
  private val logger = LoggerFactory.getLogger(classOf[IngestionServer])
  val POSTGRES_SERVICE_NAME = "postgres"
  @Value("${api.port}")
  private val port = 0

  val discovery: String => Try[URI] = service =>
    Try(discoveryClient.getInstances(service).asScala.head.getUri)

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    Http().newServerAt("0.0.0.0", port).bind(routes)
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info(s"Server online at http://${address.getHostName}:${address.getPort}/")
        case Failure(t) =>
          system.log.error(s"Failed to bind API endpoint, port:$port; terminating system", t)
          system.terminate()
      }
  }

  @throws[Exception]
  override def run(args: ApplicationArguments): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      val config = ConfigFactory.load()

      val documentDao = new DocumentRepository(configDatasource)
      val ingestionService = new DocumentService(documentDao)
      val vendorService = new VendorService(discovery, config)
      val storageService = new StorageService(config)
      val ingestionRoutes = new DocumentRoutes(ingestionService, storageService, config)
      val uploadRoutes = new UploadRoutes(ingestionService, vendorService, storageService, config)
      val apiRoutes = new IngestorAPI(ingestionRoutes, uploadRoutes)
      startHttpServer(apiRoutes.routes)
      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "IngestionServer")
  }

  private lazy val configDatasource: Database = {
    discovery.apply(POSTGRES_SERVICE_NAME).map(uri => uri.toURL) match {
      case Success(url) =>
        val dbConfigMap = Map(
          "documentdb.properties.serverName"   -> url.getHost,
          "documentdb.properties.portNumber"   -> url.getPort,
          "documentdb.properties.databaseName" -> "documentdb",
          "documentdb.properties.user"         -> "postgres",
          "documentdb.properties.password"     -> "password",
          "documentdb.dataSourceClass"         -> "org.postgresql.ds.PGSimpleDataSource",
          "documentdb.connectionPool"          -> "HikariCP")
        val dbConfig = ConfigFactory.parseMap(dbConfigMap.asJava)
        logger.info("Configured Database from Consul-discovered postgreSql instance")
        Database.forConfig("documentdb", dbConfig)
      case Failure(_) =>
        logger.info("Configuring Database from default configuration")
        Database.forConfig("documentdb")
    }
  }

}
