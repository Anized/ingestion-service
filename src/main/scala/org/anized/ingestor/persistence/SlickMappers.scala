package org.anized.ingestor.persistence

import org.anized.ingestor.domain.{VendorId, DocumentStatus}
import org.anized.ingestor.domain.DocumentStatus.DocumentStatus
import org.apache.http.entity.ContentType
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

import java.net.URI


object SlickMappers {
  implicit val DocumentStatusColumnType: JdbcType[DocumentStatus] with BaseTypedType[DocumentStatus] =
    MappedColumnType.base[DocumentStatus, String](
      status => status.toString, value => DocumentStatus.withName(value))

  implicit val ContentTypeColumnType: JdbcType[ContentType] with BaseTypedType[ContentType] =
    MappedColumnType.base[ContentType, String](
      contentType => contentType.toString, value => ContentType.parse(value))

  implicit val VendorIdColumnType: JdbcType[VendorId] with BaseTypedType[VendorId] =
    MappedColumnType.base[VendorId, String](
      contentType => contentType.toString, value => VendorId.parseUrn(value).get)

  implicit val URIColumnType: JdbcType[URI] with BaseTypedType[URI] =
    MappedColumnType.base[URI, String](
      contentType => contentType.toASCIIString, value => URI.create(value))
}