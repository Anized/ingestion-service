job "postgres" {
  datacenters = ["dc1"]
  type = "service"

  group "postgres" {
    count = 1

    volume "postgresql" {
      type      = "host"
      read_only = false
      source    = "postgresql"
    }

    network {
      port  "db"  {
        static = 5432
      }
    }

    task "postgres" {
      driver = "docker"

      volume_mount {
        volume      = "postgresql"
        destination = "/var/lib/postgresql/data"
        read_only   = false
      }

      config {
        image = "postgres"
        network_mode = "host"
        port_map {
          db = 5432
        }
      }
      env {
        POSTGRES_USER="postgres"
        POSTGRES_PASSWORD="password"
      }

      logs {
        max_files     = 5
        max_file_size = 15
      }

      service {
        name = "postgres"
        tags = ["postgres for ingestion-service"]
        port = "db"

        check {
          name     = "alive"
          type     = "tcp"
          interval = "20s"
          timeout  = "30s"
        }
      }
    }
    restart {
      attempts = 10
      interval = "5m"
      delay = "25s"
      mode = "delay"
    }

  }

  update {
    max_parallel = 1
    min_healthy_time = "5s"
    healthy_deadline = "3m"
    auto_revert = false
    canary = 0
  }
}