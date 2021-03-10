#!/bin/bash

# Set the metadata server to the get projct id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
echo "Project ID: ${PROJECTID}"

BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
ACCESS_TOKEN=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token" \
        -H "Metadata-Flavor: Google" | jq -r ".access_token")
MYSQL_ROOT_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/mysql_root_password/versions/1:access" \
        --request "GET" \
        --header "authorization: Bearer ${ACCESS_TOKEN}" \
        --header "content-type: application/json" \
        --header "x-goog-user-project: ${PROJECTID}" |
        jq -r ".payload.data" | base64 --decode)

DB_BACKUP_DATETIME=`date "+%Y%m%d-%H%M%S"`
mysqldump --u root --p "${MYSQL_ROOT_PASSWORD}" --lock-tables --databases doughnut > "/var/backups/mariadb/doughnut_${DB_BACKUP_DATETIME}.sql"
cd /var/backups/mariadb
tar -cvjSf "doughnut_${DB_BACKUP_DATETIME}.sql.bz2" ./"doughnut_${DB_BACKUP_DATETIME}.sql"
rm -f ./"doughnut_${DB_BACKUP_DATETIME}.sql"
gsutil cp ./"doughnut_${DB_BACKUP_DATETIME}.sql.bz2" gs://"${BUCKET}/db_backups/"
rm -f ./"doughnut_${DB_BACKUP_DATETIME}.sql.bz2"
