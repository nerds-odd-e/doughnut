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
    bat \
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
    procps \
    gnupg \
    curl \
    gawk \
    dirmngr \
    xclip \
    fasd \
    fzf \
    && apt-get -y upgrade \
    && apt-get autoremove \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /var/cache/apt \
    && chsh -s $(which zsh) \
    && chsh -s $(which zsh) gitpod \
    && curl -fsSL https://raw.githubusercontent.com/zimfw/install/master/install.zsh | zsh


# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user, zsh and zimfw

CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

RUN mkdir -p /home/gitpod/.config/nix \
    && touch /home/gitpod/.config/nix/nix.conf \
    && curl -fsSL https://raw.githubusercontent.com/zimfw/install/master/install.zsh | zsh
    && curl -L https://nixos.org/nix/install --no-daemon | sh \
    && echo "experimental-features = nix-command flakes" >> /home/gitpod/.config/nix/nix.conf \
    && . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \

EXPOSE 3000
EXPOSE 3309
EXPOSE 5900
EXPOSE 6080
EXPOSE 8081
EXPOSE 9081
EXPOSE 9082
