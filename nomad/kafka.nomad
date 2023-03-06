job "kafka" {
  datacenters = ["dc1"]
  type        = "service"
  priority    = 50

  update {
    stagger      = "5s"
    max_parallel = 2
  }
  group "zookeeper" {
    count = 1
    task "zookeeper" {
      driver = "docker"
      config {
        dns_servers = ["${NOMAD_IP_zk_port}"]
        image       = "wurstmeister/zookeeper"
        port_map {
          zk_port = 2181
        }
      }
      resources {
        cpu    = 20
        memory = 200
        network {
          port "zk_port" {
            static = 2181
          }
          mbits = 1
        }
      }

      service {
        port = "zk_port"
        name = "kf-zookeeper"
        check {
          name     = "hello zookeeper"
          type     = "tcp"
          interval = "5s"
          timeout  = "2s"
        }
      }
    }
  }
  group "kafka-cluster" {
    count = 3

    task "kafka" {

      driver = "docker"
      config {
        dns_servers = ["${NOMAD_IP_kafka_port}"]
        image       = "wurstmeister/kafka"

        port_map {
          kafka_port = 8092
        }
      }

      resources {
        cpu    = 20
        memory = 200
        network {
          port "kafka_port" {
            static = 8092
          }
          mbits = 1
        }
      }

      service {
        port = "kafka_port"
        check {
          name     = "hello kafka"
          type     = "tcp"
          interval = "5s"
          timeout  = "2s"
        }
      }

      env {
        KAFKA_ZOOKEEPER_CONNECT    = "${NOMAD_ADDR_zk_port}"
        KAFKA_ADVERTISED_PORT      = 8092
        KAFKA_ADVERTISED_HOST_NAME = "127.0.0.1"
      }
    }
  }
}