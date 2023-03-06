package org.anized.ingestor.service

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Directives.getFromFile
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import org.anized.ingestor.domain.Stored
import org.anized.ingestor.routes.DocumentRoutes
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try
class StorageService(config: Config)(implicit system: ActorSystem[_]) {
  import system.executionContext
  private val logger = LoggerFactory.getLogger(classOf[DocumentRoutes])
  val dataStoragePath: String = config.getString("storage.path")

  def storeDocument(source: Source[ByteString, Any]): Future[Stored] = {
    val result: Try[Future[Stored]] = Try(Paths.get(dataStoragePath))
      .map(base => base resolve UUID.randomUUID().toString)
      .map(storePath => (source runWith (FileIO toPath storePath))
        .map(result => Stored(storePath.toUri, result.count)))
    Future.fromTry(result).flatten
  }

  def retrieveDocument(documentUri: URI): Route = {
    logger.info(s"retrieving document from $documentUri")
    getFromFile(documentUri.getPath)
  }

  def deleteDocument(documentUri: URI): Future[Boolean] = {
    logger.info(s"deleting document from $documentUri")
    Future.fromTry(Try(new File(documentUri).delete()))
  }

}
