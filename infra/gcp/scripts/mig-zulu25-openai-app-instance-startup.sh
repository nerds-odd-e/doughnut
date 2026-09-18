#!/bin/bash

set -euo pipefail

METADATA_BASE_URL="http://metadata.google.internal/computeMetadata/v1"
METADATA_HEADER="Metadata-Flavor: Google"
HOSTS_FILE="${HOSTS_FILE:-/etc/hosts}"

if ! DATABASE_PRIVATE_IP=$(curl -fsS \
  "$METADATA_BASE_URL/instance/attributes/DATABASE_PRIVATE_IP" \
  -H "$METADATA_HEADER"); then
  echo "Error: DATABASE_PRIVATE_IP instance metadata is required" >&2
  exit 1
fi

is_ipv4_address() {
  local address=$1 octet
  local -a octets

  [[ "$address" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || return 1
  IFS=. read -r -a octets <<<"$address"
  for octet in "${octets[@]}"; do
    [[ "$octet" =~ ^(0|[1-9][0-9]{0,2})$ ]] || return 1
    ((10#$octet <= 255)) || return 1
  done
}

if ! is_ipv4_address "$DATABASE_PRIVATE_IP"; then
  echo "Error: DATABASE_PRIVATE_IP instance metadata must be a valid IPv4 address: <$DATABASE_PRIVATE_IP>" >&2
  exit 1
fi

hosts_update=$(mktemp)
awk -v database_private_ip="$DATABASE_PRIVATE_IP" '
  {
    routes_database = 0
    for (field = 2; field <= NF; field += 1) {
      if ($field == "db-server") {
        routes_database = 1
      }
    }
    if (!routes_database) {
      print
    }
  }
  END {
    print database_private_ip "\tdb-server"
  }
' "$HOSTS_FILE" >"$hosts_update"
cat "$hosts_update" >"$HOSTS_FILE"
rm "$hosts_update"

# Set the metadata server to get the project id.
PROJECTID=$(curl -fsS "$METADATA_BASE_URL/project/project-id" -H "$METADATA_HEADER")
BUCKET=$(curl -fsS "$METADATA_BASE_URL/instance/attributes/BUCKET" -H "$METADATA_HEADER")
ARTIFACT="donut"
VERSION="0.0.1-SNAPSHOT"

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

# Download doughnut-app jar
gcloud storage cp gs://"${BUCKET}/backend_app_jar/${ARTIFACT}-${VERSION}.jar" "/opt/doughnut_app/${ARTIFACT}-${VERSION}.jar"

# Stop unneeded salt-minion
systemctl stop salt-minion

# Make JRE 25 default
export JAVA_HOME=/usr/lib/jvm/zre-25-amd64
export PATH=$PATH:$JAVA_HOME/bin

export ACCESS_TOKEN=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token" \
  -H "Metadata-Flavor: Google" | jq -r ".access_token")

export MYSQL_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/mysql_password/versions/1:access" \
  --request "GET" \
  --header "authorization: Bearer ${ACCESS_TOKEN}" \
  --header "content-type: application/json" \
  --header "x-goog-user-project: ${PROJECTID}" |
  jq -r ".payload.data" | base64 --decode)

export OAUTH2_github_client_id=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/oauth2_github_client_id/versions/1:access" \
  --request "GET" \
  --header "authorization: Bearer ${ACCESS_TOKEN}" \
  --header "content-type: application/json" \
  --header "x-goog-user-project: ${PROJECTID}" |
  jq -r ".payload.data" | base64 --decode)

export OAUTH2_github_client_secret=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/oauth2_github_client_secret/versions/1:access" \
  --request "GET" \
  --header "authorization: Bearer ${ACCESS_TOKEN}" \
  --header "content-type: application/json" \
  --header "x-goog-user-project: ${PROJECTID}" |
  jq -r ".payload.data" | base64 --decode)

export GITHUB_DOUGHNUT_REPO_ACCESS_TOKEN=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/github_doughnut_repo_access_token/versions/2:access" \
  --request "GET" \
  --header "authorization: Bearer ${ACCESS_TOKEN}" \
  --header "content-type: application/json" \
  --header "x-goog-user-project: ${PROJECTID}" |
  jq -r ".payload.data" | base64 --decode)

export GITHUB_FOR_ISSUES_API_TOKEN=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/github_for_issues_api_token/versions/latest:access" \
  --request "GET" \
  --header "authorization: Bearer ${ACCESS_TOKEN}" \
  --header "content-type: application/json" \
  --header "x-goog-user-project: ${PROJECTID}" |
  jq -r ".payload.data" | base64 --decode)

export OPENAI_API_TOKEN=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/openai_api_token/versions/1:access" \
  --request "GET" \
  --header "authorization: Bearer ${ACCESS_TOKEN}" \
  --header "content-type: application/json" \
  --header "x-goog-user-project: ${PROJECTID}" |
  jq -r ".payload.data" | base64 --decode)

# Start server
export TZ=UTC
export TERM=xterm
export DEBIAN_FRONTEND=noninteractive
export JAVA_OPTS="-XX:InitialRAMPercentage=40.0 \
        -XX:MaxRAMPercentage=50.0 \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=100 \
        -XX:G1HeapRegionSize=32M \
        -XX:InitiatingHeapOccupancyPercent=35 \
        -XX:+UseStringDeduplication \
        -XX:+UseCompressedOops \
        -XX:+AlwaysPreTouch \
        -XX:+UseNUMA \
        -XX:+DisableExplicitGC \
        -XX:+ParallelRefProcEnabled \
        -XX:+PerfDisableSharedMem \
        -XX:+ExitOnOutOfMemoryError \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:HeapDumpPath=/var/log \
        -XX:ActiveProcessorCount=2 \
        -Djava.security.egd=file:/dev/./urandom \
        -Dspring.output.ansi.enabled=never \
        -Dspring.jmx.enabled=false \
        -Dspring.liveBeansView.mbeanDomain=false \
        -Duser.timezone=UTC"

# Start Spring Boot app
# Write directly to stdout/stderr - Cloud Logging agent automatically captures these
# Background the process but keep stdout/stderr connected
bash -c "java ${JAVA_OPTS} \
        -Dspring-boot.run.profiles=prod \
        -Dspring.profiles.active=prod \
        -Dspring.datasource.url='jdbc:mysql://db-server:3306/doughnut?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
        -Dspring.datasource.password=${MYSQL_PASSWORD} \
        -Dspring.github_for_issues.token=${GITHUB_FOR_ISSUES_API_TOKEN} \
        -Dspring.openai.token=${OPENAI_API_TOKEN} \
        -Dlogging.level.com.zaxxer.hikari=WARN \
        -Dlogging.level.com.zaxxer.hikari.HikariConfig=WARN \
        -jar /opt/doughnut_app/${ARTIFACT}-${VERSION}.jar" &
