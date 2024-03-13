#!/bin/bash

# Set the metadata server to the get projct id
PROJECTID=$(gcloud config get-value project)
BUCKET="dough-01"
ARCH="linux_amd64"
TRAEFIK_VERSION="v2.4.8"

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

# Get the files we need
mkdir -p /opt/doughnut_app
mkdir -p /etc/ssl/private
gsutil cp gs://"${BUCKET}/ssl/private/odde.pem" /etc/ssl/private/odde.pem
gsutil cp gs://"${BUCKET}/ssl/private/odde.key" /etc/ssl/private/odde.key
gsutil cp gs://"${BUCKET}/ssl/private/star_odd-e_com.crt" /etc/ssl/private/star_odd-e_com.crt
chmod 640 -R /etc/ssl/private/*

cat <<'EOF' > /etc/apt/sources.list.d/mysql.list
deb http://repo.mysql.com/apt/debian/ buster mysql-apt-config
deb http://repo.mysql.com/apt/debian/ buster mysql-8.0
deb http://repo.mysql.com/apt/debian/ buster mysql-tools
deb-src http://repo.mysql.com/apt/debian/ buster mysql-8.0
EOF

# Install dependencies
apt-key adv --keyserver hkp://pool.sks-keyservers.net:80 --recv-keys 8C718D3B5072E1F5
apt-get -y update && apt-get upgrade && apt-get dist-upgrade
apt-get -y install jq openjdk-17-jre gnupg gnupg-agent libmysqlclient21 mysql-community-client ca-certificates openssl readline-common
apt-get -y autoremove

# Make Java 17 default
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin
update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java

# Download and setup traefik-v2
mkdir -p /opt/traefik/logs
mkdir -p /opt/traefik/dynamic/conf
curl -sL "https://github.com/traefik/traefik/releases/download/${TRAEFIK_VERSION}/traefik_${TRAEFIK_VERSION}_${ARCH}.tar.gz" > "/opt/traefik/traefik_${TRAEFIK_VERSION}_${ARCH}.tar.gz"
tar -zxvf "/opt/traefik/traefik_${TRAEFIK_VERSION}_${ARCH}.tar.gz" -C /opt/traefik/

# traefik static toml config
cat <<'EOF' > /opt/traefik/traefik.toml
[Global]
  CheckNewVersion = false
  SendAnonymousUsage = false

[ServersTransport]
  InsecureSkipVerify = true

[entryPoints]
  [entryPoints.web]
    address = ":80"
  [entryPoints.websecure]
    address = ":443"

[providers]
  [providers.file]
    directory = "/opt/traefik/dynamic/conf"
    watch = true

[log]
  level = "INFO"
  filePath = "/opt/traefik/logs/traefik.log"

[accessLog]
  filePath = "/opt/traefik/logs/access.log"
  bufferingSize = 100
EOF

# traefik dynamic toml config
cat <<'EOF' > /opt/traefik/dynamic/conf/dynamic.toml
[tls.stores]
  [tls.stores.default]

[tls.options]
  [tls.options.default]
    minVersion = "VersionTLS12"
    cipherSuites = [
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
    ]
  [tls.options.mintls13]
    minVersion = "VersionTLS13"

[[tls.certificates]]
  certFile = "/etc/ssl/private/star_odd-e_com.crt"
  keyFile = "/etc/ssl/private/odde.key"
  stores = ["default"]

[http]
  [http.routers]
    # Define a connection between requests and services
    [http.routers.doughnut-app-http]
      entryPoints = "web"
      rule = "Host(`dough.odd-e.com`) || Host(`35.237.98.250`) && PathPrefix(`/`)"
      service = "doughnut-app"
      middlewares = "doughnut-app-https"
    [http.routers.doughnut-app]
      entryPoints = "websecure"
      rule = "Host(`dough.odd-e.com`) || Host(`35.237.98.250`) && PathPrefix(`/`)"
      service = "doughnut-app"
      [[http.routers.doughnut-app.tls.domains]]
        main = "odd-e.com"
        sans = ["*.odd-e.com"]
  [http.middlewares.doughnut-app-https.redirectScheme]
    scheme = "https"
    permanent = true


  [http.services]
    [http.services.doughnut-app.loadBalancer]
      [[http.services.doughnut-app.loadBalancer.servers]]
        url = "http://127.0.0.1:8081"
EOF

# define db-server entry pointing to IP address of mysql db instance
echo "10.111.16.12	db-server" >> /etc/hosts
