FROM gitpod/workspace-full-vnc
# ---------------------------------------------------
# -------------------- USER root --------------------
# ---------------------------------------------------

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
   snapd \
  && snap install core \
  && snap install lsd \
  && rm -rf /var/lib/apt/lists/* \
  && rm -rf /var/cache/apt

# Install MySQL DB
RUN install-packages mysql-server \
  && mkdir -p /var/run/mysqld /var/log/mysql \
  && chown -R gitpod:gitpod /etc/mysql /var/run/mysqld /var/log/mysql /var/lib/mysql /var/lib/mysql-files /var/lib/mysql-keyring /var/lib/mysql-upgrade

# Install our own MySQL config
RUN rm -f /etc/mysql/mysql.conf.d/mysqld.cnf \
  && rm -f /etc/mysql/mysql.conf.d/client.cnf

RUN echo "[mysqld_safe]" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "socket		= /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "nice		= 0" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "[mysqld]" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "user		= gitpod" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "pid-file	= /var/run/mysqld/mysqld.pid" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "socket		= /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "port		= 3309" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "basedir		= /usr" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "datadir		= /workspace/mysql" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "tmpdir		= /tmp" >>/etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "lc-messages-dir	= /usr/share/mysql" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "skip-external-locking" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "bind-address		= 127.0.0.1" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "key_buffer_size		= 16M" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "max_allowed_packet	= 16M" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "thread_stack		= 192K" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "thread_cache_size   = 8" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "myisam-recover-options  = BACKUP" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "general_log_file        = /var/log/mysql/mysql.log" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "general_log             = 1" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
  && echo "log_error               = /var/log/mysql/error.log" >> /etc/mysql/mysql.conf.d/mysqld.cnf

# Install default-login for MySQL clients
RUN echo "[client]" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "host     = localhost" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "user     = root" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "password =" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "socket   = /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "[mysql_upgrade]" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "host     = localhost" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "user     = root" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "password =" >> /etc/mysql/mysql.conf.d/client.cnf \
  && echo "socket   = /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/client.cnf

# Install Jetbrains Mono font
RUN wget https://download.jetbrains.com/fonts/JetBrainsMono-2.242.zip \
  && unzip JetBrainsMono-2.242.zip \
  && cp fonts/ttf/JetBrainsMono-*.ttf /usr/share/fonts/ \
  && mkdir -p /home/gitpod/.local/share/fonts/ \
  && cp fonts/ttf/JetBrainsMono-*.ttf /home/gitpod/.local/share/fonts/ \
  && rm -rf fonts

# -----------------------------------------------------
# -------------------- USER gitpod --------------------
# -----------------------------------------------------

# Setup gitpod workspace user, zsh and zimfw

CMD /bin/bash -l
USER gitpod
ENV USER gitpod
WORKDIR /home/gitpod

RUN mkdir -p /home/gitpod/.bashrc.d

# Make zsh default && install zimfw
RUN curl -fsSL https://raw.githubusercontent.com/zimfw/install/master/install.zsh | zsh \
  && echo 'zmodule romkatv/powerlevel10k --use degit' >> /home/gitpod/.zimrc \
  && zsh ~/.zim/zimfw.zsh install \
  && echo "UNSET DISPLAY" >> /home/gitpod/.zshrc

EXPOSE 3000
EXPOSE 3309
EXPOSE 5900
EXPOSE 6080
EXPOSE 8081
EXPOSE 9081
EXPOSE 9082
