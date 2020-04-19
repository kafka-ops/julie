variable "region" {
  default = "eu-west-1"
}

variable "owner" {
  default = "purbon"
}

variable "myip" {}

variable "ownershort" {
  default = "pub"
}

variable "instance_type" {
  default = "t2.medium"
}


module "ireland-cluster" {
  source         = "./topology-builder"
  broker-count   = 1
  jenkins-count  = 1
  name           = "ireland-cluster"
  region         = "eu-west-1"
  azs            = ["eu-west-1a"]
  owner          = var.owner
  ownershort     = var.ownershort
  key_name       = "purbon-ireland"
  myip           = var.myip
}
