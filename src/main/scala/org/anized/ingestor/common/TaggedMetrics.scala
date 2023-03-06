package org.anized.ingestor.common

import io.micrometer.core.instrument.{Counter, Gauge, Meter, Metrics, Tag, Timer}

import java.util.function.Supplier
import scala.jdk.CollectionConverters._

object TaggedMetrics {
  private val tagify = (tags: Iterable[String]) =>
    Option.when(tags.size % 2 == 0)(tags)
      .map(_.grouped(2) map {
        case Seq(key, value) => Tag.of(key,value)
      })
      .map(_.iterator.to(Iterable))
      .getOrElse(Iterable.empty)

  def counter(name: String,
              tags: Iterable[String] = Seq.empty): Counter =
    Counter.builder(name)
      .tags(tagify(tags).asJava)
      .register(Metrics.globalRegistry)

  def timer(name: String,
            tags: Iterable[String] = Seq.empty): Timer =
    Timer.builder(name)
      .tags(tagify(tags).asJava)
      .register(Metrics.globalRegistry)

  def gauge(name: String, value: Number,
            tags: Iterable[String] = Iterable.empty): Gauge =
    Gauge.builder(name, () => value)
      .tags(tagify(tags).asJava)
      .register(Metrics.globalRegistry)

  val asString: Seq[String] => String = (tags: Seq[String]) =>
    tags.grouped(2).foldLeft("") { (acc, pair) => s"$acc ${pair.head}=${pair.last}" }
}
