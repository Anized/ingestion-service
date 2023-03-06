import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion = "2.6.19"
lazy val akkaManagementVersion = "1.1.3"
lazy val springBootVersion = "2.7.0"
lazy val springCloudVersion = "3.1.1"
lazy val slickVersion = "3.3.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.anized",
      scalaVersion := "2.13.8"
    )),
    name := "ingestion-service",
    organization := "org.anized",
    version := "2022.02",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "3.0.4",
      "com.lightbend.akka.discovery" %% "akka-discovery-consul" % akkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "ch.megard" %% "akka-http-cors" % "1.1.3",
      "org.springframework.boot" % "spring-boot-starter" % springBootVersion,
      "org.springframework.boot" % "spring-boot-starter-web" % springBootVersion,
      "org.springframework.boot" % "spring-boot-starter-actuator" % springBootVersion,
      "org.springframework.cloud" % "spring-cloud-starter-consul-all" % springCloudVersion,

      "org.postgresql" % "postgresql" % "42.3.6",
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,

      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "io.micrometer" % "micrometer-registry-prometheus" % "1.9.0",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.12" % Test,
      "org.mockito" %% "mockito-scala-scalatest" % "1.17.7" % Test,
      "com.typesafe.slick" %% "slick-testkit" % slickVersion % Test,
      "org.testcontainers" % "postgresql" % "1.17.2" % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.8" % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.8" % Test,
      "com.hmhco" % "testcontainers-consul" % "0.0.4" % Test,
      "org.awaitility" % "awaitility" % "4.2.0" % Test
    )
  )
enablePlugins(DockerPlugin, JavaAppPackaging)

coverageEnabled := false
coverageDataDir := file("docs/coverage")
Test / envVars := Map("spring.profiles.active" -> "test")
Test / fork := true

Docker / packageName := "anized/ingestion-service"
dockerExposedPorts := Seq(8080)
dockerBaseImage := "anapsix/alpine-java"

dockerCommands ++= Seq(
  ExecCmd("WORKDIR", "files")
)