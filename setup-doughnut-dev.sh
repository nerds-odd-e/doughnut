#!/usr/bin/env bash

set -eo pipefail

readonly NIX_CONF_DIR="${HOME}/.config/nix"
readonly NIX_CONF="${NIX_CONF_DIR}/nix.conf"
readonly NIXPKGS_CONF_DIR="${HOME}/.config/nixpkgs"
readonly NIXPKGS_CONF="${NIXPKGS_CONF_DIR}/config.nix"
readonly TEMP_DIR="$(mktemp -d)"

# Check bash version (we need 4.0+ for some features)
if ((BASH_VERSINFO[0] < 4)); then
  echo "Error: Bash 4.0 or higher is required"
  exit 1
fi

# Enable debug mode if DEBUG env var is set
[[ -n "${DEBUG}" ]] && set -x

# Error handling with cleanup
cleanup() {
  [[ -d "${TEMP_DIR}" ]] && rm -rf "${TEMP_DIR}"
}

trap 'echo "Error: Script failed on line $LINENO"; cleanup' ERR
trap cleanup EXIT
trap cleanup SIGTERM SIGINT SIGHUP
if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
  trap cleanup SIGTERM SIGINT SIGHUP EXIT
fi

log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

# Global variables
declare -r unameOut="$(uname -s)"
declare os_type="Unsupported"

get_os_type() {
  case "${unameOut}" in
    Linux*)  os_type="Linux" ;;
    Darwin*) os_type="Mac" ;;
    *)       log "Error: Unknown OS ${unameOut}"; exit 1 ;;
  esac
}

check_wsl2() {
  if [[ "${os_type}" == "Linux" ]] && grep -qi microsoft /proc/version; then
    if ! grep -qi "wsl2" /proc/version; then
      log "Error: WSL1 detected. Please upgrade to WSL2 for better performance"
      exit 1
    fi
    log "WSL2 environment detected"
  fi
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
  if ! curl -sSf -L -o "${TEMP_DIR}/install-nix" https://install.lix.systems/lix; then
    log "Failed to download nix installer"
    exit 1
  fi
  chmod +x "${TEMP_DIR}/install-nix" || {
    log "Failed to make installer executable"
    exit 1
  }
}

configure_nix_flakes() {
  local flakes_config="experimental-features = nix-command flakes"

  # Check if we have write permission to config directory
  if ! mkdir -p "${HOME}/.config/nix" 2>/dev/null; then
    log "Error: Cannot create directory ${HOME}/.config/nix"
    exit 1
  fi

  touch "${NIX_CONF}"

  if ! grep -Fxq "${flakes_config}" "${NIX_CONF}"; then
    log "Configuring nix flakes"
    echo "${flakes_config}" > "${NIX_CONF}"
  fi
}

allow_nix_unfree() {
  mkdir -p "${NIXPKGS_CONF_DIR}"
  log "Enabling unfree packages"
  echo '{ allowUnfree = true; }' > "${NIXPKGS_CONF}"
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
    cat "${TEMP_DIR}/install-nix" | sh -s -- install
  else
    log "Error: Unsupported OS Platform for Nix development environment"
    exit 1
  fi

  # Source nix in current shell
  if [ -e '/nix/var/nix/profiles/default/etc/profile.d/nix-daemon.sh' ]; then
    . '/nix/var/nix/profiles/default/etc/profile.d/nix-daemon.sh'
  fi

  allow_nix_unfree
  configure_nix_flakes
  log "Nix installation completed successfully"
}

main() {
  local rc=0

  # Prevent running as root
  if [[ $EUID -eq 0 ]]; then
    log "Error: This script should not be run as root"
    exit 1
  fi

  log "Starting doughnut development environment setup"
  check_wsl2
  install_nixpkg_manager || rc=$?

  if [[ $rc -eq 0 ]]; then
    log "------------------------------------------ CONGRATS !!! ----------------------------------------------------"
    log "  doughnut basic nix development environment tooling setup completed successfully."
    log "  Please exit this shell terminal and start a new one in doughnut root directory then execute 'nix develop'."
    log "  To uninstall Nix in the future, you can run: /nix/nix-installer uninstall"
    log "------------------------------------------    END       ----------------------------------------------------"
  else
    log "------------------------------------------ WARNING !!! ----------------------------------------------------"
    log "  Installation FAILED with warnings or errors. Please check the logs above."
    log "  You may need to manually verify the installation or try again."
    log "------------------------------------------    END       ----------------------------------------------------"
  fi

  exit $rc
}

main "$@"
