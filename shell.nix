{ pkgs ? import <nixpkgs> { } }:
with pkgs;
let
  inherit (pkgs) stdenv;
  allowConfig = config.nixpkgs.config {
    allowUnfree=true;
    allowUnsupportedSystem=true;
  };
  apple_sdk = darwin.apple_sdk.frameworks;
  nodejs = nodejs-15_x;
  jdk = jdk11;
  intellij = jetbrains.idea-community;
in mkShell {
  name = "doughnut";
  MYSQL_HOME = builtins.getEnv "MYSQL_HOME";
  MYSQL_DATADIR = builtins.getEnv "MYSQL_DATADIR";
  buildInputs = [
    gradle nodejs yarn jdk python3 zsh zsh-powerlevel10k
    any-nix-shell autoconf automake coreutils-full gcc gnumake gnupg
    git git-secret gitAndTools.delta locale lsd platinum-searcher most
    binutils-unwrapped hostname inetutils openssh pkg-config rsync
    bat duf fasd fzf htop jq lsof lzma progress ripgrep tree wget which
    libmysqlclient libpcap libressl
    cacert curlie glances httpie mariadb python38Packages.pip
    chromedriver geckodriver google-cloud-sdk
    vim vimPlugins.nerdtree vimPlugins.nvimdev-nvim vimPlugins.spacevim vscodium
  ] ++ lib.optionals stdenv.isDarwin [
    darwin.apple_sdk.libs.utmp darwin.apple_sdk.libs.Xplugin
    apple_sdk.AppKit apple_sdk.AGL apple_sdk.ApplicationServices apple_sdk.AudioToolbox
    apple_sdk.AudioUnit apple_sdk.AVFoundation apple_sdk.Carbon apple_sdk.CoreAudio
    apple_sdk.CoreGraphics apple_sdk.CoreMedia apple_sdk.CoreVideo apple_sdk.Cocoa apple_sdk.CoreServices apple_sdk.CoreText
    apple_sdk.Foundation apple_sdk.ImageIO apple_sdk.IOKit apple_sdk.Kernel apple_sdk.MediaToolbox apple_sdk.OpenGL
    apple_sdk.QTKit apple_sdk.Security apple_sdk.SystemConfiguration xcodebuild
  ] ++ lib.optionals (!stdenv.isDarwin) [
    chromium firefox google-chrome intellij
  ];
  shellHook = ''
    export JAVA_HOME="${pkgs.jdk}"
    export PATH=$PATH:$JAVA_HOME/bin
    export MYSQL_BASEDIR=${pkgs.mariadb}
    export MYSQL_HOME="''${MYSQL_HOME:-''$PWD/mysql}"
    export MYSQL_DATADIR="''${MYSQL_DATADIR:-''$MYSQL_HOME/data}"

    export MYSQL_UNIX_PORT=$MYSQL_HOME/mysql.sock
    export MYSQL_PID_FILE=$MYSQL_HOME/mysql.pid

    # to import environment variables defined in env.sh
    set -a
    source env.sh
    set +a

    echo "#######################################################################"
    echo ">>>>> MYSQL_HOME: $MYSQL_HOME "
    echo ">>>>> MYSQL_DATADIR: $MYSQL_DATADIR "
    echo "#######################################################################"
    
    mariadb-install-db --datadir=$MYSQL_DATADIR --basedir=$MYSQL_BASEDIR --pid-file=$MYSQL_PID_FILE
    mariadbd --datadir=$MYSQL_DATADIR --pid-file=$MYSQL_PID_FILE --socket=$MYSQL_UNIX_PORT &
    export MYSQL_PID=$!

cat <<EOF > backend/src/main/resources/db/init_doughnut_db.sql
CREATE DATABASE IF NOT EXISTS doughnut_development DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS doughnut_test        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SET PASSWORD FOR 'doughnut'@'localhost' = PASSWORD('doughnut');
GRANT ALL PRIVILEGES ON doughnut_development.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_test.*        TO 'doughnut'@'localhost';
FLUSH PRIVILEGES;
EOF

    export GPG_TTY=$(tty)
    export NIX_SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt
    if [[ "$OSTYPE" == "darwin"* ]]; then
       export NIX_SSL_CERT_FILE=/etc/ssl/cert.pem
    fi

    sleep 3s
    mysql < backend/src/main/resources/db/init_doughnut_db.sql
    export GPG_TTY='(tty)'

    cleanup()
    {
      mariadb-admin --socket=$MYSQL_UNIX_PORT shutdown
      wait $MYSQL_PID
      kill -9 $MYSQL_PID
    }
    trap cleanup EXIT
  '';
}
