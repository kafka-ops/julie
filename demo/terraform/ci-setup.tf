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
  default = "t3a.medium"
}

variable "account_file" {
  default = "~/.gcloud/account.json"
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

#module "us-central-cluster" {
#  source         = "./topology-builder-gcp"
#  broker-count   = 1
#  jenkins-count  = 1
#  name           = "us-central-cluster"
#  region         = "us-central1"
#  azs            = ["us-central1a"]
#  owner          = var.owner
#  ownershort     = var.ownershort
#  credentials    = file(var.account_file)
#  myip           = var.myip
#  }
