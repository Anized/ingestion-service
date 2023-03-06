package org.anized.ingestor.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.anized.ingestor.domain.{VendorId, Document, DocumentStatus}
import org.anized.ingestor.domain.DocumentStatus.DocumentStatus
import org.apache.http.entity.ContentType
import spray.json.{DefaultJsonProtocol, JsArray, JsString, JsValue, JsonFormat, RootJsonFormat}

import java.net.{URI, URL}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val urlFormat: JsonFormat[URL] = new JsonFormat[URL] {
    def write(x: URL): JsString = JsString(x.toString)
    def read(value: JsValue): URL = value match {
      case JsString(x) => new URL(x)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse URL")
    }
  }
  implicit val localDateTimeFormat: JsonFormat[LocalDateTime] = new JsonFormat[LocalDateTime] {
    private val iso_date_time = DateTimeFormatter.ISO_DATE_TIME
    def write(x: LocalDateTime): JsString = JsString(iso_date_time.format(x))
    def read(value: JsValue): LocalDateTime = value match {
      case JsString(x) => LocalDateTime.parse(x, iso_date_time)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
    }
  }
  implicit val ctFormat: JsonFormat[ContentType] = new JsonFormat[ContentType] {
    def write(x: ContentType): JsString = JsString(x.toString)
    def read(value: JsValue): ContentType = value match {
      case JsString(x) => ContentType.parse(x)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse ContentType")
    }
  }
  implicit val documentStatusFormat: JsonFormat[DocumentStatus] = new JsonFormat[DocumentStatus.DocumentStatus] {
    def write(x: DocumentStatus.DocumentStatus): JsString = JsString(x.toString)
    def read(value: JsValue): DocumentStatus = value match {
      case JsString(x) => DocumentStatus.withName(x)
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse DocumentStatus")
    }
  }
  implicit val vendorIdFormat: JsonFormat[VendorId] = new JsonFormat[VendorId] {
    def write(x: VendorId): JsString = JsString(x.toString)

    def read(value: JsValue): VendorId = value match {
      case JsString(x) => VendorId.parseUrn(x).get
      case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse VendorId")
    }
  }

    implicit val uriFormat: JsonFormat[URI] = new JsonFormat[URI] {
      def write(x: URI): JsString = JsString(x.toString)

      def read(value: JsValue): URI = value match {
        case JsString(x) => URI.create(x)
        case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse URI")
      }
    }

  implicit val documentFormat: RootJsonFormat[Document] = jsonFormat9(Document)
}