package org.anized.ingestor.domain

import StorageProvider.{StorageProvider, FS}

import java.net.URI

object StorageProvider extends Enumeration {
  type StorageProvider = Value

  val FS: StorageProvider.Value = Value
}

case class Stored(uri: URI, size: Long, storageProvider: StorageProvider = FS)