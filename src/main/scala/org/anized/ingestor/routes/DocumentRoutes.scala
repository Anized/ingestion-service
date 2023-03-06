package org.anized.ingestor.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete, reject}
import akka.http.scaladsl.server.Route
import akka.stream.IOResult
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.anized.ingestor.ServiceErrors.NotFound
import org.anized.ingestor.common.Formats.humanReadableSize
import org.anized.ingestor.common.TaggedMetrics._
import org.anized.ingestor.domain.Document
import org.anized.ingestor.routes.Rejections.ProcessRejection
import org.anized.ingestor.service.{DocumentService, StorageService}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.net.URI
import scala.concurrent.Future
import scala.util.{Failure, Success}

class DocumentRoutes(documentService: DocumentService, storageService: StorageService, config: Config)
                    (implicit system: ActorSystem[_]) extends JsonSupport {
  import system.executionContext
  case class DocumentUpdate(id: Long, owner: String, data: Multipart.FormData)
  private val logger = LoggerFactory.getLogger(classOf[DocumentRoutes])
  private val contentUrlTemplate: String = config.getString("document.urlTemplate")
  private val contentUrl = (documentId: Long) => URI.create(String.format(contentUrlTemplate, documentId))

  private val findById = Flow[Long].mapAsync(1) { documentId =>
    documentService.findById(documentId) flatMap {
      case Some(document) =>
        Future.successful(document)
      case None =>
        Future.failed(NotFound("document not found"))
    }
  }

  private val delete = Flow[Document].mapAsync(1) { doc =>
    storageService.deleteDocument(doc.contentUri).flatMap(_ =>
      documentService.deleteDocument(doc.id).map(count => IOResult.apply(count)))
  }

  private val getData = Flow[Document] map { document =>
    storageService.retrieveDocument(document.contentUri)
  }

  private def returnDocument(document: Document) =
    complete(document.copy(contentUri = contentUrl(document.id)))

  def listAll: Route = {
    val result = documentService.listDocuments
      .map(docs => docs.map(doc => doc.copy(contentUri = contentUrl(doc.id))))
    complete(result)
  }

  def delete(documentId: Long): Route = {
    counter("document.delete.counter", Seq("id", documentId.toString)).increment()
    val deleteResult = Source.single(documentId) via findById via delete runWith Sink.head
    onComplete(deleteResult) {
      case Success(_) =>
        complete(StatusCodes.OK, s"Deleted document with id=$documentId")
      case _ =>
        reject(ProcessRejection(NotFound(s"Document id=$documentId not found")))
    }
  }

  def describe(documentId: Long): Route = {
    val result = Source.single(documentId) via findById runWith Sink.head
    onComplete(result) {
      case Success(document) => returnDocument(document)
      case _ =>
        reject(ProcessRejection(NotFound(s"Document id=$documentId not found")))
    }
  }

  def info(documentId: Long): Route = {
    val result = Source.single(documentId) via findById runWith Sink.head
    onComplete(result) {
      case Success(document) =>
        complete(StatusCodes.OK,
          s"Document id=${document.id} name=${document.name} size=${humanReadableSize(document.size)}")
      case _ =>
        reject(ProcessRejection(NotFound(s"Document id=$documentId not found")))
    }
  }

  def download(documentId: Long): Route = {
    val meta = Seq("id", documentId.toString)
    logger.info(s"download: $meta")
    timer("document.download.timer", meta).record(() => {
      val downloadRoute = Source.single(documentId) via findById via getData runWith Sink.head
      onComplete(downloadRoute) {
        case Success(value) =>
          value
        case Failure(t) =>
          reject(ProcessRejection(t))
      }
    })
  }
}
