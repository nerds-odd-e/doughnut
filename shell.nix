{ pkgs ? import (builtins.fetchGit {
  name = "nixos-21.05";
  url = "https://github.com/nixos/nixpkgs/";
  ref = "refs/heads/master";
  rev = "a87936871bd4edf966fd6d98d7d6ac96a3284b6c";
}) { } }:
with pkgs;
let
  inherit (pkgs) stdenv;
  allowConfig = config.nixpkgs.config {
    allowUnfree = true;
    allowUnsupportedSystem = true;
  };
  apple_sdk = darwin.apple_sdk.frameworks;
  nodejs = nodejs-16_x;
  intellij = jetbrains.idea-community;
in mkShell {
  name = "doughnut";
  MYSQL_HOME = builtins.getEnv "MYSQL_HOME";
  MYSQL_DATADIR = builtins.getEnv "MYSQL_DATADIR";
  buildInputs = [
    autoconf
    automake
    cmake
    coreutils-full
    gcc10
    gnumake
    libgccjit
    gradle
    nodejs
    python3
    yarn
    jdk16
    direnv
    nix-direnv
    any-nix-shell
    bash_5
    zsh
    zsh-powerlevel10k
    git
    git-secret
    gitAndTools.delta
    locale
    lsd
    platinum-searcher
    binutils-unwrapped
    hostname
    inetutils
    openssh
    pkg-config
    rsync
    autojump
    bat
    fasd
    fzf
    gnupg
    htop
    jq
    less
    lesspipe
    lsof
    lzma
    ncdu
    zoxide
    most
    ps
    pstree
    ripgrep
    tree
    vgrep
    unixtools.whereis
    which
    libmysqlclient
    libpcap
    libressl
    patchelf
    cacert
    curlie
    glances
    httpie
    mysql80
    mysql-client
    mysql_jdbc
    python39Packages.pip
    chromedriver
    geckodriver
    google-cloud-sdk
    packer
    dbeaver
    tmux
    tmuxPlugins.tmux-fzf
    vim
    vimpager
    vscodium
  ] ++ lib.optionals stdenv.isDarwin [
    darwin.apple_sdk.libs.utmp
    darwin.apple_sdk.libs.Xplugin
    apple_sdk.AppKit
    apple_sdk.AGL
    apple_sdk.ApplicationServices
    apple_sdk.AudioToolbox
    apple_sdk.AudioUnit
    apple_sdk.AVFoundation
    apple_sdk.Carbon
    apple_sdk.CoreAudio
    apple_sdk.CoreGraphics
    apple_sdk.CoreMedia
    apple_sdk.CoreVideo
    apple_sdk.Cocoa
    apple_sdk.CoreServices
    apple_sdk.CoreText
    apple_sdk.Foundation
    apple_sdk.ImageIO
    apple_sdk.IOKit
    apple_sdk.Kernel
    apple_sdk.MediaToolbox
    apple_sdk.OpenGL
    apple_sdk.QTKit
    apple_sdk.Security
    apple_sdk.SystemConfiguration
    xcodebuild
  ] ++ lib.optionals (!stdenv.isDarwin) [
    xvfb-run
    firefox
    google-chrome
    gitter
    intellij
    xclip
  ];
  shellHook = ''
        export NIXPKGS_ALLOW_UNFREE=1
        export JAVA_HOME="${pkgs.jdk16}"
        export GRADLE_HOME="${pkgs.gradle}"

        export MYSQL_BASEDIR=${pkgs.mysql80}
        export MYSQL_HOME="''${MYSQL_HOME:-$PWD/mysql}"
        export MYSQL_DATADIR="''${MYSQL_DATADIR:-$MYSQL_HOME/data}"
        export MYSQL_UNIX_SOCKET=$MYSQL_HOME/mysql.sock
        export MYSQLX_UNIX_SOCKET=$MYSQL_HOME/mysqlx.sock
        export MYSQL_PID_FILE=$MYSQL_HOME/mysql.pid
        export MYSQL_TCP_PORT=3309
        export MYSQLX_TCP_PORT=33090

        export PATH=$PATH:$JAVA_HOME/bin:$GRADLE_HOME/bin

        echo "################################################################################"
        echo "                                                                                "
        echo "##    !! DOUGHNUT NIX-SHELL !!      "
        echo "##    JAVA_HOME: $JAVA_HOME         "
        echo "##    GRADLE_HOME: $GRADLE_HOME     "
        echo "##    MYSQL_HOME: $MYSQL_HOME       "
        echo "##    MYSQL_DATADIR: $MYSQL_DATADIR "
        echo "                                                                                "
        echo "################################################################################"
        mkdir -p $MYSQL_HOME
        mkdir -p $MYSQL_DATADIR

    cat <<EOF > $MYSQL_HOME/init_doughnut_db.sql
    CREATE USER IF NOT EXISTS 'doughnut'@'localhost' IDENTIFIED BY 'doughnut';
    CREATE DATABASE IF NOT EXISTS doughnut_development DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS doughnut_test        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS doughnut_e2e_test    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    GRANT ALL PRIVILEGES ON doughnut_development.* TO 'doughnut'@'localhost';
    GRANT ALL PRIVILEGES ON doughnut_test.*        TO 'doughnut'@'localhost';
    GRANT ALL PRIVILEGES ON doughnut_e2e_test.*    TO 'doughnut'@'localhost';
    FLUSH PRIVILEGES;
    EOF

        export MYSQLD_PID=$(ps -ax | grep -v " grep " | grep mysqld | awk '{ print $1 }')
        if [[ -z "$MYSQLD_PID" ]]; then
          [ ! "$(ls -A mysql/data)" ] && mysqld --initialize-insecure --port=$MYSQL_TCP_PORT --user=`whoami` --datadir=$MYSQL_DATADIR --basedir=$MYSQL_BASEDIR --explicit_defaults_for_timestamp
          mysqld --datadir=$MYSQL_DATADIR --pid-file=$MYSQL_PID_FILE --port=$MYSQL_TCP_PORT --socket=$MYSQL_UNIX_SOCKET --mysqlx-socket=$MYSQLX_UNIX_SOCKET --mysqlx_port=$MYSQLX_TCP_PORT &
          export MYSQLD_PID=$!

          sleep 2 && mysql -u root -S $MYSQL_UNIX_SOCKET < $MYSQL_HOME/init_doughnut_db.sql
        fi

        export GPG_TTY=$(tty)
        export NIX_SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt
        if [[ "$OSTYPE" == "darwin"* ]]; then
           export NIX_SSL_CERT_FILE=/etc/ssl/cert.pem
        fi

        export GPG_TTY='(tty)'

        cleanup()
        {
          rm -f $MYSQL_HOME/init_doughnut_db.sql
          if [[ ! -z "$MYSQLD_PID" ]]; then
            mysqladmin -u root --socket=$MYSQL_UNIX_SOCKET shutdown
            wait $MYSQL_PID
          fi
        }
        trap cleanup EXIT
      '';
}
