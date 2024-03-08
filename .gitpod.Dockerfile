# syntax=docker.io/docker/dockerfile:1.7.0
FROM yeongsheng/doughnut-gitpod:2024-03-08

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user, zsh, cachix & atuin

CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

# Install cachix
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh

# atuin shell history
RUN curl https://raw.githubusercontent.com/ellie/atuin/main/install.sh | bash
