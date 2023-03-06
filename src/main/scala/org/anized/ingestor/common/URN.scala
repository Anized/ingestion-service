package org.anized.ingestor.common

import java.util.Objects
import scala.util.{Failure, Success, Try}

object URN {
  private lazy val RFC2141 = "^\\s*urn:([a-z0-9][a-z0-9_-]+):([a-z0-9:_-]+)\\s*$".r
  private val concat = (nss: String, nid: String) => "urn:" + nss + ":" + nid

  def parse(value: String): Try[URN] = parseUrn(value)
  def create(nss: String, nid: String): Try[URN] = parse(concat(nss, nid))

  private val parseUrn = (value: String) => {
    value.toLowerCase match {
      case RFC2141(nss,nid) => Success(new URN(nss,nid))
      case _ => Failure(new Throwable(s"URN string cannot not be parsed from [$value]"))
    }
  }
}

class URN (val namespace: String, val identity: String) {
  override def toString: String = URN.concat(namespace, identity)

  def canEqual(other: Any): Boolean = other.isInstanceOf[URN]

  override def equals(other: Any): Boolean = other match {
    case that: URN => (that canEqual this) &&
      namespace == that.namespace && identity == that.identity
    case _ => false
  }

  override def hashCode(): Int = Objects.hash(namespace, identity)
}