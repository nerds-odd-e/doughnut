# syntax=docker/dockerfile:1.3-labs
FROM registry.jetbrains.team/p/prj/containers/projector-idea-c
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
    htop \
    lsof \
    net-tools \
    git-extras \
    unzip \
    wget \
    zip \
    bash-completion \
    procps \
    gnupg \
    curl

# Install MySQL DB
# RUN wget https://dev.mysql.com/get/mysql-apt-config_0.8.19-1_all.deb ./ \
#     && dpkg -i ./mysql-apt-config_0.8.19-1_all.deb \
#     && apt-get update \
#     && DEBIAN_FRONTEND=noninteractive apt-get install -y \
#     && apt-get install mysql-server libmysqlclient21 \
#     && mkdir -p /var/run/mysqld /var/log/mysql \
#     && chown -R projector-user:projector-user /etc/mysql /var/run/mysqld /var/log/mysql /var/lib/mysql /var/lib/mysql-files /var/lib/mysql-keyring /var/lib/mysql-upgrade \
#     && rm -rf /var/lib/apt/lists/* \
#     && rm -rf /var/cache/apt \
#     && rm -f /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && rm -f /etc/mysql/mysql.conf.d/client.cnf

# Install our own MySQL config
# RUN echo "[mysqld_safe]" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "socket		= /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "nice		= 0" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "[mysqld]" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "user		= gitpod" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "pid-file	= /var/run/mysqld/mysqld.pid" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "socket		= /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "port		= 3309" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "basedir		= /usr" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "datadir		= /workspace/mysql" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "tmpdir		= /tmp" >>/etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "lc-messages-dir	= /usr/share/mysql" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "skip-external-locking" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "bind-address		= 127.0.0.1" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "key_buffer_size		= 16M" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "max_allowed_packet	= 16M" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "thread_stack		= 192K" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "thread_cache_size   = 8" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "myisam-recover-options  = BACKUP" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "\n" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "general_log_file        = /var/log/mysql/mysql.log" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "general_log             = 1" >> /etc/mysql/mysql.conf.d/mysqld.cnf \
#     && echo "log_error               = /var/log/mysql/error.log" >> /etc/mysql/mysql.conf.d/mysqld.cnf

# Install default-login for MySQL clients
# RUN echo "[client]" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "host     = localhost" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "user     = root" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "password =" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "socket   = /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "[mysql_upgrade]" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "host     = localhost" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "user     = root" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "password =" >> /etc/mysql/mysql.conf.d/client.cnf \
#     && echo "socket   = /var/run/mysqld/mysqld.sock" >> /etc/mysql/mysql.conf.d/client.cnf

# -----------------------------------------------------
# ---------------- USER projector-user ----------------
# -----------------------------------------------------

# Setup gitpod workspace user 'projector-user'

CMD /bin/bash -l
USER projector-user
ENV USER projector-user
WORKDIR /home/projector-user

RUN mkdir -p /home/projector-user/.bashrc.d

# RUN echo "if [ ! -e /var/run/mysqld/gitpod-init.lock ]" >> /home/projector-user/.bashrc \
#     && echo "then" >> /home/projector-user/.bashrc \
#     && echo "  touch /var/run/mysqld/gitpod-init.lock" >> /home/projector-user/.bashrc \
#     && echo "  [ ! -d /workspace/mysql ] && mysqld --initialize-insecure" >> /home/projector-user/.bashrc \
#     && echo "  [ ! -e /var/run/mysqld/mysqld.pid ] && mysqld --daemonize" >> /home/projector-user/.bashrc \
#     && echo "  rm /var/run/mysqld/gitpod-init.lock" >> /home/projector-user/.bashrc \
#     && echo "fi" >> /home/projector-user/.bashrc

EXPOSE 3000
EXPOSE 3309
EXPOSE 5900
EXPOSE 6080
EXPOSE 8081
EXPOSE 9081
EXPOSE 9082
EXPOSE 8887
