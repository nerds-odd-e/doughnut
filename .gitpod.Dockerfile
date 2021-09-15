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
#RUN <<EOF > /etc/mysql/mysql.conf.d/mysqld.cnf
RUN rm -f /etc/mysql/mysql.conf.d/mysqld.cnf && \
rm -f /etc/mysql/mysql.conf.d/client.cnf && \
rm -f /etc/mysql/mysql-bashrc-launch.sh

RUN echo "[mysqld_safe]" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "socket		= /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "nice		= 0" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "[mysqld]" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "user		= gitpod" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "pid-file	= /var/run/mysqld/mysqld.pid" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "socket		= /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "port		= 3309" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "basedir		= /usr" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "datadir		= /workspace/mysql" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "tmpdir		= /tmp" >>/etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "lc-messages-dir	= /usr/share/mysql" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "skip-external-locking" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "bind-address		= 127.0.0.1" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "key_buffer_size		= 16M" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "max_allowed_packet	= 16M" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "thread_stack		= 192K" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "thread_cache_size   = 8" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "myisam-recover-options  = BACKUP" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "general_log_file        = /var/log/mysql/mysql.log" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "general_log             = 1" >> /etc/mysql/mysql.conf.d/mysqld.cnf && \
echo "log_error               = /var/log/mysql/error.log" >> /etc/mysql/mysql.conf.d/mysqld.cnf

# Install default-login for MySQL clients
#COPY infra/gitpod/mysql/client.cnf /etc/mysql/mysql.conf.d/client.cnf
#RUN cat <<\EOF > /etc/mysql/mysql.conf.d/client.cnf
RUN echo "[client]" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "host     = localhost" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "user     = root" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "password =" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "socket   = /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "[mysql_upgrade]" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "host     = localhost" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "user     = root" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "password =" >> /etc/mysql/mysql.conf.d/client.cnf && \
echo "socket   = /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/client.cnf

#COPY infra/gitpod/mysql/mysql-bashrc-launch.sh /etc/mysql/mysql-bashrc-launch.sh
#RUN cat <<\EOF > /etc/mysql/mysql-bashrc-launch.sh
RUN echo "#!/bin/bash" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "\n" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "if [ ! -e /var/run/mysqld/gitpod-init.lock ]" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "then" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "    touch /var/run/mysqld/gitpod-init.lock" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "\n" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "    [ ! -d /workspace/mysql ] && mysqld --initialize-insecure" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "    [ ! -e /var/run/mysqld/mysqld.pid ] && mysqld --daemonize" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "    rm /var/run/mysqld/gitpod-init.lock" >> /etc/mysql/mysql-bashrc-launch.sh && \
echo "fi" >> /etc/mysql/mysql-bashrc-launch.sh

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
