variable "name" {}
variable "region" {}
variable "gcp-project" {
  default = "solutionsarchitect-01"
}
variable "owner" {}
variable "ownershort" {}

variable "credentials" {}
variable "project" {
  default = "topology-builder"
}

variable "jenkins-count" {
  default = 1
}
variable "broker-count" {
  default = 1
}

provider "google" {
  credentials = var.credentials
  project     = var.gcp-project
  region      = var.region
}


data "google_compute_image" "ubuntu" {
  family  = "ubuntu-1804-lts"
  project = "ubuntu-os-cloud"
}

variable "my_private_subnet" {
        default = "10.26.1.0/24"
    }
variable "my_public_subnet" {
        default = "10.26.2.0/24"
    }

output "ip" {
  value = "${google_compute_instance.broker.*.network_interface.0.access_config.0.nat_ip}"
}
