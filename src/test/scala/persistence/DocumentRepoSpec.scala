package persistence

import com.dimafeng.testcontainers.ForAllTestContainer
import fixtures.{DatabaseFixtures, Documents}
import org.anized.ingestor.common.URN
import org.anized.ingestor.domain.{VendorId, DocumentStatus}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.language.postfixOps

class DocumentRepoSpec
    extends AsyncFlatSpec with Matchers with ForAllTestContainer with BeforeAndAfterEach with DatabaseFixtures {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "A document's metadata" should "be stored" in {
    val vendorId = URN.create("vendor-id", "tester").get
    Await.ready(documentRepo.upsert(Documents.testDocument), 2 seconds)
    documentRepo.listAll map { documents =>
      assert(documents.size === 1)
      val doc = documents.head
      assert(doc.name === "test.doc")
      assert(doc.status === DocumentStatus.Defined)
      assert(doc.ownerId === VendorId.fromUrn(vendorId).get)
      assert(doc.version === 0)
    }
  }

  "When a document is updated, the version#" should "be incremented" in {
    val initial = Documents.testDocument
    val inserted = Await.result(documentRepo.upsert(initial), 2 seconds)
    assert(inserted.name === "test.doc")
    assert(inserted.version === 0)
    documentRepo.upsert(inserted.copy(timestamp = LocalDateTime.now)) map { doc =>
      assert(doc.name === "test.doc")
      assert(doc.version === 1)
    }
  }

  "Document metadata" should "be recorded" in {
    documentRepo.upsert(Documents.testDocument) map { doc =>
      assert(doc.name === "test.doc")
    }
  }

  "Document metadata" should "be found by ID" in {
    documentRepo.upsert(Documents.testDocument) flatMap { doc =>
      assert(doc.name === "test.doc")
      documentRepo.findById(doc.id) map { found =>
        assert(found.get.name === "test.doc")
      }
    }
  }

  "Document metadata" should "be found by name" in {
    documentRepo.upsert(Documents.testDocument) flatMap { doc =>
      assert(doc.name === "test.doc")
      documentRepo.findByName(doc.name) map { found =>
        assert(found.head.name === "test.doc")
      }
    }
  }

  "Document status" should "be updated" in {
    documentRepo.upsert(Documents.testDocument) flatMap { doc =>
      assert(doc.name === "test.doc")
      documentRepo.updateStatus(doc.id, DocumentStatus.Uploaded) map { updated =>
        assert(updated === 1)
      }
    }
  }

  "Document metadata" should "be deleted by ID" in {
    documentRepo.upsert(Documents.testDocument) flatMap { doc =>
      assert(doc.name === "test.doc")
      documentRepo.deleteById(doc.id) map { deleted =>
        assert(deleted === 1)
      }
    }
  }

  override def beforeEach(): Unit = Await.result(documentRepo.purgeDocuments, 2 seconds)

  override def afterStart(): Unit = {
    executeScript(container,
      Source.fromResource(documentSchemaDDL).getLines.mkString("\n"))
  }

  override def beforeStop(): Unit = container.stop()

}