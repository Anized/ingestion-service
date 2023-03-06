package org.anized.ingestor.routes

import akka.actor.typed.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import org.anized.ingestor.ServiceErrors.ServiceError
import org.anized.ingestor.domain.VendorId
import org.anized.ingestor.metrics.MetricsRegistry
import org.anized.ingestor.routes.Rejections.ProcessRejection
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success}

class IngestorAPI(documents: DocumentRoutes, uploaders: UploadRoutes)(implicit val system: ActorSystem[_])
      extends SprayJsonSupport with DefaultJsonProtocol {
  private val logger = LoggerFactory.getLogger(classOf[IngestorAPI])

  private val rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder().handle {
      case ProcessRejection(error: ServiceError) =>
        logger.warn("rejecting error={}", error)
        complete(HttpResponse(error.code, entity = error.getMessage))
      case ProcessRejection(t: Throwable) =>
        logger.warn("rejecting exception: {}", t.getMessage)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = t.getMessage))
    }.result()

  private val discardBytesOnFailure: Directive0 =
    extractRequestContext.flatMap { ctx =>
      mapRouteResult {
        case success: Complete if success.response.status.isSuccess() =>
          success
        case failure: RouteResult =>
          failure match {
            case completed: Complete =>
              logger.warn(
                s"Route failed: ${completed.response.status.value} ${completed.response.status.reason()}")
            case rejected: Rejected =>
              logger.warn(s"Route failed: ${rejected.rejections.head}")
          }
          ctx.request.discardEntityBytes()
          failure
      }
    }

  private val listRoute: Route = cors() {
    path("list") {
      Directives.get {
        documents.listAll
      }
    }
  }

  private val uploadPath: Route =
    withoutSizeLimit {
      extractRequestContext { _ =>
        discardBytesOnFailure {
          optionalHeaderValueByName("vendor-id") {
            case Some(vendorId) =>
              validateVendorId(vendorId) { aid =>
                uploaders.upload(aid)
              }
            case None =>
              reject(MissingHeaderRejection("vendor-id header missing"))
          }
        }
      }
    }

  private val uploadRoute: Route = cors() {
    path("upload") {
      post {
        uploadPath
      }
    }
  }

  private val idRoute: Route =
    pathPrefix("id" / LongNumber) { id =>
      concat(
        (path("describe") & get) {
          documents.describe(id)
        },
        (path("info") & get) {
          documents.info(id)
        },
        put {
          uploadPath
        },
        get {
          documents.download(id)
        },
        delete {
          documents.delete(id)
        })
    }

  private def validateVendorId(vendorId: String)(action: VendorId => Route): Route =
    VendorId.parseUrn(vendorId) match {
      case Success(aid) => action(aid)
      case Failure(t) =>
        reject(ValidationRejection(t.getMessage))
    }

  private val healthCheck: Route =
    path("check") {
      get {
        complete("OK")
      }
    }

  private val metrics: Route =
    pathPrefix("metrics") {
      path("prometheus") {
        get {
          complete(StatusCodes.OK, MetricsRegistry.getPrometheus
            .map(_.scrape()).getOrElse(""))
        }
      }
    }

  val routes: Route = logRequestResult("Request-Response", Logging.DebugLevel)({
    val documentRoutes = pathPrefix("document") {
      handleRejections(rejectionHandler) {
        concat(listRoute, uploadRoute, idRoute, metrics)
      }
    }
    val healthRoutes = pathPrefix("health") {
      healthCheck
    }
    concat(documentRoutes, healthRoutes)
  } )
}
