#!/bin/bash

set -eo pipefail

export NIXPKGS_ALLOW_UNFREE=1
export unameOut="$(uname -s)"
export os_type=Linux

case "${unameOut}" in
    Linux*) os_type=Linux ;;
    Darwin*) os_type=Mac ;;
    *) os_type="UNKNOWN:${unameOut}" ;;
esac

nix_installed_path=$(which nix 2>/dev/null)
if [[ $nix_installed_path == *"not found"* ]]; then
    if [ "${os_type}" = "Mac" ]; then
        curl -k -L https://nixos.org/nix/install --darwin-use-unencrypted-nix-store-volume | sh
    elif [ "${os_type}" = "Linux" ]; then
        curl -k -L https://nixos.org/nix/install --no-daemon | sh
    else
        echo "Unsupported OS Platform for Nix development enviroment. Exiting!!!"
        exit 1
    fi
    mkdir -p ~/.config/nix
    echo 'experimental-features = nix-command flakes' >>~/.config/nix/nix.conf
fi

. ~/.nix-profile/etc/profile.d/nix.sh
nix develop -c $SHELL
