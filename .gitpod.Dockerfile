# syntax=docker.io/docker/dockerfile:1.15.1
FROM yeongsheng/doughnut-gitpod:2025-04-24

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user
CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

# activate nix
ENV PATH="${PATH}:/nix/var/nix/profiles/default/bin"
RUN export PATH="${PATH}:/nix/var/nix/profiles/default/bin"
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
  # upgrade to lix
  && nix run \
  --experimental-features "nix-command flakes" \
  --extra-substituters https://cache.lix.systems --extra-trusted-public-keys "cache.lix.systems:aBnZUw8zA7H35Cz2RyKFVs3H4PlGTLawyY5KRbvJR8o=" \
  'git+https://git.lix.systems/lix-project/lix?ref=refs/tags/2.92.0' -- \
  upgrade-nix \
  --extra-substituters https://cache.lix.systems --extra-trusted-public-keys "cache.lix.systems:aBnZUw8zA7H35Cz2RyKFVs3H4PlGTLawyY5KRbvJR8o=" \
      && nix-env -iA nixpkgs.direnv && nix-env -iA nixpkgs.nix-direnv

# fasd
RUN echo 'eval "$(fasd --init auto)"' >> ~/.bashrc
RUN echo 'eval "$(fasd --init auto)"' >> ~/.zshrc

# direnv
RUN echo 'eval "$(direnv hook bash)"' >> ~/.bashrc
RUN echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
