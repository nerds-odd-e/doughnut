#!/usr/bin/env bash

set -eo pipefail

NIXPKGS_ALLOW_UNFREE=1
unameOut="$(uname -s)"
os_type=Unsupported
export NIXPKGS_ALLOW_UNFREE unameOut os_type

get_os_type() {
  case "${unameOut}" in
    Linux*) os_type=Linux ;;
    Darwin*) os_type=Mac ;;
    *) os_type="UNKNOWN:${unameOut}" ;;
  esac
}

download_nixpkg_manager_install_script() {
  rm -f ./install-nix
  curl -sSf -L -o install-nix https://install.lix.systems/lix
  chmod +x ./install-nix
}

configure_nix_flakes() {
  if [ ! -f "${HOME}/.config/nix/nix.conf" ]; then
    mkdir -p "${HOME}/.config/nix"
    touch "${HOME}/.config/nix/nix.conf"
  fi

  if ! grep -Fxq "experimental-features = nix-command flakes" "${HOME}/.config/nix/nix.conf"; then
    cat <<-EOF > "${HOME}/.config/nix/nix.conf"
    experimental-features = nix-command flakes
EOF
  fi
}

allow_nix_unfree() {
  mkdir -p "${HOME}/.config/nixpkgs"
  touch "${HOME}/.config/nixpkgs/config.nix"
  cat <<-EOF > "${HOME}/.config/nixpkgs/config.nix"
  { allowUnfree = true; }
EOF
}

install_nixpkg_manager() {
  get_os_type
  if ! command -v nix >/dev/null 2>&1; then
    download_nixpkg_manager_install_script
    touch .bash_profile
    if [ "${os_type}" == "Mac" || "${os_type}" == "Linux" ]; then
      ./install-nix -s -- install
    else
      echo "Unsupported OS Platform for Nix development enviroment. Exiting!!!"
      exit 1
    fi
  fi

  allow_nix_unfree

  configure_nix_flakes
}

install_nixpkg_manager

echo "------------------------------------------ CONGRATS !!! ----------------------------------------------------"
echo "  doughnut basic nix development environment tooling setup complete."
echo "  Please exit this shell terminal and start a new one in doughnut root directory then execute 'nix develop'."
echo "------------------------------------------    END       ----------------------------------------------------"
rm -f ./install-nix
