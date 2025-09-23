# syntax=docker.io/docker/dockerfile:1.18.0

ARG TARGET_PLATFORM=linux/amd64

FROM --platform=$TARGET_PLATFORM gitpod/workspace-full-vnc
# ---------------------------------------------------
# -------------------- USER root --------------------
# ---------------------------------------------------

USER root

ENV TERM=xterm
ENV DEBIAN_FRONTEND=noninteractive

# Install Cypress dependencies.
RUN apt-get -y update \
    && apt-get install -y lsb-release ca-certificates curl apt-utils \
    && curl -sSLo /tmp/debsuryorg-archive-keyring.deb https://packages.sury.org/debsuryorg-archive-keyring.deb \
    && dpkg -i /tmp/debsuryorg-archive-keyring.deb \
    && sh -c 'echo "deb [signed-by=/usr/share/keyrings/deb.sury.org-php.gpg] https://packages.sury.org/php/ $(lsb_release -sc) main" > /etc/apt/sources.list.d/php.list'

RUN apt-get install -y \
    libgtk2.0-0 \
    libgtk-3-0 \
    libgbm-dev \
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
    git \
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
    neovim \
    direnv \
    && apt-get autoremove \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /var/cache/apt \
    && rm -rf /nix \
    && rm -rf /home/gitpod/.nix-channels \
    && rm -rf /home/gitpod/.nix-defexpr \
    && rm -rf /home/gitpod/.nix-profile \
    && rm -rf /home/gitpod/.config/nixpkgs

# use bash over dash for /bin/sh
RUN dpkg-reconfigure -p critical dash

RUN mkdir -m 0755 /nix && \
    chown gitpod /nix && \
    mkdir -p /etc/nix && \
    echo 'sandbox = false' > /etc/nix/nix.conf && \
    echo 'filter-syscalls = false' >> /etc/nix/nix.conf

RUN bash -c 'set -e && \
    for i in {1..32}; do \
        pkill -u nixbld$i 2>/dev/null || true; \
    done && \
    for i in {1..32}; do \
        userdel nixbld$i 2>/dev/null || true; \
    done && \
    groupdel nixbld 2>/dev/null || true && \
    rm -rf /var/empty/nixbld* /home/nixbld* 2>/dev/null || true && \
    groupadd -g 30000 nixbld && \
    for i in {1..32}; do \
        useradd -u $((30000 + i)) -g nixbld -d /var/empty -s /sbin/nologin nixbld$i; \
    done'

# Fix home directory ownership for the determinate-nix installation to overcome seccomp issue
RUN chown -R root:root /home/gitpod || true
# Install Nix
ENV NIX_CONFIG=$'filter-syscalls = false'
# RUN curl --proto '=https' --tlsv1.2 -L https://nixos.org/nix/install | sh -s -- --no-daemon \
RUN curl --proto '=https' --tlsv1.2 -sSf -L https://install.determinate.systems/nix | sh -s -- install linux --no-confirm --init none --no-start-daemon

# Fix home directory ownership back to gitpod
RUN chown -R gitpod:gitpod /home/gitpod

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user, zsh and zimfw

CMD ["/bin/bash", "-l"]
USER gitpod
ENV USER=gitpod
WORKDIR /home/gitpod
RUN echo '. /home/gitpod/.nix-profile/etc/profile.d/nix.sh' >> /home/gitpod/.bashrc \
  && echo '. /home/gitpod/.nix-profile/etc/profile.d/nix.sh' >> /home/gitpod/.zshrc \
  && mkdir -p /home/gitpod/.config/nixpkgs && echo '{ allowUnfree = true; }' >> /home/gitpod/.config/nixpkgs/config.nix \
  && mkdir -p /home/gitpod/.config/nix \
  && touch /home/gitpod/.config/nix/nix.conf \
  && echo "experimental-features = nix-command flakes" >> /home/gitpod/.config/nix/nix.conf
ENV PATH="${PATH}:/nix/var/nix/profiles/default/bin"

# xclip & nix develop
RUN echo "test -f ./sh_profile && source ./sh_profile" >> /home/gitpod/.bashrc \
    && echo "test -f ./sh_profile && source ./sh_profile" >> /home/gitpod/.zshrc

EXPOSE 5173
EXPOSE 3309
EXPOSE 5900
EXPOSE 6080
EXPOSE 8081
EXPOSE 9081
EXPOSE 9082
EXPOSE 5001
EXPOSE 2525
