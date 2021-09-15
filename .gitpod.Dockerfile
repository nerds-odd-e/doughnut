FROM gitpod/workspace-full-vnc

# -------------------- USER root --------------------
USER root

# Install Cypress dependencies.
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y \
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
# Nice to haves
   zsh \
   bat \
   htop \
   lsof \
   net-tools \
   git-extras \
   unzip \
   wget \
   zip \
   bash-completion \
   procps \
   powerline \
   fonts-powerline \
 # clean apt to reduce image size:
 && rm -rf /var/lib/apt/lists/* \
 && rm -rf /var/cache/apt

# Install MySQL DB
RUN install-packages mysql-server \
 && mkdir -p /var/run/mysqld /var/log/mysql \
 && chown -R gitpod:gitpod /etc/mysql /var/run/mysqld /var/log/mysql /var/lib/mysql /var/lib/mysql-files /var/lib/mysql-keyring /var/lib/mysql-upgrade

# Install our own MySQL config
#COPY infra/gitpod/mysql/mysql.cnf /etc/mysql/mysql.conf.d/mysqld.cnf
RUN cat <<\EOF > /etc/mysql/mysql.conf.d/mysqld.cnf
[mysqld_safe]
socket		= /var/run/mysqld/mysqld.sock
nice		= 0

[mysqld]
user		= gitpod
pid-file	= /var/run/mysqld/mysqld.pid
socket		= /var/run/mysqld/mysqld.sock
port		= 3309
basedir		= /usr
datadir		= /workspace/mysql
tmpdir		= /tmp
lc-messages-dir	= /usr/share/mysql
skip-external-locking
bind-address		= 127.0.0.1

key_buffer_size		= 16M
max_allowed_packet	= 16M
thread_stack		= 192K
thread_cache_size   = 8

myisam-recover-options  = BACKUP

general_log_file        = /var/log/mysql/mysql.log
general_log             = 1
log_error               = /var/log/mysql/error.log
EOF

# Install default-login for MySQL clients
#COPY infra/gitpod/mysql/client.cnf /etc/mysql/mysql.conf.d/client.cnf
RUN cat <<\EOF > /etc/mysql/mysql.conf.d/client.cnf
[client]
host     = localhost
user     = root
password =
socket   = /var/run/mysqld/mysqld.sock
[mysql_upgrade]
host     = localhost
user     = root
password =
socket   = /var/run/mysqld/mysqld.sock
EOF

#COPY infra/gitpod/mysql/mysql-bashrc-launch.sh /etc/mysql/mysql-bashrc-launch.sh
RUN cat <<\EOF > /etc/mysql/mysql-bashrc-launch.sh
#!/bin/bash

# this script is intended to be called from .bashrc
# This is a workaround for not having something like supervisord

if [ ! -e /var/run/mysqld/gitpod-init.lock ]
then
    touch /var/run/mysqld/gitpod-init.lock

    # initialize database structures on disk, if needed
    [ ! -d /workspace/mysql ] && mysqld --initialize-insecure

    # launch database, if not running
    [ ! -e /var/run/mysqld/mysqld.pid ] && mysqld --daemonize

    rm /var/run/mysqld/gitpod-init.lock
fi
EOF

# Install Jetbrains Mono font
RUN wget https://download.jetbrains.com/fonts/JetBrainsMono-2.242.zip \
  && unzip JetBrainsMono-2.242.zip \
  && cp fonts/ttf/JetBrainsMono-*.ttf /usr/share/fonts/ \
  && mkdir -p /home/gitpod/.local/share/fonts/ \
  && cp fonts/ttf/JetBrainsMono-*.ttf /home/gitpod/.local/share/fonts/ \
  && rm -rf fonts

# Install Nix
RUN addgroup --system nixbld \
  && adduser gitpod nixbld \
  && for i in $(seq 1 30); do useradd -ms /bin/bash nixbld$i &&  adduser nixbld$i nixbld; done \
  && mkdir -m 0755 /nix && chown gitpod /nix \
  && mkdir -p /etc/nix && echo 'sandbox = false' > /etc/nix/nix.conf
  
# -------------------- USER gitpod --------------------
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

# Make zsh default && install zimfw
RUN curl -fsSL https://raw.githubusercontent.com/zimfw/install/master/install.zsh | zsh \
  && echo 'zmodule romkatv/powerlevel10k --use degit' >> /home/gitpod/.zimrc \
  && zsh ~/.zim/zimfw.zsh install


# Install any-nix-shell & zimfw
RUN . /home/gitpod/.nix-profile/etc/profile.d/nix.sh \
  && nix-env -i any-nix-shell -f https://github.com/NixOS/nixpkgs/archive/master.tar.gz \
  && echo 'any-nix-shell zsh --info-right | . /dev/stdin' >> /home/gitpod/.zshrc

# MySQL bash launch in docker container instance without resorting to supervisord
RUN echo "/etc/mysql/mysql-bashrc-launch.sh" >> ~/.bashrc
