resource "google_compute_instance" "broker" {
  count         = var.broker-count
  name          = "${var.ownershort}-broker-${count.index}-${element(var.azs, count.index)}"
  description   = "broker nodes - Managed by Terraform"
  machine_type  = "n1-standard-1"
  zone          = element(var.azs, count.index)

  tags = ["topology-builder-demo", "broker", "ssh"]

  boot_disk {
    initialize_params {
      image = data.google_compute_image.ubuntu.self_link
    }
  }

  // Local SSD disk
  scratch_disk {
    interface = "SCSI"
  }

  network_interface {
    network = "default"

    access_config {
      // Ephemeral IP
    }
  }

  // tags
  metadata = {
    Name = "${var.ownershort}-broker-${count.index}-${element(var.azs, count.index)}"
    description = "broker nodes - Managed by Terraform"
    nice-name = "kafka-${count.index}"
    big-nice-name = "follower-kafka-${count.index}"
    brokerid = "${count.index}"
    role = "broker"
    owner = "${var.owner}"
    sshUser = "ubuntu"
    createdBy = "terraform"
    region = "${var.region}"
  }

  metadata_startup_script = "sudo apt-get update"


  service_account {
    scopes = ["userinfo-email", "compute-ro", "storage-ro"]
  }
}
