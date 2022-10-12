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

# Make Java 17 default
export JAVA_HOME=/usr/lib/jvm/zre-17-amd64
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

export GITHUB_FOR_ISSUES_API_TOKEN=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/github_for_issues_api_token/versions/1:access" \
    --request "GET" \
    --header "authorization: Bearer ${ACCESS_TOKEN}" \
    --header "content-type: application/json" \
    --header "x-goog-user-project: ${PROJECTID}" |
    jq -r ".payload.data" | base64 --decode)
# Start server
bash -c "java -jar -Dspring-boot.run.profiles=prod -Dspring.profiles.active=prod -Dspring.datasource.url='jdbc:mysql://db-server:3306/doughnut' -Dspring.datasource.password=${MYSQL_PASSWORD} /opt/doughnut_app/${ARTIFACT}-${VERSION}.jar" &
