packer {
  required_plugins {
    googlecompute = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/googlecompute"
    }
  }
}

variable "repo_path" {
  type    = string
  default = "https://github.com/nerds-odd-e/doughnut"
}

variable "project_id" {
  type    = string
  default = "carbon-syntax-298809"
}

variable "source_image_family" {
  type    = string
  default = "debian-12"
}

variable "machine_type" {
  type    = string
  default = "e2-medium"
}

variable "region" {
  type    = string
  default = "us-east1"
}

variable "zone" {
  type    = string
  default = "us-east1-b"
}

variable "ssh_username" {
  type    = string
  default = "packer"
}

variable "service_account_json" {
  type    = string
  default = "${env("SERVICE_ACCOUNT_JSON")}"
}

source "googlecompute" "doughnut" {
  project_id          = var.project_id
  state_timeout       = "10m"
  machine_type        = var.machine_type
  source_image_family = var.source_image_family
  region              = var.region
  zone                = var.zone
  image_description   = "doughnut Debian12 MySQL80 base image provisioned with saltstack"
  image_name          = "doughnut-debian12-zulu22-mysql80-base-saltstack"
  ssh_username        = var.ssh_username
  disk_size           = 25
  disk_type           = "pd-ssd"
  account_file        = var.service_account_json
  tags                = ["packer"]
  use_os_login        = true
  use_internal_ip     = false
  ssh_timeout         = "10m"
}

build {
  sources = ["source.googlecompute.doughnut"]

  provisioner "salt-masterless" {
    local_state_tree     = "salt/states"
    local_pillar_roots   = "salt/pillar"
    remote_state_tree    = "/srv/salt"
    remote_pillar_roots  = "/srv/pillar"
  }
}
