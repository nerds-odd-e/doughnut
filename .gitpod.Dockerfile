# syntax=docker/dockerfile:1.3.1
FROM gitpod/workspace-full-vnc
# ---------------------------------------------------
# -------------------- USER root --------------------
# ---------------------------------------------------

USER root

# Install Cypress dependencies.
RUN apt-get -y update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    libgtk2.0-0 \
    libgtk-3-0 \
    libnotify-dev \
    libgconf-2-4 \
    libnss3 \
    libxss1 \
    libasound2 \
    libxtst6 \
    xauth \
    xvfb \
    zsh \
    htop \
    lsof \
    net-tools \
    git-all \
    vim \
    git-extras \
    unzip \
    wget \
    zip \
    bash-completion \
    zsh-completion \
    procps \
    gnupg \
    curl \
    gawk \
    dirmngr \
    xclip \
    fasd \
    fzf \
    && apt-get autoremove \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /var/cache/apt \
    && rm -rf /nix \
    && rm -rf /home/gitpod/.nix-channels \
    && rm -rf /home/gitpod/.nix-defexpr \
    && rm -rf /home/gitpod/.nix-profile \
    && rm -rf /home/gitpod/.config/nixpkgs


# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user, zsh and zimfw

CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

RUN curl -o install-nix https://releases.nixos.org/nix/nix-2.7.0/install \
    && chmod +x ./install-nix \
    && ./install-nix --no-daemon
RUN mkdir -p /home/gitpod/.config/nix \
    && touch /home/gitpod/.config/nix/nix.conf \
    && echo "experimental-features = nix-command flakes" >> /home/gitpod/.config/nix/nix.conf

EXPOSE 3000
EXPOSE 3309
EXPOSE 5900
EXPOSE 6080
EXPOSE 8081
EXPOSE 9081
EXPOSE 9082
