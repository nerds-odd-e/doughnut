# syntax=docker.io/docker/dockerfile:1.7.1
FROM yeongsheng/doughnut-gitpod:2024-05-22

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user
CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

# activate nix
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh

# atuin
RUN nix profile install "github:atuinsh/atuin"
RUN echo 'eval "$(atuin init bash)"' >> ~/.bashrc \
    && echo 'eval "$(atuin init zsh)"' >> ~/.zshrc
