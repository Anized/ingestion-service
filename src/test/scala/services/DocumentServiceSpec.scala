package services

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import org.anized.ingestor.service.DocumentService
import com.dimafeng.testcontainers.ForAllTestContainer
import fixtures.{DatabaseFixtures, Documents}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.language.postfixOps

class DocumentServiceSpec
    extends AsyncFlatSpec with Matchers with ForAllTestContainer with BeforeAndAfterEach with DatabaseFixtures {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system
  private val document = Documents.testDocument.copy(
    contentUri = URI.create("/data"), name = "haskell-language.pdf", contentType = Documents.pdfContent)
  var documentService: DocumentService = _

  "Document metadata" should "be stored" in {
    documentService.storeDocumentMetadata(document) map { doc =>
      assert(doc.name === "haskell-language.pdf")
    }
  }

  "Document metadata" should "be found by ID" in {
    documentService.storeDocumentMetadata(document) flatMap { inserted =>
      documentService.findById(inserted.id) map {
        case Some(doc) =>
          assert(doc.name === "haskell-language.pdf")
        case None =>
          fail("document not found")
      }
    }
  }

  "Document metadata" should "be found by name" in {
    documentService.storeDocumentMetadata(document) flatMap { inserted =>
      documentService.findByName(inserted.name) map {
        case Seq(doc) =>
          assert(doc.name === "haskell-language.pdf")
        case _ =>
          fail("document not found")
      }
    }
  }

  "Document metadata" should "be listed for all" in {
    documentService.storeDocumentMetadata(document) flatMap { inserted =>
      documentService.listDocuments map { docs =>
          assert(docs.size === 1)
      }
    }
  }

  "Document metadata" should "be deleted" in {
    documentService.storeDocumentMetadata(document) flatMap { inserted =>
      documentService.deleteDocument(inserted.id) map { count =>
        assert(count === 1)
      }
    }
  }

  override def beforeEach(): Unit = Await.result(documentRepo.purgeDocuments, 2 seconds)

  override def afterStart(): Unit = {
    documentService = new DocumentService(documentRepo)
    executeScript(container,
      Source.fromResource(documentSchemaDDL).getLines.mkString("\n"))
  }

  override def beforeStop(): Unit = container.stop()
}