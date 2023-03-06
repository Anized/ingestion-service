package routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.anized.ingestor.ServiceErrors.NotFound
import org.anized.ingestor.routes.DocumentRoutes
import org.anized.ingestor.routes.Rejections.ProcessRejection
import org.anized.ingestor.service.{DocumentService, StorageService}
import com.typesafe.config.Config
import fixtures.Documents
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{mock, when}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class DocumentRouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  private val testDoc1 = Documents.testDocument.copy(name = "document.one", id = 1, size = 12345667)
  private val testDoc2 = Documents.testDocument.copy(name = "document.two", id = 2, size = 64)

  val storageService: StorageService = mock[StorageService]
  when(storageService.retrieveDocument(any))
    .thenReturn(getFromResource("data/haskell-language.pdf"))
  when(storageService.deleteDocument(any))
    .thenReturn(Future.successful(true))
  val config: Config = mock[Config]
  when(config.getString("document.urlTemplate"))
    .thenReturn("http://localhost/document/id/%d")
  val documentService: DocumentService = mock[DocumentService]
  when(documentService.listDocuments)
    .thenReturn(Future.successful(Seq(testDoc1, testDoc2)))
  when(documentService.findById(any[Long]))
    .thenAnswer((context: InvocationOnMock) => {
      val args = context.getArguments
      Future.successful(args(0).asInstanceOf[Long] match {
        case 1 => Some(testDoc1)
        case 2 => Some(testDoc2)
        case _ => None
      })
    })
  when(documentService.deleteDocument(1))
    .thenReturn(Future.successful(1))

  lazy val documentRoutes: DocumentRoutes = new DocumentRoutes(documentService, storageService, config)

  "The document route" should {
    "answer with a document list for GET /list" in {
      Get("/list") ~> documentRoutes.listAll ~> check {
        response.status shouldEqual StatusCodes.OK
      }
    }

    "answer with document metadata for GET /describe/1" in {
      Get("/id/1/describe") ~> documentRoutes.describe(1) ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "{\"contentType\":\"*/*\",\"contentUri\":\"http://localhost/document/id/1\"," +
          "\"id\":1,\"name\":\"document.one\",\"ownerId\":\"urn:vendor-id:tester\",\"size\":12345667," +
          "\"status\":\"Defined\",\"timestamp\":\"2022-05-25T12:30:00\",\"version\":0}"
      }
    }

    "answer with 404 for GET /describe with non-existing id" in {
      Get("/id/10/describe") ~> documentRoutes.describe(10) ~> check {
        rejection shouldEqual ProcessRejection(NotFound("Document id=10 not found"))
      }
    }

    "answer with short info for GET /id/1/info" in {
      Get("/id/1/info") ~> documentRoutes.info(1) ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Document id=1 name=document.one size=11.8 MiB"
      }
    }

    "fail to answer info for unknown document" in {
      Get("/id/10/info") ~> documentRoutes.info(10) ~> check {
        rejection shouldEqual ProcessRejection(NotFound("Document id=10 not found"))
      }
    }

    "answer with document's data for GET /id/1" in {
      Get("/id/1") ~> documentRoutes.download(1) ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Test Data File\n"
      }
    }

    "fail answer with document's data for unknown document" in {
      Get("/id/10") ~> documentRoutes.download(10) ~> check {
        rejection shouldEqual ProcessRejection(NotFound("document not found"))
      }
    }

    "delete document metadata for DELETE /id/1" in {
      Delete("/id/1") ~> documentRoutes.delete(1) ~> check {
        response.status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Deleted document with id=1"
      }
    }

    "delete document metadata for DELETE /id/10 should fail" in {
      Delete("/id/10") ~> documentRoutes.delete(10) ~> check {
        rejection shouldEqual ProcessRejection(NotFound("Document id=10 not found"))
      }
    }
  }

}
