traefik_app_dir:
  file.directory:
    - name: /opt/traefik
    - makedirs: True
    - require_in:
        - archive: traefik_app
        - file: /opt/traefik/traefik.toml
        - file: /opt/traefik/dynamic.toml

traefik_logs_dir:
  file.directory:
    - name: /opt/traefik/logs

/opt/traefik/traefik.toml:
  file.managed:
    - source: salt://traefik/templates/traefik.toml

/opt/traefik/dynamic.toml:
  file.managed:
    - source: salt://traefik/templates/dynamic.toml

ssl_dir:
  file.directory:
    - name: /etc/ssl/private
    - dir_mode: 640
    - file_mode: 640
    - recurse:
      - mode
    - require_in:
        - cmd: get_odde_ssl_pem
        - cmd: get_odde_ssl_key
        - cmd: get_odde_ssl_star_crt

get_odde_ssl_pem:
  cmd.run:
    - name: gsutil cp gs://dough-01/ssl/private/odde.pem /etc/ssl/private/odde.pem

get_odde_ssl_key:
  cmd.run:
    - name: gsutil cp gs://dough-01/ssl/private/odde.key /etc/ssl/private/odde.key

get_odde_ssl_star_crt:
  cmd.run:
    - name: gsutil cp gs://dough-01/ssl/private/star_odd-e_com.crt /etc/ssl/private/star_odd-e_com.crt

traefik_app:
  archive.extracted:
    - name: /opt/traefik
    - skip_verify: True
    - enforce_toplevel: False
    - keep_source: False
    - source: https://github.com/traefik/traefik/releases/download/v2.4.8/traefik_v2.4.8_linux_amd64.tar.gz
