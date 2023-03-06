package metrics

import org.anized.ingestor.common.TaggedMetrics
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps

class MetricTagSpec extends AnyFlatSpec with Matchers {

  "A valid gauge" should "be created" in {
    val gauge = TaggedMetrics.gauge("test-gauge", 0, Seq("key", "value"))
    assert(gauge.value() === 0)
  }

  "A counter with unmatched tag" should "fail to be created" in {
    val counter = TaggedMetrics.counter("test-counter", Seq("key", "value", "another"))
    assert(counter.count() === 0)
  }

  "Metric tags" should "be rendered readably for logging" in {
    val tags = Seq("id", "112", "filename", "doc.name")
    assert(TaggedMetrics.asString(tags) === " id=112 filename=doc.name")
  }


}