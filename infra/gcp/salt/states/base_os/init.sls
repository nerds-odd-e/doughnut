/etc/hosts:
  file.append:
    - text: 10.111.16.12      db-server

/etc/profile.d/doughnut_env.sh:
  file.managed:
    - source: salt://base_os/templates/doughnut_env.sh
    - template: jinja
    - mode: 755

mysql-repo:
  pkgrepo.managed:
    - humanname: MySQL
    - name: deb [signed-by=/usr/share/keyrings/mysql-archive-keyring.gpg] http://repo.mysql.com/apt/debian bookworm mysql-8.0
    - file: /etc/apt/sources.list.d/mysql.list
    - key_url: https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
    - aptkey: False
    - gpgcheck: 1
    - require_in:
      - pkg: doughnut-app-deps

mysql-repo-key:
  file.managed:
    - name: /usr/share/keyrings/mysql-archive-keyring.gpg
    - source: https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
    - skip_verify: True
    - mode: 644
    - user: root
    - group: root
    - require_in:
      - pkgrepo: mysql-repo

doughnut-app-deps:
  pkg.installed:
    - normalize: True
    - refresh: True
    - skip_verify: True
    - skip_suggestions: True
    - allow_updates: False
    - update_holds: True
    - pkgs:
        - apt-transport-https
        - ca-certificates
        - debian-keyring
        - debian-archive-keyring
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
