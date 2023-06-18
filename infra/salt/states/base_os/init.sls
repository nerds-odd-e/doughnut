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
    - name: deb http://repo.mysql.com/apt/debian/ Bullseye mysql-8.0
    - dist: bullseye
    - file: /etc/apt/sources.list.d/mysql.list
    - keyid: 467B942D3A79BD29
    - keyserver: keyserver.ubuntu.com
    - require_in:
        - file: /etc/apt/sources.list.d/mysql.list

doughnut-app-deps:
  pkg.installed:
    - normalize: True
    - refresh: True
    - skip_verify: True
    - skip_suggestions: True
    - allow_updates: False
    - update_holds: True
    - pkgs:
        - ca-certificates
        - openssl
        - gnupg
        - gnupg-agent
        - readline-common
        - jq
        - libmysqlclient21
        - mysql-community-client
    - require_in:
        - cmd: os-dist-upgrade
        - cmd: doughnut-jre
        - file: /etc/profile.d/doughnut_env.sh
zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb:
  file.managed:
    - name: /tmp/zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb
    - source: https://cdn.azul.com/zulu/bin/zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb
    - skip_verify: True
    - require_in:
        - cmd: install-jre


install-jre:
  cmd.run:
    - name: apt-get install -y /tmp/zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb
    - require_in:
        - cmd: doughnut-jre

doughnut-jre:
  cmd.run:
    - name: update-alternatives --set java {{ pillar['doughnut_app']['java_home'] }}/bin/java

os-dist-upgrade:
  cmd.run:
    - name: apt-get -y update && apt-get -y dist-upgrade
