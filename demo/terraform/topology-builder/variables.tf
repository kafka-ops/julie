variable "name" {}
variable "region" {}
variable "owner" {}
variable "ownershort" {}

variable "project" {
  default = "topology-builder"
}

variable "jenkins-count" {
  default = 1
}
variable "broker-count" {
  default = 1
}

provider "aws" {
  version = "~> 2.27"
  region = var.region
}

variable "key_name" {}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}
