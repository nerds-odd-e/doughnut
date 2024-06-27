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
    - name: deb http://repo.mysql.com/apt/debian/ Bookworm mysql-8.0
    - dist: bookworm
    - file: /etc/apt/sources.list.d/mysql.list
    - keyid: A8D3785C
    - keyserver: keyserver.ubuntu.com
    - require_in:
        - file: /etc/apt/sources.list.d/mysql.list

/etc/caddy/Caddyfile:
  file.managed:
    - source: salt://base_os/templates/Caddyfile
    - mode: 644
    - require_in:
        - service: caddy-service

caddy-repo-gpg-key:
  cmd.run:
    - name: curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    - unless: test -f /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    - require_in:
        - cmd: caddy-apt-source

caddy-apt-source:
  cmd.run:
    - name: curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
    - unless: test -f /etc/apt/sources.list.d/caddy-stable.list
    - require_in:
        - pkg: doughnut-app-deps

caddy-service:
  service.running:
    - name: caddy
    - enable: True
    - reload: True
    - watch:
        - file: /etc/caddy/Caddyfile

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
        - caddy
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
        - service: caddy-service
        
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
