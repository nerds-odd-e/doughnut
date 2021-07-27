/etc/hosts:
  file.append:
    - text: 10.111.16.12      db-server

/etc/profile.d/doughnut_env.sh:
  file.managed:
    - source: salt://base_os/templates/doughnut_env.sh
    - mode: 755

/etc/apt/sources.list.d/mysql.list:
  file.managed:
    - source: salt://base_os/templates/mysql.list
    - require_in:
        - pkg: doughnut-app-deps

mysql-deb-apt-repo:
  pkgrepo.managed:
    - humanname: MySQL
    - name: deb http://repo.mysql.com/apt/debian/ Buster mysql-8.0
    - dist: buster
    - file: /etc/apt/sources.list.d/mysql.list
    - keyid: 5072E1F5
    - keyserver: keyserver.ubuntu.com
    - require_in:
        - file: /etc/apt/sources.list.d/mysql.list

doughnut-app-deps:
  pkg.installed:
    - normalize: True
    - refresh: True
    - skip_verify: True
    - skip_suggestions: True
    - pkgs:
        - ca-certificates
        - openssl
        - gnupg
        - gnupg-agent
        - readline-common
        - jq
        - libmysqlclient21
        - mysql-community-client
        - google-cloud-packages-archive-keyring
        - google-cloud-sdk
    - require_in:
        - cmd: os-dist-upgrade
        - cmd: doughnut-jre
        - file: /etc/profile.d/doughnut_env.sh

zulu16.32.15-ca-jre16.0.2-linux_amd64.deb:
  file.managed:
    - name: /tmp/zulu16.32.15-ca-jre16.0.2-linux_amd64.deb
    - source: https://cdn.azul.com/zulu/bin/zulu16.32.15-ca-jre16.0.2-linux_amd64.deb
    - skip_verify: True
    - require_in:
        - cmd: install-jre


install-jre:
  cmd.run:
    - name: apt install -y /tmp/zulu16.32.15-ca-jre16.0.2-linux_amd64.deb
    - require_in:
        - cmd: doughnut-jre

doughnut-jre:
  cmd.run:
    - name: update-alternatives --set java /usr/lib/jvm/zre-16-amd64/bin/java

os-dist-upgrade:
  cmd.run:
    - name: apt -y update && apt -y dist-upgrade
