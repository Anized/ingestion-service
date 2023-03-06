package org.anized.ingestor.metrics

import io.micrometer.core.instrument.Metrics.globalRegistry
import io.micrometer.core.instrument.{Clock, Metrics}
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.prometheus.client.CollectorRegistry
import scala.jdk.CollectionConverters._

object MetricsRegistry {

  private val prometheusMeterRegistry = new PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)
  Metrics.addRegistry((new CompositeMeterRegistry).add(prometheusMeterRegistry))
  globalRegistry.config.commonTags("service", "ingestion-service")
  lazy val getPrometheus: Option[PrometheusMeterRegistry] =
    Metrics.globalRegistry.getRegistries.asScala.collectFirst { case reg: PrometheusMeterRegistry => reg}
}
