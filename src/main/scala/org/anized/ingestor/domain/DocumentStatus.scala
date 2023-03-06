package org.anized.ingestor.domain

object DocumentStatus extends Enumeration {
    type DocumentStatus = Value
    val NotKnown: DocumentStatus.Value = Value("NotKnown")
    val Defined: DocumentStatus.Value = Value("Defined")
    val Uploaded: DocumentStatus.Value = Value("Uploaded")
    val Deleted: DocumentStatus.Value = Value("Deleted")
  }
