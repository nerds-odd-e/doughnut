# syntax=docker.io/docker/dockerfile:1.7.0
FROM yeongsheng/doughnut-gitpod:2024-04-07

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user & cachix

CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

# Install cachix
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh
