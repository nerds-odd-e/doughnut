# syntax=docker.io/docker/dockerfile:1.8.0
FROM yeongsheng/doughnut-gitpod:2024-06-12

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

# direnv
RUN echo 'eval "$(direnv hook bash)"' >> ~/.bashrc
RUN echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc

# atuin
RUN curl -o install-atuin --proto '=https' --tlsv1.2 -sSf https://setup.atuin.sh \
    && chmod +x ./install-atuin \
    && ./install-atuin
RUN echo 'eval "$(atuin init bash)"' >> ~/.bashrc \
    && echo 'eval "$(atuin init zsh)"' >> ~/.zshrc
