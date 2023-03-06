job "ingestor" {
  datacenters = ["dc1"]

  group "ingestion-service" {
    count = 1

    volume "ingestor" {
      type      = "host"
      read_only = false
      source    = "ingestor"
    }

    network {
      mode = "host"
      port "api" {}
      port "http" {}
      port "metrics" {
        to = 9001
      }
    }

    task "server" {
      driver = "docker"

      config {
        image = "anized/ingestion-service:2022.02"
        network_mode = "host"
      }

      volume_mount {
        volume      = "ingestor"
        destination = "/opt/ingestor/data"
        read_only   = false
      }

      env {
        SERVER_PORT= "${NOMAD_PORT_http}"
        API_PORT= "${NOMAD_PORT_api}"
        SPRING_CLOUD_CONSUL_HOST = "localhost"
        SPRING_CLOUD_SERVICE_REGISTRY_AUTO_REGISTRATION_ENABLED = "false"
      }

      resources {
        cpu    = 200
        memory = 512
      }

      service {
        name = "ingestion-service"
        port = "api"

        tags = [
          "urlprefix-/document"
        ]

        check {
          type     = "http"
          path     = "/health/check"
          interval = "20s"
          timeout  = "30s"
        }
      }
    }
  }
}