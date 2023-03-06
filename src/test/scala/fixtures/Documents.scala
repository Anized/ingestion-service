package fixtures

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import org.anized.ingestor.domain.{Document, DocumentStatus, VendorId}
import org.apache.http.entity.ContentType

import java.net.URI
import java.nio.file.{FileSystems, Path}
import java.time.LocalDateTime
import scala.concurrent.Future
import scala.language.postfixOps


object Documents {

  val testDocument: Document = Document(name = "test.doc",
    timestamp = LocalDateTime.parse("2022-05-25T12:30:00"),
    status = DocumentStatus.Defined,
    ownerId = VendorId.parseUrn("urn:vendor-id:tester").get,
    contentUri = URI.create(""), contentType = ContentType.WILDCARD)

  val pdfContent: ContentType = ContentType.parse("application/pdf")

  def loadDocument(path: String): Iterator[Byte] = {
    import java.io.BufferedInputStream
    val resource = getClass.getClassLoader.getResource(path)
    val reader = new BufferedInputStream(resource.openStream())
    Iterator.continually(reader.read()).takeWhile(-1 !=).map(i => i.toByte)
  }

  def streamDocument(path: String): Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(
      FileSystems.getDefault.getPath(getClass.getClassLoader.getResource(path).getPath))

  val documentList: List[Document] = List(
    testDocument.copy(id = 1, name = "document.one"),
    testDocument.copy(id = 2, name = "document.two"))
}
