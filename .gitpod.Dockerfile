# syntax=docker.io/docker/dockerfile:1.6
FROM yeongsheng/doughnut-gitpod:2023-09-26

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user, zsh and zimfw

CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

# Install cachix
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
    && nix-env -iA cachix -f https://cachix.org/api/v1/install \
    && cachix use cachix

# Install git
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
    && nix-env -i git


# atuin shell history
RUN curl https://raw.githubusercontent.com/ellie/atuin/main/install.sh | bash
