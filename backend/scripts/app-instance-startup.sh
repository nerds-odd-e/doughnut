#!/bin/sh

# Set the metadata server to the get projct id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
ARTIFACT="doughnut"
VERSION="0.0.1-SNAPSHOT"

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

# Get the files we need
gsutil cp gs://${BUCKET}/${ARTIFACT}-${VERSION}.jar .

# Install dependencies
apt-get update
apt-get -y install jq openjdk-11-jdk gnupg gnupg-agent mariadb-client mariadb-backup

# Make Java 11 default
update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/jre/bin/java

ACCESS_TOKEN=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token" \
	-H "Metadata-Flavor: Google" | jq -r ".access_token")

MYSQL_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/mysql_password/versions/1:access" \
	--request "GET" \
	--header "authorization: Bearer ${ACCESS_TOKEN}" \
	--header "content-type: application/json" \
	--header "x-goog-user-project: ${PROJECTID}" |
	jq -r ".payload.data" | base64 --decode)

# define db-server entry pointing to IP address of mariadb instance
echo "10.142.0.18	db-server" >>/etc/hosts

# Start server
echo ${MYSQL_PASSWORD}
java -jar -Dspring-boot.run.profiles=prod -Dspring.datasource.password=${MYSQL_PASSWORD} ${ARTIFACT}-${VERSION}.jar
