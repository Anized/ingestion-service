package org.anized.ingestor.common

import scala.annotation.tailrec

object Formats {

  def humanReadableSize(bytes: Long): String = {
    val base = 1024
    val labels = Vector("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB")

    @tailrec
    def getExponent(curBytes: Long, baseValue: Int, curExponent: Int = 0): Int =
      curBytes < baseValue match {
        case true => curExponent
        case _ =>
          val newExponent = 1 + curExponent
          getExponent(curBytes / (baseValue * newExponent), baseValue, newExponent)
      }

    val exponent = getExponent(bytes, base)
    f"${bytes /  Math.pow(base, exponent)}%.1f ${labels(exponent)}"
  }
}
