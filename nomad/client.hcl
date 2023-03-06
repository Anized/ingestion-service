client {
  host_volume "postgresql" {
    path      = "/opt/postgres/data"
    read_only = false
  }
}