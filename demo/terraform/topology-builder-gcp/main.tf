output "my_public_address"  {
  value = "${var.my_public_subnet}"
}
output "my_private_address"  {
  value = "${var.my_private_subnet}"
}

variable "azs" {
  description = "list of available zones"
  type = list
}

variable "myip" {}

locals {
  myip-cidr = "${var.myip}/32"
}

variable "network_self_link" {
  default = "default"
}
