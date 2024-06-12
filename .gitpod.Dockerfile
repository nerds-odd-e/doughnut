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
RUN DEBIAN_FRONTEND=noninteractive sudo apt-get install -y direnv
RUN echo 'eval "$(direnv hook bash)"' >> ~/.bashrc
RUN echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
RUN cd /workspace/doughnut && direnv allow

