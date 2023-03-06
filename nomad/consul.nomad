job "consul" {
  datacenters = ["dc1"]
  type        = "service"

  group "consul" {
    count = 1
    network {
      mode = "host"
    }
    task "consul" {
      driver = "raw_exec"

      config {
        command = "consul"
        args = [
          "agent", "-dev"
        ]
      }

      artifact {
        source = "https://releases.hashicorp.com/consul/1.11.4/consul_1.11.4_linux_386.zip"
        options {
          checksum = "sha256:dd2f9af4edc263168ae5e03f97fcf80e72ca59753a68707254ff36a648f8876a"
        }
      }
    }
  }
}