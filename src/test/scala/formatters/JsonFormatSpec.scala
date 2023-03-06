package formatters

import org.anized.ingestor.domain.{VendorId, DocumentStatus}
import org.anized.ingestor.domain.DocumentStatus.DocumentStatus
import org.anized.ingestor.routes.JsonSupport
import org.apache.http.entity.ContentType
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

import java.net.{URI, URL}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.language.postfixOps

class JsonFormatSpec extends AnyFlatSpec with Matchers with JsonSupport {

  "A vendorId" should "be converted to Json and back" in {
    val subject: VendorId = VendorId("test_vendor")
    val jsvalue: JsValue = subject.toJson
    assert(jsvalue.toString() === "\"urn:vendor-id:test_vendor\"")
    val transformed = jsvalue.convertTo[VendorId]
    assert(transformed.id === "test_vendor")
    val thrown = intercept[Throwable] {
      new JsString("error").convertTo[VendorId]
    }
    assert(thrown.getMessage === "URN string cannot not be parsed from [error]")
    val thrown2 = intercept[Throwable] {
      vendorIdFormat.read(JsBoolean(false))
    }
    assert(thrown2.getMessage === "Unexpected type spray.json.JsFalse$ when trying to parse VendorId")
  }

  "A URI" should "be converted to Json and back" in {
    val subject: URI = URI.create("http://node")
    val jsvalue: JsValue = subject.toJson
    assert(jsvalue.toString() === "\"http://node\"")
    val transformed = jsvalue.convertTo[URI]
    assert(transformed.toString === "http://node")
    val thrown = intercept[Throwable] {
      new JsString("http://node/{id}/info").convertTo[URI]
    }
    assert(thrown.getMessage === "Illegal character in path at index 12: http://node/{id}/info")
    val thrown2 = intercept[Throwable] {
      uriFormat.read(JsBoolean(false))
    }
    assert(thrown2.getMessage === "Unexpected type spray.json.JsFalse$ when trying to parse URI")
  }

  "A DocumentStatus" should "be converted to Json and back" in {
    val subject: DocumentStatus = DocumentStatus.Uploaded
    val jsvalue: JsValue = subject.toJson
    assert(jsvalue.toString() === "\"Uploaded\"")
    val transformed = jsvalue.convertTo[DocumentStatus]
    assert(transformed.toString === "Uploaded")
    val thrown = intercept[Throwable] {
      new JsString("Evaporated").convertTo[DocumentStatus]
    }
    assert(thrown.getMessage === "No value found for 'Evaporated'")
    val thrown2 = intercept[Throwable] {
      documentStatusFormat.read(JsBoolean(false))
    }
    assert(thrown2.getMessage === "Unexpected type spray.json.JsFalse$ when trying to parse DocumentStatus")
  }

  "A ContentType" should "be converted to Json and back" in {
    val subject: ContentType = ContentType.APPLICATION_JSON
    val jsvalue: JsValue = subject.toJson
    assert(jsvalue.toString() === "\"application/json; charset=UTF-8\"")
    val transformed = jsvalue.convertTo[ContentType]
    assert(transformed.toString === "application/json; charset=UTF-8")
    val thrown = intercept[Throwable] {
      new JsString(";").convertTo[ContentType]
    }
    assert(thrown.getMessage === "Invalid content type: ;")
    val thrown2 = intercept[Throwable] {
      ctFormat.read(JsBoolean(false))
    }
    assert(thrown2.getMessage === "Unexpected type spray.json.JsFalse$ when trying to parse ContentType")
  }

  "A LocalDateTime" should "be converted to Json and back" in {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val subject: LocalDateTime = LocalDateTime.parse("2022-06-24T12:30", formatter)
    val jsvalue: JsValue = subject.toJson
    assert(jsvalue.toString() === "\"2022-06-24T12:30:00\"")
    val transformed = jsvalue.convertTo[LocalDateTime]
    assert(transformed.toString === "2022-06-24T12:30")
    val thrown = intercept[Throwable] {
      new JsString("June 19, 1865").convertTo[LocalDateTime]
    }
    assert(thrown.getMessage === "Text 'June 19, 1865' could not be parsed at index 0")
    val thrown2 = intercept[Throwable] {
      localDateTimeFormat.read(JsBoolean(false))
    }
    assert(thrown2.getMessage === "Unexpected type spray.json.JsFalse$ when trying to parse LocalDateTime")
  }

  "A URL" should "be converted to Json and back" in {
    val subject: URL = new URL("http://node")
    val jsvalue: JsValue = subject.toJson
    assert(jsvalue.toString() === "\"http://node\"")
    val transformed = jsvalue.convertTo[URL]
    assert(transformed.toString === "http://node")
    val thrown = intercept[Throwable] {
      new JsString("Evaporated").convertTo[URL]
    }
    assert(thrown.getMessage === "no protocol: Evaporated")
    val thrown2 = intercept[Throwable] {
      urlFormat.read(JsBoolean(false))
    }
    assert(thrown2.getMessage === "Unexpected type spray.json.JsFalse$ when trying to parse URL")
  }

}