package org.anized.ingestor.domain

import DocumentStatus.DocumentStatus
import org.apache.http.entity.ContentType

import java.net.URI
import java.time.LocalDateTime

final case class Document(id: Long = 0, name: String, timestamp: LocalDateTime,
                          status: DocumentStatus, version: Int = 0, size: Long = 0,
                          ownerId: VendorId, contentType: ContentType, contentUri: URI)