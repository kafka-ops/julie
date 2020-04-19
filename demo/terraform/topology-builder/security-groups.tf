resource "aws_security_group" "ssh" {
  description = "Managed by Terraform"
  name = "${var.ownershort}-${var.project}-ssh"

  # Allow ping from my ip and self
  ingress {
    from_port = 8
    to_port = 0
    protocol = "icmp"
    self = true
    cidr_blocks = ["${local.myip-cidr}"]
  }

  # ssh from me and self
  ingress {
      from_port = 22
      to_port = 22
      protocol = "TCP"
      self = true
      cidr_blocks = ["${local.myip-cidr}"]
  }

  # ssh from anywhere
  ingress {
      from_port = 22
      to_port = 22
      protocol = "TCP"
      cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "brokers" {
  description = "brokers - Managed by Terraform"
  name = "${var.ownershort}-${var.project}-brokers"

   # cluster
  ingress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      self = true
  }

  # client connections from ssh hosts, connect, my ip, clients
  ingress {
      from_port = 9092
      to_port = 9092
      protocol = "TCP"
      self = true
      cidr_blocks = ["${local.myip-cidr}"]
      security_groups = ["${aws_security_group.jenkins.id}"] # should an explicit group for clients, ssh covers it
  }

  # Allow ping from my ip, self, bastion
  ingress {
    from_port = 8
    to_port = 0
    protocol = "icmp"
    self = true
    cidr_blocks = ["${local.myip-cidr}"]
  }

  egress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "zookeepers" {
  description = "Zookeeper security group - Managed by Terraform"
  name = "${var.ownershort}-${var.project}-zookeepers"

  ingress {
      from_port = 2181
      to_port = 2181
      protocol = "TCP"
      security_groups = ["${aws_security_group.brokers.id}"]
      cidr_blocks =  ["${data.aws_vpc.main.cidr_block}"]
  }

  ingress {
      from_port = 2888
      to_port = 2888
      protocol = "TCP"
      self = true
  }

  ingress {
      from_port = 3888
      to_port = 3888
      protocol = "TCP"
      self = true
  }

  egress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "jenkins" {
  description = "jenkins - Managed by Terraform"
  name = "${var.ownershort}-jenkins"

   # cluster
  ingress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      self = true
  }

  # allow connection from my ip to the jenkins via web
  ingress {
      from_port = 8080
      to_port = 8080
      protocol = "TCP"
      self = true
      cidr_blocks = ["${local.myip-cidr}"]
  }

  # Allow ping from my ip, self, bastion
  ingress {
    from_port = 8
    to_port = 0
    protocol = "icmp"
    self = true
    cidr_blocks = ["${local.myip-cidr}"]
  }

  egress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      cidr_blocks = ["0.0.0.0/0"]
  }
}
