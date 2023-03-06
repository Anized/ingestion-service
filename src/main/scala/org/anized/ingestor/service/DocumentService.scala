package org.anized.ingestor.service

import org.anized.ingestor.domain.{Document, DocumentStatus}
import org.anized.ingestor.persistence.DocumentRepository

import scala.concurrent.Future

class DocumentService(documentRepo: DocumentRepository) {
  def storeDocumentMetadata(document: Document): Future[Document] = documentRepo.upsert(document)
  def listDocuments: Future[Seq[Document]] = documentRepo.listAll
  def findByName(name: String): Future[Seq[Document]] = documentRepo.findByName(name)
  def findById(documentId: Long): Future[Option[Document]] = documentRepo.findById(documentId)
  def deleteDocument(id: Long): Future[Int] = documentRepo.updateStatus(id, DocumentStatus.Deleted)
}
