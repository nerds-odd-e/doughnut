FROM gitpod/workspace-full-vnc

USER root

# Install Cypress dependencies.
RUN sudo apt-get update \
 && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
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
   git-extras \
   unzip \
   wget \
   zip \
 && sudo rm -rf /var/lib/apt/lists/*

# Install Jetbrains Mono font
RUN wget https://download.jetbrains.com/fonts/JetBrainsMono-2.242.zip \
  && unzip JetBrainsMono-2.242.zip \
  && mv JetBrainsMono-*.ttf /usr/share/fonts/ \
  && mv JetBrainsMono-*.ttf ~/.local/share/fonts/

# Install zimfw & default to zsh
RUN curl -fsSL https://raw.githubusercontent.com/zimfw/install/master/install.zsh | zsh

# Install Nix
RUN addgroup --system nixbld \
  && adduser gitpod nixbld \
  && for i in $(seq 1 30); do useradd -ms /bin/bash nixbld$i &&  adduser nixbld$i nixbld; done \
  && mkdir -m 0755 /nix && chown gitpod /nix \
  && mkdir -p /etc/nix && echo 'sandbox = false' > /etc/nix/nix.conf
  
# Install Nix
CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

RUN touch .bash_profile \
 && curl https://nixos.org/releases/nix/nix-2.3.15/install | sh

RUN echo '. /home/gitpod/.nix-profile/etc/profile.d/nix.sh' >> /home/gitpod/.bashrc
RUN mkdir -p /home/gitpod/.bashrc.d
RUN mkdir -p /home/gitpod/.config/nixpkgs && echo '{ allowUnfree = true; }' >> /home/gitpod/.config/nixpkgs/config.nix

# Install cachix
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
  && nix-env -iA cachix -f https://cachix.org/api/v1/install \
  && cachix use cachix

# Install git
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
  && nix-env -i git git-lfs

# Install direnv
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
  && nix-env -i direnv \
  && direnv hook bash >> /home/gitpod/.bashrc

# Make zsh default
RUN sudo chsh -s /bin/zsh

# Install any-nix-shell & zimfw
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
  && nix-env -i any-nix-shell -f https://github.com/NixOS/nixpkgs/archive/master.tar.gz \
  && echo 'any-nix-shell zsh --info-right | . /dev/stdin' >> /home/gitpod/.zshrc \
  && . /home/.zshrc \
  && echo 'zmodule romkatv/powerlevel10k --use degit' >> /home/gitpod/.zimrc \
  && zimfw install
