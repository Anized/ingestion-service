package org.anized.ingestor.persistence

import akka.actor.typed.ActorSystem
import org.anized.ingestor.domain.DocumentStatus.DocumentStatus
import org.anized.ingestor.domain.{VendorId, Document, DocumentStatus}
import org.anized.ingestor.persistence.SlickMappers._
import org.apache.http.entity.ContentType
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}

import java.net.URI
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DocumentRepository(db: Database)(implicit system: ActorSystem[_]) {

  class DocumentTable(tag: Tag) extends Table[Document](tag, "documents") {
    def id          = column[Long]           ("id", O.AutoInc, O.PrimaryKey)
    def name        = column[String]         ("name")
    def timestamp   = column[LocalDateTime]  ("timestamp")
    def status      = column[DocumentStatus] ("status")
    def version     = column[Int]            ("version")
    def size        = column[Long]           ("size")
    def ownerId     = column[VendorId]       ("owner_id")
    def contentType = column[ContentType]    ("content_type")
    def contentUri  = column[URI]            ("content_uri")

    def * = (id, name, timestamp, status, version, size, ownerId, contentType, contentUri) <>
      (Document.tupled, Document.unapply)
  }

  val documents = TableQuery[DocumentTable]

  private def filterQuery(id: Long) =
    documents
      .filter(_.status =!= DocumentStatus.Deleted)
      .filter(_.id === id)

  private def insert(document: Document): Future[Document] =
    db.run(
      documents returning documents.map(_.id) into ((doc, id) =>
        doc.copy(id = id)) += document)

  def upsert(document: Document): Future[Document] =
    db.run(filterQuery(document.id).result.headOption) flatMap {
      case Some(existing) =>
        val updated = document.copy(version = existing.version + 1)
        db.run(documents.insertOrUpdate(updated)) map { _ => updated }
      case None =>
        insert(document.copy(version = 0))
    }

  def updateStatus(documentId: Long, status: DocumentStatus): Future[Int] = {
    val action =
      for { document <- documents if document.id === documentId} yield document.status
    db.run(action.update(status).transactionally)
  }

  def listAll: Future[Seq[Document]] =
    db.run(documents.filter(_.status =!= DocumentStatus.Deleted).result)

  def findById(documentId: Long): Future[Option[Document]] =
    db.run(filterQuery(documentId).result.headOption)

  def findByName(name: String): Future[Seq[Document]] = {
    val q = for {d <- documents if d.name === name} yield d
    db.run(q.result)
  }

  def deleteById(id: Long): Future[Int] = db.run(filterQuery(id).delete)

  def purgeDocuments: Future[Int] = db.run(documents.delete)

}