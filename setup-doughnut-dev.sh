#!/bin/bash

set -eo pipefail

export NIXPKGS_ALLOW_UNFREE=1
export unameOut="$(uname -s)"
export os_type=Unsupported

get_os_type() {
    case "${unameOut}" in
        Linux*) os_type=Linux ;;
        Darwin*) os_type=Mac ;;
        *) os_type="UNKNOWN:${unameOut}" ;;
    esac
}

download_nixpkg_manager_install_script() {
    rm -f install-nix
    curl -o install-nix https://releases.nixos.org/nix/nix-2.6.0/install
    chmod +x ./install-nix
}

configure_nix_flakes() {
    if [[ ! -f ~/.config/nix/nix/nix.conf ]]; then
        mkdir -p ~/.config/nix
        touch ~/.config/nix/nix.conf
    fi

    if ! grep -Fxq "experimental-features = nix-command flakes" ~/.config/nix/nix.conf; then
        echo 'experimental-features = nix-command flakes' >>~/.config/nix/nix.conf
    fi
    nix-channel --update
    nix-env -iA nixpkgs.nix && nix-env -u --always
}

install_nixpkg_manager() {
    get_os_type
    if ! command -v nix &>/dev/null; then
        download_nixpkg_manager_install_script
        if [ "${os_type}" = "Mac" ]; then
            ./install-nix --darwin-use-unencrypted-nix-store-volume
        elif [ "${os_type}" = "Linux" ]; then
            ./install-nix --no-daemon
        else
            echo "Unsupported OS Platform for Nix development enviroment. Exiting!!!"
            exit 1
        fi
        rm -f ./install-nix
    fi
    configure_nix_flakes
}

install_nixpkg_manager

. ~/.nix-profile/etc/profile.d/nix.sh
nix develop -c $SHELL
