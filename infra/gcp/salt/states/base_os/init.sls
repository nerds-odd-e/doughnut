/etc/hosts:
  file.blockreplace:
    - marker_start: "# BEGIN SALT MANAGED ZONE"
    - marker_end: "# END SALT MANAGED ZONE"
    - content: |
        10.111.16.12      db-server
    - append_if_not_found: True
    - backup: '.bak'
    - show_changes: True

mysql-apt-config-install:
  cmd.run:
    - name: |
        export DEBIAN_FRONTEND=noninteractive
        apt-get update -qq
        apt-get install -y -qq debconf-utils wget
        wget -q -O /tmp/mysql-apt-config.deb https://dev.mysql.com/get/mysql-apt-config_0.8.35-1_all.deb || \
        wget -q -O /tmp/mysql-apt-config.deb https://repo.mysql.com/apt/debian/pool/mysql-apt-config/m/mysql-apt-config/mysql-apt-config_0.8.35-1_all.deb || \
        (curl -fsSL -o /tmp/mysql-apt-config.deb https://dev.mysql.com/get/mysql-apt-config_0.8.35-1_all.deb || curl -fsSL -o /tmp/mysql-apt-config.deb https://repo.mysql.com/apt/debian/pool/mysql-apt-config/m/mysql-apt-config/mysql-apt-config_0.8.35-1_all.deb)
        echo "mysql-apt-config mysql-apt-config/select-server select mysql-8.4-lts" | debconf-set-selections
        echo "mysql-apt-config mysql-apt-config/select-product select Ok" | debconf-set-selections
        dpkg -i /tmp/mysql-apt-config.deb || apt-get install -f -y

mysql-repo-refresh:
  cmd.run:
    - name: apt-get update
    - require:
      - cmd: mysql-apt-config-install

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
        - mysql-community-client
    - require:
      - cmd: mysql-repo-refresh

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
