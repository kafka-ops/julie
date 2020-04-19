resource "aws_instance" "brokers" {
  count         = var.broker-count
  ami           = data.aws_ami.ubuntu.id
  instance_type = local.broker-instance-type
  availability_zone = element(var.azs, count.index)
  # security_groups = ["${var.security_group}"]
  security_groups = ["${aws_security_group.brokers.name}", "${aws_security_group.zookeepers.name}", "${aws_security_group.ssh.name}"]
  key_name = var.key_name
  root_block_device {
    volume_size = 512 # 512Gb
  }
  tags = {
    Name = "${var.ownershort}-broker-${count.index}-${element(var.azs, count.index)}"
    description = "broker nodes - Managed by Terraform"
    nice-name = "kafka-${count.index}"
    big-nice-name = "follower-kafka-${count.index}"
    brokerid = "${count.index}"
    role = "broker"
    Role = "broker"
    play = "topology-builder-demo"
    owner = "${var.owner}"
    sshUser = "ubuntu"
    # sshPrivateIp = true // this is only checked for existence, not if it's true or false by terraform.py (ati)
    createdBy = "terraform"
    # ansible_python_interpreter = "/usr/bin/python3"
    #EntScheduler = "mon,tue,wed,thu,fri;1600;mon,tue,wed,thu;fri;sat;0400;"
    region = "${var.region}"
    #role_region = "brokers-${var.region}"
  }
}

  resource "aws_instance" "jenkins" {
    count         = var.jenkins-count
    ami           = data.aws_ami.ubuntu.id
    instance_type = local.jenkins-instance-type
    availability_zone = element(var.azs, count.index)
    # security_groups = ["${var.security_group}"]
    security_groups = ["${aws_security_group.jenkins.name}", "${aws_security_group.ssh.name}"]
    key_name = var.key_name
    root_block_device {
      volume_size = 1000 # 1TB
    }
    tags = {
      Name = "${var.ownershort}-jenkins-${count.index}-${element(var.azs, count.index)}"
      description = "Jenkins nodes - Managed by Terraform"
      nice-name = "jenkins-${count.index}"
      brokerid = "${count.index}"
      role = "jenkins"
      Role = "jenkins"
      play = "topology-builder-demo"
      owner = "${var.owner}"
      sshUser = "ubuntu"
      # sshPrivateIp = true // this is only checked for existence, not if it's true or false by terraform.py (ati)
      createdBy = "terraform"
      # ansible_python_interpreter = "/usr/bin/python3"
      #EntScheduler = "mon,tue,wed,thu,fri;1600;mon,tue,wed,thu;fri;sat;0400;"
      region = "${var.region}"
      #role_region = "jenkins-${var.region}"
    }
  }
