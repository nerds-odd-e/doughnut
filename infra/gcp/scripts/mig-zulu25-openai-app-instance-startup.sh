#!/bin/bash

# Set the metadata server to the get projct id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
ARTIFACT="doughnut"
VERSION="0.0.1-SNAPSHOT"

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

# Download doughnut-app jar
gsutil cp gs://"${BUCKET}/backend_app_jar/${ARTIFACT}-${VERSION}.jar" "/opt/doughnut_app/${ARTIFACT}-${VERSION}.jar"

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

export GITHUB_FOR_ISSUES_API_TOKEN=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/github_for_issues_api_token/versions/2:access" \
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
export TZ="Asia/Singapore"
export TERM=xterm
export DEBIAN_FRONTEND=noninteractive
export JAVA_OPTS="-XX:InitialRAMPercentage=75.0 \
        -XX:MaxRAMPercentage=85.0 \
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
        -XX:+UseLargePages \
        -XX:LargePageSizeInBytes=2m \
        -XX:+UseTransparentHugePages \
        -XX:+ExitOnOutOfMemoryError \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:HeapDumpPath=/var/log \
        -XX:ActiveProcessorCount=2 \
        -Djava.security.egd=file:/dev/./urandom \
        -Dspring.output.ansi.enabled=never \
        -Dspring.jmx.enabled=false \
        -Dspring.liveBeansView.mbeanDomain=false"

# Start Spring Boot app
# Redirect stdout/stderr to log files that Cloud Logging agent monitors
# Cloud Logging agent automatically captures logs from /var/log/*.log files
nohup java ${JAVA_OPTS} -jar \
        -Dspring-boot.run.profiles=prod \
        -Dspring.profiles.active=prod \
        -Dspring.datasource.url='jdbc:mysql://db-server:3306/doughnut' \
        -Dspring.datasource.password=${MYSQL_PASSWORD} \
        -Dspring.github_for_issues.token=${GITHUB_FOR_ISSUES_API_TOKEN} \
        -Dspring.openai.token=${OPENAI_API_TOKEN} \
        -Dlogging.level.com.zaxxer.hikari=WARN \
        -Dlogging.level.com.zaxxer.hikari.HikariConfig=WARN \
        /opt/doughnut_app/${ARTIFACT}-${VERSION}.jar \
        > /var/log/doughnut-app.log 2>&1 &
