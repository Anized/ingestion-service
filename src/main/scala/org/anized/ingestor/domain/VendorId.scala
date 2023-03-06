package org.anized.ingestor.domain

import VendorId.vendor_nss
import org.anized.ingestor.common.URN

import javax.lang.model.SourceVersion
import scala.util.Try

case class VendorId(id: String) extends URN(vendor_nss, id)

object VendorId {
  val vendor_nss = "vendor-id"

  def parseUrn(vendorUrn: String): Try[VendorId] =
    URN.parse(vendorUrn) flatMap validate

  def fromUrn(vendorUrn: URN): Try[VendorId] = validate(vendorUrn)

  def validate(vendorUrn: URN): Try[VendorId] =
    Try(vendorUrn)
      .collect { case urn if isValid(urn) => VendorId(urn.identity)}

  private val isValid = (urn: URN) =>
    urn.namespace == vendor_nss && SourceVersion.isIdentifier(urn.identity)
}