package domain

import org.anized.ingestor.common.URN
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps
import scala.util.{Failure, Success}

class URNSpec extends AnyFlatSpec with Matchers {

  "A valid URN" should "be parsed" in {
    URN.parse("urn:document-id:123456") map { subject =>
      assert(subject.namespace === "document-id")
      assert(subject.identity === "123456")
    }
  }

  "An invalid URN" should "fail" in {
    URN.parse("xxx") match {
      case Success(_) => fail("expected parse to fail")
      case Failure(t) =>
        assert(t.getMessage === "URN string cannot not be parsed from [xxx]")
    }
  }

  "Equivalent URNs" should "evaluate as equal" in {
    URN.parse("urn:document-id:123456") map { urn1 =>
      URN.parse("urn:document-id:123456") map { urn2 =>
        assert(urn1 === urn2)
        assert(urn1.hashCode() === urn2.hashCode())
      }
    }
  }

  "Non-equivalent URNs" should "not evaluate as equal" in {
    URN.parse("urn:document-id:654321") map { urn1 =>
      URN.parse("urn:document-id:123456") map { urn2 =>
        assert(urn1.equals(urn2) === false)
        assert(urn1.hashCode() !== urn2.hashCode())
      }
    }
  }

  "Equality test with non-URNs" should "not evaluate as equal" in {
    URN.parse("urn:document-id:654321") map { urn1 =>
      assert(urn1.equals("senseless") === false)
    }
  }

}