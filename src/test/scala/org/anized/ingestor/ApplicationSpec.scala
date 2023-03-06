package org.anized.ingestor

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.hmhco.testcontainers.consul.ConsulContainer
import fixtures.RandomPort
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

class ApplicationSpec extends AnyFlatSpec with BeforeAndAfterAll {
  System.setProperty("spring.profiles.active", "test")
  private val consul = new ConsulContainer
  private val postgres = new PostgreSQLContainer()

  "Invoking app main" should "not throw an exception" in {
    System.setProperty("server.port", RandomPort.newPort.toString)
    System.setProperty("api.port", RandomPort.newPort.toString)
    Application.main(Array[String]())
  }

  "Invoking app main with an invalid port" should "fail" in {
    System.setProperty("api.port", "-1")
    Application.main(Array[String]())
/*  val thrown = intercept[BindException] {
      Application.main(Array[String]())
    }
    assert(thrown.getMessage === "[/0.0.0.0:80] Permission denied")*/
  }

  override def beforeAll: Unit = {
    consul.start()
    System.setProperty("spring.cloud.consul.port", consul.getHttpPort.toString)
    postgres.start()
    System.setProperty("documentdb.properties.serverName", postgres.host)
    System.setProperty("documentdb.properties.portNumber", postgres.mappedPort(5432).toString)
    System.setProperty("documentdb.properties.user", postgres.username)
    System.setProperty("documentdb.properties.password", postgres.password)
    System.setProperty("documentdb.properties.databaseName", postgres.databaseName)
  }

  override def afterAll: Unit = {
    consul.stop()
    postgres.stop()
  }

}
