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
    - name: deb http://repo.mysql.com/apt/debian/ buster mysql-8.0
    - dist: buster
    - file: /etc/apt/sources.list.d/mysql.list
    - keyid: 8C718D3B5072E1F5
    - keyserver: keys.gnupg.net
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
        - openjdk-11-jre
        - libmysqlclient21
        - mysql-community-client
        - google-cloud-packages-archive-keyring
        - google-cloud-sdk
    - require_in:
        - cmd: os-dist-upgrade
        - cmd: doughnut-jre
        - file: /etc/profile.d/doughnut_env.sh

doughnut-jre:
  cmd.run:
    - name: update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java

os-dist-upgrade:
  cmd.run:
    - name: apt-get -y update && apt-get -y dist-upgrade
