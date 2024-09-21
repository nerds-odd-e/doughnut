{ pkgs ? import <nixpkgs> { } }:
with pkgs;
let
  inherit (pkgs) stdenv;
  apple_sdk = darwin.apple_sdk.frameworks;
in mkShell {
  name = "doughnut";
  MYSQL_HOME = builtins.getEnv "MYSQL_HOME";
  MYSQL_DATADIR = builtins.getEnv "MYSQL_DATADIR";
  buildInputs = [
    python312
    poetry
    nodejs_22
    corepack_22
    zsh
    jdk22
    git
    git-secret
    gitleaks
    jq
    libmysqlclient
    mysql80
    mysql-client
    mysql_jdbc
    google-cloud-sdk
    yamllint
    nixfmt-classic
    hclfmt
  ] ++ lib.optionals stdenv.isDarwin [ sequelpro ]
    ++ lib.optionals (!stdenv.isDarwin) [
      sequeler
      ungoogled-chromium
      psmisc
      x11vnc
      xclip
      xvfb-run
  ];
  shellHook = ''
    #!/usr/bin/env bash

    # Deactivate nvm if exists
    command -v nvm >/dev/null 2>&1 && { nvm deactivate; }

    export PS1="(nix)$PS1"
    export GPG_TTY=$(tty)
    export JAVA_HOME="$(readlink -e $(type -p javac) | sed  -e 's/\/bin\/javac//g')"
    export PNPM_HOME="$(readlink -e $(type -p pnpm) | sed -e 's/\/bin\/pnpm//g')"
    export NODE_PATH="$(readlink -e $(type -p node) | sed  -e 's/\/bin\/node//g')"
    export PYTHON_PATH="$(readlink -e $(type -p python) | sed  -e 's/\/bin\/python//g')"
    export POETRY_PATH="$(readlink -e $(type -p poetry) | sed  -e 's/\/bin\/poetry//g')"
    export PUB_CACHE="''${PUB_CACHE:-$PWD/.pub-cache}"
    export OPENAI_API_TOKEN="''${AI_TOKEN}"

    export MYSQL_BASEDIR=${pkgs.mysql80}
    export MYSQL_HOME="''${MYSQL_HOME:-$PWD/mysql}"
    export MYSQL_DATADIR="''${MYSQL_DATADIR:-$MYSQL_HOME/data}"
    export MYSQL_UNIX_SOCKET=$MYSQL_HOME/mysql.sock
    export MYSQLX_UNIX_SOCKET=$MYSQL_HOME/mysqlx.sock
    export MYSQL_PID_FILE=$MYSQL_HOME/mysql.pid
    export MYSQL_TCP_PORT=3309
    export MYSQLX_TCP_PORT=33090
    export LANG="en_US.UTF-8"
    export SOURCE_REPO_NAME="''${PWD##*/}"
    export PATH=$JAVA_HOME/bin::$NODE_PATH/bin:$PNPM_HOME/bin:$MYSQL_BASEDIR/bin:$PATH

    echo "###################################################################################################################"
    echo "                                                                                "
    echo "##   !! $SOURCE_REPO_NAME NIX DEVELOPMENT ENVIRONMENT !!"
    echo "##   NIX VERSION: `nix --version`                       "
    echo "##   JAVA_HOME: $JAVA_HOME                              "
    echo "##   NODE_PATH: $NODE_PATH                              "
    echo "##   PNPM_HOME: $PNPM_HOME                              "
    echo "##   PYTHON_PATH: $PYTHON_PATH                          "
    echo "##   POETRY_PATH: $POETRY_PATH                          "
    echo "##   MYSQL_BASEDIR: $MYSQL_BASEDIR                      "
    echo "##   MYSQL_HOME: $MYSQL_HOME                            "
    echo "##   MYSQL_DATADIR: $MYSQL_DATADIR                      "
    echo "##   JAVA VERSION: `javac --version`                    "
    echo "##   NODE VERSION: `node --version`                     "
    echo "##   PNPM VERSION: `pnpm --version`                     "
    echo "##   BIOME VERSION: `pnpm biome --version`              "
    echo "##   PYTHON VERSION: `python --version`                 "
    echo "##   POETRY VERSION: `poetry --version`                 "
    echo "                                                                                "
    echo "###################################################################################################################"

    mkdir -p $MYSQL_HOME
    mkdir -p $MYSQL_DATADIR

    cat <<EOF > $MYSQL_HOME/init_doughnut_db.sql
    CREATE USER IF NOT EXISTS 'doughnut'@'localhost' IDENTIFIED BY 'doughnut';
    CREATE USER IF NOT EXISTS 'doughnut'@'127.0.0.1' IDENTIFIED BY 'doughnut';
    CREATE DATABASE IF NOT EXISTS doughnut_development DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS doughnut_test        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS doughnut_e2e_test    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    GRANT ALL PRIVILEGES ON doughnut_development.* TO 'doughnut'@'localhost';
    GRANT ALL PRIVILEGES ON doughnut_test.*        TO 'doughnut'@'localhost';
    GRANT ALL PRIVILEGES ON doughnut_e2e_test.*    TO 'doughnut'@'localhost';
    GRANT ALL PRIVILEGES ON doughnut_development.* TO 'doughnut'@'127.0.0.1';
    GRANT ALL PRIVILEGES ON doughnut_test.*        TO 'doughnut'@'127.0.0.1';
    GRANT ALL PRIVILEGES ON doughnut_e2e_test.*    TO 'doughnut'@'127.0.0.1';
    FLUSH PRIVILEGES;
    EOF

    export MYSQLD_PID=$(ps -ax | grep -v " grep " | grep mysqld | awk '{ print $1 }')
    if [[ -z "$MYSQLD_PID" ]]; then
      [ ! "$(ls -A mysql/data)" ] && mysqld --initialize-insecure --port=$MYSQL_TCP_PORT --user=`whoami` --datadir=$MYSQL_DATADIR --tls-version=TLSv1.2 --basedir=$MYSQL_BASEDIR --explicit_defaults_for_timestamp
      mysqld --datadir=$MYSQL_DATADIR --pid-file=$MYSQL_PID_FILE --port=$MYSQL_TCP_PORT --socket=$MYSQL_UNIX_SOCKET --mysqlx-socket=$MYSQLX_UNIX_SOCKET --mysqlx_port=$MYSQLX_TCP_PORT --tls-version=TLSv1.2 &
      export MYSQLD_PID=$!

      sleep 6 && mysql -u root -S $MYSQL_UNIX_SOCKET < $MYSQL_HOME/init_doughnut_db.sql
    fi

    if [[ "$USER" = @(codespace|gitpod) ]]; then
      [[ -d $HOME/.cache/Cypress ]] || pnpx cypress install --force
    fi

    corepack prepare pnpm@9.11.0 --activate
    corepack use pnpm@9.11.0
    pnpm --frozen-lockfile recursive install
    # start biome daemon-server
    pnpm biome stop && pnpm biome start
  '';
}
