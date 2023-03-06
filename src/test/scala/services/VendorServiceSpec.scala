package services

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import org.anized.ingestor.domain.VendorId
import org.anized.ingestor.service.VendorService
import com.typesafe.config.Config
import fixtures.{Response, TestHttpServer}
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.{HttpURLConnection, URI}
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class VendorServiceSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  var testServer: Option[TestHttpServer] = None
  val discover: String => Try[URI] = (n: String) => testServer.map(_.httpBase) match {
    case Some(uri) => Success(uri)
    case None => Failure(new Throwable(s"unable to discover $n from test server"))
  }
  val vendorId: VendorId = VendorId.parseUrn("urn:vendor-id:test_vendor").get
  val serviceConfig: Config = mock[Config]
  when(serviceConfig.getString("service-name")).thenReturn("test-service")
  when(serviceConfig.getString("doc-quotas")).thenReturn("%s/vendor/%s/document/quotas")
  when(serviceConfig.getString("doc-usage-update")).thenReturn("%s/vendor/%s/document/used-bytes/%d")
  val config: Config = mock[Config]
  when(config.getConfig("vendor-service")).thenReturn(serviceConfig)
  val vendorService = new VendorService(discover, config)

  "An vendor's quota" should "be successfully queried" in {
    vendorService.getDocumentQuotas(vendorId) map  { quota =>
      assert(quota.maxSize === 1500)
    }
  }

  "An unknown vendor's quota" should "not be returned" in {
    val unknownVendorId: VendorId = VendorId.parseUrn("urn:vendor-id:pinellas").get
    vendorService.getDocumentQuotas(unknownVendorId).transformWith {
      case Success(_) =>
        fail("query expected to fail")
      case Failure(t) =>
        assert(t.getMessage === "404 Not Found")
    }
  }

  "A bad request... " should "not be returned" in {
    val unknownVendorId: VendorId = VendorId.parseUrn("urn:vendor-id:brookhaven").get
    vendorService.getDocumentQuotas(unknownVendorId).transformWith {
      case Success(_) =>
        fail("query expected to fail")
      case Failure(t) =>
        assert(t.getMessage === "400 Bad Request")
    }
  }

  "An internal error..." should "not be returned" in {
    val unknownVendorId: VendorId = VendorId.parseUrn("urn:vendor-id:wicklow").get
    vendorService.getDocumentQuotas(unknownVendorId).transformWith {
      case Success(_) =>
        fail("query expected to fail")
      case Failure(t) =>
        assert(t.getMessage === "500 Internal Server Error")
    }
  }

  "An vendor's quota" should "not be returned if the service URI is invalid" in {
    val subject = new VendorService(_ => Success(URI.create("http://node")), config)
    subject.getDocumentQuotas(vendorId).transformWith {
      case Success(_) =>
        fail("query expected to fail")
      case Failure(t) =>
        t.getMessage should include("java.net.UnknownHostException: node")
    }
  }

  "An vendor's quota" should "be updated" in {
    vendorService.updateQuotas(vendorId, 500) map  { quota =>
      assert(quota.maxSize === 2000)
    }
  }

  "An vendor's quota" should "not be queried if the service is not found" in {
    val subject = new VendorService(
      n => Failure(new Throwable(s"unable to discover $n from test server")), config)
    subject.getDocumentQuotas(vendorId).transformWith {
      case Success(_) =>
        fail("service look-up expected to fail")
      case Failure(t) =>
        assert(t.getMessage === "discovery failed for test-service")
    }
  }

  override def beforeAll(): Unit = {
    val server = new TestHttpServer(Seq(
      Response("GET", "/vendor/urn:vendor-id:test_vendor/document/quotas",
        Some( "{\"vendor-id\" : \"urn:vendor-id:test_vendor\", \"max-size\" : 1500, \"total-used\" : 0}"),
          HttpURLConnection.HTTP_OK, "application/json"),
      Response("GET", "/vendor/urn:vendor-id:pinellas/document/quotas",
        None, HttpURLConnection.HTTP_NOT_FOUND, "application/json"),
      Response("GET", "/vendor/urn:vendor-id:brookhaven/document/quotas",
        None, HttpURLConnection.HTTP_BAD_REQUEST, "application/json"),
      Response("GET", "/vendor/urn:vendor-id:wicklow/document/quotas",
        None, HttpURLConnection.HTTP_INTERNAL_ERROR, "application/json"),
      Response("PUT", "/vendor/urn:vendor-id:test_vendor/document/used-bytes/500",
        Some( "{\"vendor-id\" : \"urn:vendor-id:test_vendor\", \"max-size\" : 2000, \"total-used\" : 500}"),
        HttpURLConnection.HTTP_OK, "application/json")))
    server.start()
    testServer = Some(server)
  }

  override def afterAll(): Unit = testServer.foreach(s => s.shutdown())

}