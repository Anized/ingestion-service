package org.anized.ingestor.service

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import org.anized.ingestor.ServiceErrors
import org.anized.ingestor.ServiceErrors._
import org.anized.ingestor.domain.VendorId
import org.anized.ingestor.routes.JsonSupport
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import spray.json.RootJsonFormat

import java.net.URI
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

case class Quota(vendorId: VendorId, maxSize: Long, totalUsed: Long)

class VendorService(discover: String => Try[URI], config: Config)(implicit system: ActorSystem[_]) extends JsonSupport {
  import system.executionContext
  private val logger = LoggerFactory.getLogger(classOf[VendorService])
  implicit val quotaFormat: RootJsonFormat[Quota] = jsonFormat(Quota, "vendor-id", "max-size", "total-used")
  private val vendorServiceConfig = config.getConfig("vendor-service")
  private val vendorServiceName = vendorServiceConfig.getString("service-name")

  def getDocumentQuotas(vendorId: VendorId): Future[Quota] = callService {
    serviceUrl: URI =>
      doRequest[Quota](docQuotasQuery.apply(serviceUrl, vendorId))
  }

  def updateQuotas(vendorId: VendorId, docSize: Long): Future[Quota] = callService {
    serviceUrl: URI =>
      doRequest[Quota](docUsageUpdate.apply(serviceUrl, vendorId, docSize)
        .withMethod(HttpMethods.PUT))
  }

  private val docUsageUpdate: (URI, VendorId, Long) => HttpRequest =
    (serviceUrl: URI, vendorId: VendorId, docSize: Long) =>
      HttpRequest(uri = Uri.parseAbsolute(
        vendorServiceConfig.getString("doc-usage-update")
          .format(serviceUrl.toASCIIString, vendorId, docSize)))

  private val docQuotasQuery: (URI, VendorId) => HttpRequest =
    (serviceUrl: URI, vendorId: VendorId) =>
      HttpRequest(uri = Uri.parseAbsolute(
        vendorServiceConfig.getString("doc-quotas")
          .format(serviceUrl.toASCIIString, vendorId)))

  private def callService[A: FromEntityUnmarshaller](action: URI => Future[A]): Future[A] =
    discover.apply(vendorServiceName) match {
      case Failure(t) =>
        logger.warn(s"discovery failed for $vendorServiceName: ${t.getMessage}")
        Future.failed(new Throwable(s"discovery failed for $vendorServiceName", t))
      case Success(vendorServiceUrl) =>
        action.apply(vendorServiceUrl)
    }

  private[this] def doRequest[A: FromEntityUnmarshaller](request: HttpRequest): Future[A] = {
    logger.info(s"calling ${request.method.value} ${request.uri}")
    Http().singleRequest(request).transformWith {
      case Success(res) if res.status == StatusCodes.OK =>
        Unmarshal(res.entity).to[A]
      case Success(res) =>
        res.discardEntityBytes()
        logger.warn(s"call failed to $vendorServiceName: status=${res.status}")
        Future.failed(ServiceErrors.mapException(res.status))
      case Failure(t) =>
        logger.warn(s"call failed to $vendorServiceName: ${t.getMessage}")
        Future.failed(InternalError(t.getMessage))
    }
  }

  /*
    val httpClient = Http().outgoingConnection(host = "api.enphaseenergy.com")

    private val flow: Future[Quota] = Source.single(HttpRequest(uri = Uri(systemSummaryUrl)))
      .via(httpClient)
      .mapAsync(1)(response => Unmarshal(response.entity).to[Quota])
      .runWith(Sink.head)
  */


}
