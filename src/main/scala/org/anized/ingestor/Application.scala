package org.anized.ingestor

import org.springframework.boot.SpringApplication

object Application extends App {
  SpringApplication run classOf[IngestionServer]
  Thread.sleep(2000)
}
