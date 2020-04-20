resource "google_compute_network" "vpc" {
  name          =  format("%s","${var.ownershort}-${var.project}-vpc")
  auto_create_subnetworks = "false"
  routing_mode            = "GLOBAL"
}
resource "google_compute_firewall" "allow-internal" {
  name    = "${var.ownershort}-${var.project}-fw-allow-internal"
  network = google_compute_network.vpc.name
  allow {
    protocol = "icmp"
  }
  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }
  allow {
    protocol = "udp"
    ports    = ["0-65535"]
  }
  source_ranges = [
    "${var.my_private_subnet}",
    "${var.my_public_subnet}"
  ]
}

resource "google_compute_firewall" "broker" {
  name    = "broker"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["9092"]
  }

  // Allow traffic from my ip to instances with an broker tag
  source_ranges = ["${local.myip-cidr}"]
  target_tags   = ["broker"]
}

resource "google_compute_firewall" "allow-access-ssh" {
  name    = "${var.ownershort}-fw-allow-bastion"
  network = google_compute_network.vpc.name
  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
  source_ranges = ["${local.myip-cidr}"]
  target_tags = ["ssh"]
}
