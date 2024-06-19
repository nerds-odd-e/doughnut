# syntax=docker.io/docker/dockerfile:1.8.1
FROM yeongsheng/doughnut-gitpod:2024-06-19

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

# fasd
RUN echo 'eval "$(fasd --init auto)"' >> ~/.bashrc
RUN echo 'eval "$(fasd --init auto)"' >> ~/.zshrc

# direnv
RUN echo 'eval "$(direnv hook bash)"' >> ~/.bashrc
RUN echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
