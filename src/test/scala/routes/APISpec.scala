package routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{MissingHeaderRejection, Route, ValidationRejection}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.anized.ingestor.domain.VendorId
import org.anized.ingestor.routes.{DocumentRoutes, IngestorAPI, JsonSupport, UploadRoutes}
import fixtures.Documents
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.springframework.web.client.HttpServerErrorException.InternalServerError

import java.io.File
import scala.concurrent.Future

class APISpec extends AnyWordSpec with Matchers with ScalaFutures
              with ScalatestRouteTest with MockitoSugar with JsonSupport {

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val documentRoute: DocumentRoutes = mock[DocumentRoutes]
  val uploadRoutes: UploadRoutes = mock[UploadRoutes]
  private lazy val apiRoutes: Route = new IngestorAPI(documentRoute,uploadRoutes).routes
  private lazy val docPath = getClass.getClassLoader.getResource("data/haskell-language.pdf").getPath

  private val uploadRequest: HttpRequest = {
    val file = new File(docPath)
    val formData = Multipart.FormData.fromFile("filename", ContentTypes.`application/octet-stream`, file, 100000)
    Post("/document/upload", formData)
      .addHeader(RawHeader("Content-type", "multipart/form-data"))
  }


  "The Ingestor API" should {

    "return list of documents" in {
      when(documentRoute.listAll).thenReturn(
        complete(StatusCodes.OK, Future.successful(Documents.documentList)))

      Get("/document/list") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should include("""name":"document.one""")
        entityAs[String] should include("""name":"document.two""")
      }
    }

    "get document info" in {
      when(documentRoute.info(1)).thenReturn(complete(StatusCodes.OK))

      Get(s"/document/id/1/info") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("OK")
      }
    }

    "describe document" in {
      when(documentRoute.describe(1)).thenReturn(complete(StatusCodes.OK))

      Get(s"/document/id/1/describe") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("OK")
      }
    }

    "delete a document" in {
      when(documentRoute.delete(1)).thenReturn(complete(StatusCodes.OK))

      Delete(s"/document/id/1") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("OK")
      }
    }

    "upload a document" in {
      when(uploadRoutes.upload(any[VendorId], any[Option[Long]])).thenReturn(complete(StatusCodes.OK))
      val request = uploadRequest.addHeader(RawHeader("vendor-id", "urn:vendor-id:test_vendor"))
      request ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }

    "fail to upload a document with invalid vendor-id header" in {
      when(uploadRoutes.upload(any[VendorId], any[Option[Long]])).thenReturn(complete(StatusCodes.OK))
      val request = uploadRequest.addHeader(RawHeader("vendor-id", "urn:vendor-id:123456"))
      request ~> apiRoutes ~> check {
        rejection shouldBe ValidationRejection("Predicate does not hold for urn:vendor-id:123456")
      }
    }

    "fail to upload a document without vendor-id header" in {
      when(uploadRoutes.upload(any[VendorId], any[Option[Long]])).thenReturn(complete(StatusCodes.OK))

      uploadRequest ~> apiRoutes ~> check {
        rejection shouldBe MissingHeaderRejection("vendor-id header missing")
      }
    }

    "fail to upload a document when an internal error occurs" in {
      when(uploadRoutes.upload(any[VendorId], any[Option[Long]]))
        .thenReturn(complete(StatusCodes.InternalServerError))
      val request = uploadRequest.addHeader(RawHeader("vendor-id", "urn:vendor-id:test_vendor"))
      request ~> apiRoutes ~> check {
        status should ===(StatusCodes.InternalServerError)
      }
    }

    "update a document" in {
      when(uploadRoutes.upload(any[VendorId], any[Option[Long]])).thenReturn(complete(StatusCodes.OK))
      val updateRequest: HttpRequest = {
        val file = new File(docPath)
        val formData = Multipart.FormData.fromFile("filename", ContentTypes.`application/octet-stream`, file, 100000)
        Put("/document/id/1", formData)
          .addHeader(RawHeader("Content-type", "multipart/form-data"))
          .addHeader(RawHeader("vendor-id", "urn:vendor-id:test_vendor"))
      }

      updateRequest ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }


    "download a document" in {
      when(documentRoute.download(1)).thenReturn(complete(StatusCodes.OK, "<file contents>"))

      Get("/document/id/1") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("<file contents>")
      }
    }

    "return error message when attempting to delete a non-existent file" in {
      when(documentRoute.delete(2)).thenReturn(complete(StatusCodes.BadRequest, s"Document id=2 not found"))

      Delete(s"/document/id/2") ~> apiRoutes ~> check {
        status should ===(StatusCodes.BadRequest)
        entityAs[String] should ===(s"Document id=2 not found")
      }
    }

    "respond to health-check" in {
      Get("/health/check") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("OK")
      }
    }

    "respond to metrics scrape" in {
      Get("/document/metrics/prometheus") ~> apiRoutes ~> check {
        status should ===(StatusCodes.OK)
      }
    }
  }
}
