#!/bin/bash

# Set the metadata server to the get projct id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
ARTIFACT="doughnut"
VERSION="0.0.1-SNAPSHOT"
ARCH="linux_amd64"
TRAEFIK_VERSION="v2.4.6"

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

# Get the files we need
# mkdir -p /opt/doughnut_app
gsutil cp gs://"${BUCKET}/backend_app_jar/${ARTIFACT}-${VERSION}.jar" "/opt/doughnut_app/${ARTIFACT}-${VERSION}.jar"

# Make Java 11 default
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin

bash -c "/opt/traefik/traefik --configfile=/opt/traefik/traefik.toml" &

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

# Stop unneeded salt-minion
systemctl stop salt-minion

# Start server
bash -c "java -jar -Dspring-boot.run.profiles=prod -Dspring.profiles.active=prod -Dspring.datasource.url='jdbc:mysql://db-server:3306/doughnut' -Dspring.datasource.password=${MYSQL_PASSWORD} /opt/doughnut_app/${ARTIFACT}-${VERSION}.jar" &
