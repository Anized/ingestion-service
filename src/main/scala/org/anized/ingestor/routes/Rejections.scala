package org.anized.ingestor.routes

import akka.http.javadsl.server.CustomRejection

object Rejections {
  final case class ProcessRejection(cause: Throwable) extends CustomRejection
}
