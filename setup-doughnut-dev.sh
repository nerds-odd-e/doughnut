#!/usr/bin/env bash

set -eo pipefail

# Check bash version (we need 4.0+ for some features)
if ((BASH_VERSINFO[0] < 4)); then
  echo "Error: Bash 4.0 or higher is required"
  exit 1
fi

# Enable debug mode if DEBUG env var is set
[[ -n "${DEBUG}" ]] && set -x

# Error handling
trap 'echo "Error: Script failed on line $LINENO"' ERR

# Logging function
log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

# Global variables
declare -x NIXPKGS_ALLOW_UNFREE=1
declare -x unameOut
declare -x os_type="Unsupported"

unameOut="$(uname -s)"

get_os_type() {
  case "${unameOut}" in
    Linux*)  os_type="Linux" ;;
    Darwin*) os_type="Mac" ;;
    *)       log "Error: Unknown OS ${unameOut}"; exit 1 ;;
  esac
}

download_nixpkg_manager_install_script() {
  if ! command -v curl >/dev/null 2>&1; then
    log "Error: curl is required but not installed"
    exit 1
  fi

  # Check internet connectivity
  if ! curl -s --head https://install.lix.systems >/dev/null; then
    log "Error: No internet connection or install.lix.systems is unreachable"
    exit 1
  fi

  rm -f ./install-nix
  if ! curl -sSf -L -o install-nix https://install.lix.systems/lix; then
    log "Failed to download nix installer"
    exit 1
  fi
  chmod +x ./install-nix || {
    log "Failed to make installer executable"
    exit 1
  }
}

configure_nix_flakes() {
  local nix_conf="${HOME}/.config/nix/nix.conf"
  local flakes_config="experimental-features = nix-command flakes"

  # Check if we have write permission to config directory
  if ! mkdir -p "${HOME}/.config/nix" 2>/dev/null; then
    log "Error: Cannot create directory ${HOME}/.config/nix"
    exit 1
  fi

  touch "${nix_conf}"

  if ! grep -Fxq "${flakes_config}" "${nix_conf}"; then
    log "Configuring nix flakes"
    echo "${flakes_config}" > "${nix_conf}"
  fi
}

allow_nix_unfree() {
  local nixpkgs_conf="${HOME}/.config/nixpkgs/config.nix"

  mkdir -p "${HOME}/.config/nixpkgs"
  log "Enabling unfree packages"
  echo '{ allowUnfree = true; }' > "${nixpkgs_conf}"
}

install_nixpkg_manager() {
  get_os_type

  if command -v nix >/dev/null 2>&1; then
    log "Nix package manager already installed"
    return 0
  fi

  log "Starting nix installation"
  download_nixpkg_manager_install_script

  touch "${HOME}/.bash_profile"

  if [[ "${os_type}" == "Mac" || "${os_type}" == "Linux" ]]; then
    ./install-nix -s -- install
  else
    log "Error: Unsupported OS Platform for Nix development environment"
    exit 1
  fi

  allow_nix_unfree
  configure_nix_flakes
  log "Nix installation completed successfully"
}

# Cleanup on script exit
trap 'rm -f ./install-nix' EXIT

main() {
  local rc=0
  log "Starting doughnut development environment setup"
  install_nixpkg_manager || rc=$?

  log "------------------------------------------ CONGRATS !!! ----------------------------------------------------"
  log "  doughnut basic nix development environment tooling setup complete."
  log "  Please exit this shell terminal and start a new one in doughnut root directory then execute 'nix develop'."
  log "------------------------------------------    END       ----------------------------------------------------"

  exit $rc
}

main "$@"
