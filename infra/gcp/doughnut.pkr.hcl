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
  image_description   = "doughnut Debian12 MySQL84 base image provisioned with saltstack"
  image_name          = "doughnut-debian12-zulu25-mysql84-base-saltstack"
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

  provisioner "shell" {
    environment_vars = [
      "DEBIAN_FRONTEND=noninteractive",
      "TERM=xterm",
      "PYTHONWARNINGS=ignore::UserWarning",
      "DPKG_FRONTEND=noninteractive"
    ]
    inline = [
      "sudo -E apt-get update -y",
      "sudo -E apt-get install -y -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' -o Dpkg::Pre-Install-Pkgs::='/usr/sbin/dpkg-preconfigure --apt' python3-full python3-venv python3-pip python3-dev gcc g++ make",
      "sudo python3 -m venv /opt/salt-venv",
      "sudo /opt/salt-venv/bin/pip3 install --upgrade pip",
      "sudo /opt/salt-venv/bin/pip3 install --upgrade setuptools",
      "sudo /opt/salt-venv/bin/pip3 install distro",
      "sudo /opt/salt-venv/bin/pip3 install PyYAML",
      "sudo /opt/salt-venv/bin/pip3 install jinja2",
      "sudo /opt/salt-venv/bin/pip3 install msgpack",
      "sudo /opt/salt-venv/bin/pip3 install requests",
      "sudo /opt/salt-venv/bin/pip3 install pycryptodome",
      "sudo /opt/salt-venv/bin/pip3 install pyzmq",
      "sudo /opt/salt-venv/bin/pip3 install looseversion",
      "sudo /opt/salt-venv/bin/pip3 install packaging",
      "sudo /opt/salt-venv/bin/pip3 install certifi",
      "sudo /opt/salt-venv/bin/pip3 install tornado",
      "sudo /opt/salt-venv/bin/pip3 install salt==3006.14",
      "sudo mkdir -p /etc/salt /var/cache/salt /var/log/salt /var/run/salt /srv",
      "echo 'id: local' | sudo tee /etc/salt/minion",
      "sudo ln -sf /opt/salt-venv/bin/salt-minion /usr/local/bin/",
      "sudo ln -sf /opt/salt-venv/bin/salt-call /usr/local/bin/",
      "sudo bash -c 'cat > /etc/systemd/system/salt-minion.service << EOL\n[Unit]\nDescription=The Salt Minion\nDocumentation=man:salt-minion(1) file:///usr/share/doc/salt/html/contents.html https://docs.saltproject.io/en/latest/contents.html\nAfter=network.target\n\n[Service]\nType=notify\nNotifyAccess=all\nLimitNOFILE=8192\nEnvironment=PATH=/opt/salt-venv/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin\nExecStart=/opt/salt-venv/bin/salt-minion\n\n[Install]\nWantedBy=multi-user.target\nEOL'",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable salt-minion",
      "/opt/salt-venv/bin/salt-minion --version"
    ]
  }

  provisioner "salt-masterless" {
    local_state_tree     = "salt/states"
    local_pillar_roots   = "salt/pillar"
    remote_state_tree    = "/srv/salt"
    remote_pillar_roots  = "/srv/pillar"
    skip_bootstrap       = true
    log_level           = "info"
  }

  provisioner "shell" {
    inline = [
      "sudo systemctl stop salt-minion",
      "sudo systemctl disable salt-minion",
      "sudo rm -rf /var/cache/salt /var/run/salt",
      "sudo rm -f /etc/salt/minion_id"
    ]
  }
}
