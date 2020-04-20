resource "google_compute_subnetwork" "public_subnet" {
  name          = format("%s","${var.ownershort}-${var.project}-${var.region}-pub-net")
  ip_cidr_range = var.my_public_subnet
  network       = var.network_self_link
  region        = var.region
}
resource "google_compute_subnetwork" "private_subnet" {
  name          = format("%s","${var.ownershort}-${var.project}-${var.region}-pri-net")
  ip_cidr_range = var.my_private_subnet
  network      =  var.network_self_link
  region        = var.region
}
