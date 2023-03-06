package routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, Multipart, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.anized.ingestor.ServiceErrors.BadRequest
import org.anized.ingestor.domain.{VendorId, Document, Stored}
import org.anized.ingestor.routes.Rejections.ProcessRejection
import org.anized.ingestor.routes.UploadRoutes
import org.anized.ingestor.service.{VendorService, DocumentService, Quota, StorageService}
import com.typesafe.config.Config
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{mock, when}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.net.URI
import scala.concurrent.Future

class UploadRouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem
  val unknownVendorError = new Throwable("unknown vendor")

  private val vendorId = VendorId.parseUrn("urn:vendor-id:test_vendor").get
  private val unknownVendorId = VendorId.parseUrn("urn:vendor-id:unknown").get
  private val noQuotaVendorId = VendorId.parseUrn("urn:vendor-id:quotaless").get
  val vendorService: VendorService = mock[VendorService]
  when(vendorService.getDocumentQuotas(any[VendorId]))
    .thenAnswer ((context: InvocationOnMock) => {
      val args = context.getArguments
      args(0).asInstanceOf[VendorId] match {
        case a: VendorId if a.id == "test_vendor" =>
          Future.successful(Quota(vendorId,10000,0))
        case a: VendorId if a.id == "quotaless" =>
          Future.successful(Quota(noQuotaVendorId,0,0))
        case _ =>
          Future.failed(unknownVendorError)
      }
    })
  when(vendorService.updateQuotas(any[VendorId], any[Long]))
    .thenAnswer ((context: InvocationOnMock) => {
      val args = context.getArguments
      Future.successful(
        Quota(args(0).asInstanceOf[VendorId],10000,args(1).asInstanceOf[Long]))
    })
  val storageService: StorageService = mock[StorageService]
  when(storageService.storeDocument(any))
    .thenReturn(Future.successful(Stored(URI.create("file:///tmp/abcdef-123456"), 120)))
  when(storageService.deleteDocument(any))
    .thenReturn(Future.successful(true))
  val config: Config = mock[Config]
  when(config.getString("document.urlTemplate"))
    .thenReturn("http://localhost/document/id/%d")
  val documentService: DocumentService = mock[DocumentService]
  when(documentService.storeDocumentMetadata(any))
    .thenAnswer ((context: InvocationOnMock) => {
      val args = context.getArguments
      val document = args(0).asInstanceOf[Document]
      Future.successful(document)
    })

  private val uploadRequest: HttpRequest = {
    val path = getClass.getClassLoader.getResource("data/haskell-language.pdf").getPath
    val file = new File(path)
    val formData = Multipart.FormData.fromFile("filename", ContentTypes.`application/octet-stream`, file, 100000)
    Post("/upload", formData).addHeader(RawHeader("Content-type", "multipart/form-data"))
  }

  lazy val uploadRoutes: UploadRoutes = new UploadRoutes(documentService,vendorService,storageService,config)

  "The upload route" should {
    "upload a documented POST'd to the API" in {
      uploadRequest ~> uploadRoutes.upload(vendorId, None) ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }
  }

  "The upload route" should {
    "fail when the vendor is not known" in {
      uploadRequest ~> uploadRoutes.upload(unknownVendorId, None) ~> check {
        rejection shouldEqual ProcessRejection(unknownVendorError)
      }
    }
  }

  "The upload route" should {
    "fail when the ~> does not have storage quota remaining" in {
      uploadRequest ~> uploadRoutes.upload(noQuotaVendorId, None) ~> check {
        rejection shouldEqual ProcessRejection(BadRequest("quotas exceeded for urn:vendor-id:quotaless max=0"))
      }
    }
  }

}
