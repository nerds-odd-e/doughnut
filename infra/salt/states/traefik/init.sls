traefik_app_dir:
  file.directory:
    - name: {{ pillar['traefik']['app_dir'] }}
    - makedirs: True
    - require_in:
        - archive: traefik_app_archive_download
        - file: traefik_static_conf
        - file: traefik_dynamic_conf

traefik_logs_dir:
  file.directory:
    - name: {{ pillar['traefik']['logs_dir'] }}

traefik_static_conf:
  file.managed:
    - name: {{ pillar['traefik']['static_conf'] }}
    - source: salt://traefik/templates/traefik.toml

traefik_dynamic_conf:
  file.managed:
    - name: {{ pillar['traefik']['dynamic_conf'] }}
    - source: salt://traefik/templates/dynamic.toml

ssl_dir:
  file.directory:
    - name: {{ pillar['traefik']['https_ssl_dir'] }}
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
    - name: gsutil cp {{ pillar['traefik']['gcp_bucket_ssl_store'] }}/odde.pem {{ pillar['traefik']['https_ssl_dir']}}/odde.pem

get_odde_ssl_key:
  cmd.run:
    - name: gsutil cp {{ pillar['traefik']['gcp_bucket_ssl_store'] }}/odde.key {{ pillar['traefik']['https_ssl_dir']}}/odde.key

get_odde_ssl_star_crt:
  cmd.run:
    - name: gsutil cp {{ pillar['traefik']['gcp_bucket_ssl_store'] }}/star_odd-e_com.crt {{ pillar['traefik']['https_ssl_dir']}}/star_odd-e_com.crt

traefik_app_archive_download:
  archive.extracted:
    - name: {{ pillar['traefik']['app_dir'] }}
    - skip_verify: True
    - enforce_toplevel: False
    - keep_source: False
    - source: {{ pillar['traefik']['download_url'] }}
