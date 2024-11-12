/etc/hosts:
  file.blockreplace:
    - marker_start: "# BEGIN SALT MANAGED ZONE"
    - marker_end: "# END SALT MANAGED ZONE"
    - content: |
        10.111.16.12      db-server
    - append_if_not_found: True
    - backup: '.bak'
    - show_changes: True

mysql-repo-key:
  file.managed:
    - name: /usr/share/keyrings/mysql-archive-keyring.gpg
    - source: https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
    - skip_verify: True
    - mode: 644
    - user: root
    - group: root

mysql-repo:
  pkgrepo.managed:
    - humanname: MySQL
    - name: deb [signed-by=/usr/share/keyrings/mysql-archive-keyring.gpg] http://repo.mysql.com/apt/debian bookworm mysql-8.0
    - file: /etc/apt/sources.list.d/mysql.list
    - key_url: https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
    - aptkey: False
    - gpgcheck: 1
    - require:
      - file: mysql-repo-key

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
    - require:
      - pkgrepo: mysql-repo

/etc/profile.d/doughnut_env.sh:
  file.managed:
    - source: salt://base_os/templates/doughnut_env.sh
    - template: jinja
    - mode: 755
    - require:
      - pkg: doughnut-app-deps

zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb:
  file.managed:
    - name: /tmp/zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb
    - source: https://cdn.azul.com/zulu/bin/zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb
    - skip_verify: True

install-jre:
  cmd.run:
    - name: apt-get install -y /tmp/zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb
    - require:
      - file: zulu{{ pillar['doughnut_app']['jre_version'] }}-linux_amd64.deb

doughnut-jre:
  cmd.run:
    - name: update-alternatives --set java {{ pillar['doughnut_app']['java_home'] }}/bin/java
    - require:
      - cmd: install-jre

os-dist-upgrade:
  cmd.run:
    - name: apt-get -y update && apt-get -y dist-upgrade
    - require:
      - pkg: doughnut-app-deps
