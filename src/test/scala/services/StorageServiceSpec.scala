package services

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.anized.ingestor.service.StorageService
import com.typesafe.config.Config
import fixtures.Documents
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.net.URI
import java.nio.file.Files
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class StorageServiceSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterAll with ScalatestRouteTest {
  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  lazy val storePath: File = Files.createTempDirectory("store").toFile
  val config: Config = mock[Config]
  when(config.getString("storage.path")).thenReturn(storePath.getPath)
  val storageService = new StorageService(config)
  val data: Source[ByteString, Future[IOResult]] =
    Documents.streamDocument("data/haskell-language.pdf")

  "A document" should "be stored and deleted" in {
    storageService.storeDocument(data) flatMap { doc =>
      assert(doc.size === 15)
      storageService.deleteDocument(doc.uri) map { success =>
        assert(success === true)
      }
    }
  }

/*
  "An empty document" should "not be stored" in {
    storageService.storeDocument(null) map { _ =>
      fail("document should not be stored")
    }
  }
*/

  "A non-existent document" should "not be deleted" in {
    val uri = URI.create("file:///tmp/store1703070674057240860/f6670972-eaea-474f-8448-2641e18d0eae")
    storageService.deleteDocument(uri) map { result =>
      assert(result === false)
    }
  }

  "A stored document" should "be retrieved" in {
    val stored = Await.result(storageService.storeDocument(data), 500 milliseconds)
    Get("/") ~> storageService.retrieveDocument(stored.uri) ~> check {
      responseAs[String] shouldEqual "Test Data File\n"
    }
  }


  override def afterAll(): Unit = storePath.deleteOnExit()

}