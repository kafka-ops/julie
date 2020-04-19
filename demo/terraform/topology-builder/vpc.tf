#data "aws_vpc" "MAIN_VPC" {
#  filter {
#    name = "tag:Name"
#
#    values = [
#      "vfde-sandbox-eucentral1-main",
#    ]
#  }
#}

#resource "aws_vpc" "main" {
#  cidr_block = "172.31.0.0/16"
#}

variable "vpc_id" {
 default = "vpc-e315c986"
}

data "aws_vpc" "main" {
  id = "${var.vpc_id}"
  cidr_block = "172.31.0.0/16"
}
