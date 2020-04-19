locals {
  broker-instance-type = "t3a.large"
  jenkins-instance-type = "t3a.large"

}

locals {
  brokers-eu-west-one = "0.0.0.0/0" # need to lock this down
  brokers-eu-central-one = "0.0.0.0/0" # need to lock this down
  brokers-all = "0.0.0.0/0" # need to lock this down
}

variable "myip" {}

variable "azs" {
  description = "Run the EC2 Instances in these Availability Zones"
  type = list
}

locals {
  myip-cidr = "${var.myip}/32"
}

// Output
output "public_ips" {
  value = ["${aws_instance.brokers.*.public_ip}", "${aws_instance.jenkins.*.public_ip}"]
}

output "public_dns" {
  value = ["${aws_instance.brokers.*.public_dns}", "${aws_instance.jenkins.*.public_dns}"]
}
