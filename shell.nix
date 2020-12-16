{ pkgs ? import <nixpkgs> {} }:
with pkgs;
with stdenv;
let
  apple_sdk = darwin.apple_sdk.frameworks;
  nodejs = nodejs-15_x;
in mkDerivation {
  name = "doughnut";
  MYSQL_HOME = builtins.getEnv "MYSQL_HOME";
  MYSQL_DATADIR = builtins.getEnv "MYSQL_DATADIR";
  buildInputs = [
    nodejs yarn jdk14 gradle 
    autoconf automake coreutils-full gcc gnumake gnupg
    git git-secret gitAndTools.delta
    binutils-unwrapped pkg-config
    bat curl fasd fzf htop jq lzma time vim wget which
    libmysqlclient libpcap libressl zsh
    cacert mariadb docker glances sops
    chromedriver geckodriver
  ] ++ lib.optionals isDarwin [
    darwin.apple_sdk.libs.utmp darwin.apple_sdk.libs.Xplugin
    apple_sdk.AppKit apple_sdk.AGL apple_sdk.ApplicationServices apple_sdk.AudioToolbox
    apple_sdk.AudioUnit apple_sdk.AVFoundation apple_sdk.Carbon apple_sdk.CoreAudio
    apple_sdk.CoreGraphics apple_sdk.CoreMedia apple_sdk.CoreVideo apple_sdk.Cocoa apple_sdk.CoreServices apple_sdk.CoreText
    apple_sdk.Foundation apple_sdk.ImageIO apple_sdk.IOKit apple_sdk.Kernel apple_sdk.MediaToolbox apple_sdk.OpenGL
    apple_sdk.QTKit apple_sdk.Security apple_sdk.SystemConfiguration xcodebuild
  ] ++ lib.optionals (!isDarwin) [
    chromium jetbrains.idea-community
  ];
  shellHook = ''
    export JAVA_HOME="${pkgs.jdk14}"
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

cat <<EOF > $MYSQL_HOME/init_doughnut_db.sql
CREATE DATABASE /*!32312 IF NOT EXISTS*/ doughnut_development /*!40100 DEFAULT CHARACTER SET utf8 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/ doughnut_test /*!40100 DEFAULT CHARACTER SET utf8 */;

CREATE USER IF NOT EXISTS 'doughnut'@'localhost' IDENTIFIED BY 'doughnut';
GRANT ALL PRIVILEGES ON doughnut_development.* TO 'doughnut'@'localhost';
GRANT ALL PRIVILEGES ON doughnut_test.* TO 'doughnut'@'localhost';
EOF

    export NIX_SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt
    if [[ "$OSTYPE" == "darwin"* ]]; then
       export NIX_SSL_CERT_FILE=/etc/ssl/cert.pem
    fi

    sleep 3s
    mysql < $MYSQL_HOME/init_doughnut_db.sql

    cleanup()
    {
      rm -f $MYSQL_HOME/init_doughnut_db.sql
      mariadb-admin --socket=$MYSQL_UNIX_PORT shutdown
      wait $MYSQL_PID
      kill -9 $MYSQL_PID
    }
    trap cleanup EXIT
  '';
}
