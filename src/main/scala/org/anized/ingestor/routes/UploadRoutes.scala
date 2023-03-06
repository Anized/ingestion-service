package org.anized.ingestor.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Directives.{complete, fileUpload, onComplete, onSuccess, reject}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.anized.ingestor.ServiceErrors.BadRequest
import org.anized.ingestor.common.TaggedMetrics._
import org.anized.ingestor.common.{Formats, TaggedMetrics}
import org.anized.ingestor.domain.{VendorId, Document, DocumentStatus, Stored}
import org.anized.ingestor.routes.Rejections.ProcessRejection
import org.anized.ingestor.service.{VendorService, DocumentService, StorageService}
import com.typesafe.config.Config
import org.apache.http.entity.ContentType
import org.slf4j.LoggerFactory

import java.net.URI
import java.time.LocalDateTime
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

class UploadRoutes(documentService: DocumentService, vendorService: VendorService,
                   storageService: StorageService, config: Config)
                  (implicit system: ActorSystem[_]) extends JsonSupport {
  import system.executionContext
  private val logger = LoggerFactory.getLogger(classOf[UploadRoutes])
  val contentUrl: String = config.getString("document.urlTemplate")
  case class StoreSpec(vendorId: VendorId, id: Option[Long], metadata: FileInfo, stored: Stored)

  private val storeMeta = Flow[StoreSpec].mapAsync(1) { storeSpec =>
    vendorService.getDocumentQuotas(storeSpec.vendorId) flatMap {
      case q if q.maxSize > 0 =>
        storeMetadata(storeSpec)
      case q =>
        storageService.deleteDocument(storeSpec.stored.uri).flatMap(_ =>
          Future.failed(BadRequest(s"quotas exceeded for ${storeSpec.vendorId} max=${q.maxSize}")))
    }
  }

  private val updateQuotas = Flow[Document].mapAsync(1) { document =>
    vendorService.updateQuotas(document.ownerId, document.size).map(_ => document)
  }

  private def storeMetadata(storeSpec: StoreSpec): Future[Document] =
    documentService.storeDocumentMetadata(
      Document(id = storeSpec.id.getOrElse(0), name = storeSpec.metadata.fileName, ownerId = storeSpec.vendorId,
        timestamp = LocalDateTime.now(), status = DocumentStatus.Uploaded,
        contentType = ContentType.parse(storeSpec.metadata.contentType.toString),
        size = storeSpec.stored.size, contentUri = storeSpec.stored.uri))
      .map(doc => {
        val tags = Seq("id", doc.id.toString, "filename", doc.name)
        logger.info("document uploaded:{} size={}", TaggedMetrics.asString(tags), doc.size)
        gauge("document.upload.size", doc.size, tags)
        counter("document.upload.counter", tags).increment()
        doc
      })

  private def uploadComplete(result: Future[Document]): Route =
    onComplete(result) {
      case Success(document) =>
        logger.info("uploaded document {} size {} successfully",
          document.name, Formats.humanReadableSize(document.size))
        complete(document.copy(contentUri = URI.create(contentUrl.format(document.id))))
      case Failure(t) =>
        logger.warn("failed to upload document: {}", t.getMessage)
        Option(t.getCause).foreach(cause =>
          logger.info("failed to upload document: cause= {}", cause.getMessage))
        reject(ProcessRejection(t))
    }

  def upload(vendorId: VendorId, id: Option[Long] = None): Route =
    fileUpload("filename") {
      case (metadata, byteSource) =>
        val tags = Seq("filename", metadata.fileName, "id",
          id.getOrElse(0).toString, "vendor", vendorId.toString)
        logger.info("document upload requested:{}", TaggedMetrics.asString(tags))
        timer("document.upload.timer", tags).record(() => {
          onSuccess(storageService.storeDocument(byteSource)) { stored =>
            val src = Source.single(StoreSpec(vendorId, id, metadata, stored))
            val result = src via storeMeta via updateQuotas runWith Sink.head
            uploadComplete(result)
          }
        })
    }

}
