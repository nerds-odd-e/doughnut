traefik:
  version: v2.4.8
  arch: linux_amd64
  download_url: "https://github.com/traefik/traefik/releases/download/v2.4.8/traefik_v2.4.8_linux_amd64.tar.gz"
  app_dir: /opt/traefik
  static_conf: /opt/traefik/traefik.toml
  dynamic_conf: /opt/traefik/dynamic.toml
  logs_dir: /opt/traefik/logs
  https_ssl_dir: /etc/ssl/private
