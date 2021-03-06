#!/bin/bash

# Set the metadata server to the get projct id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
echo "Project ID: ${PROJECTID}"

# Install dependencies
apt-get -y update && apt-get -y upgrade
apt-get -y install expect htop jq gnupg gnupg-agent mariadb-backup mariadb-client mariadb-server

ACCESS_TOKEN=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token" \
        -H "Metadata-Flavor: Google" | jq -r ".access_token")
MYSQL_ROOT_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/mysql_root_password/versions/1:access" \
	--request "GET" \
	--header "authorization: Bearer $(gcloud auth print-access-token)" \
	--header "content-type: application/json" \
	--header "x-goog-user-project: ${PROJECTID}" |
	jq -r ".payload.data" | base64 --decode)
MYSQL_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/mysql_password/versions/1:access" \
	--request "GET" \
	--header "authorization: Bearer $(gcloud auth print-access-token)" \
	--header "content-type: application/json" \
	--header "x-goog-user-project: ${PROJECTID}" |
	jq -r ".payload.data" | base64 --decode)

SECURE_MYSQL=$(expect -c "
set timeout 10
spawn mysql_secure_installation
expect \"Enter current password for root (enter for none):\"
send \"${MYSQL_ROOT_PASSWORD}\r\"
expect \"Change the root password?\"
send \"n\r\"
expect \"Remove anonymous users?\"
send \"y\r\"
expect \"Disallow root login remotely?\"
send \"y\r\"
expect \"Remove test database and access to it?\"
send \"y\r\"
expect \"Reload privilege tables now?\"
send \"y\r\"
expect eof
")

echo "$SECURE_MYSQL"

apt -y purge expect

cat <<'EOF' > init_doughnut_db.sql
CREATE DATABASE IF NOT EXISTS doughnut DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ;
CREATE USER IF NOT EXISTS 'doughnut'@'localhost' IDENTIFIED BY 'doughnut';
SET PASSWORD FOR 'doughnut'@'localhost' = PASSWORD("${MYSQL_PASSWORD}");
GRANT ALL PRIVILEGES ON doughnut.* TO 'doughnut'@'localhost';
REVOKE DROP ON doughnut.* FROM 'doughnut'@'localhost';
FLUSH PRIVILEGES;
EOF

mkdir -p /var/backups/mariadb
mkdir -p /opt/mariadb/scripts

cat <<'EOF' > /opt/mariadb/scripts/db-backup.sh
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
mysqldump --user=root --password="${MYSQL_ROOT_PASSWORD}" --lock-tables --databases doughnut > "/var/backups/mariadb/doughnut_${DB_BACKUP_DATETIME}.sql"
cd /var/backups/mariadb
tar -cvjSf "doughnut_${DB_BACKUP_DATETIME}.sql.bz2" ./"doughnut_${DB_BACKUP_DATETIME}.sql"
rm -f ./"doughnut_${DB_BACKUP_DATETIME}.sql"
gsutil cp ./"doughnut_${DB_BACKUP_DATETIME}.sql.bz2" gs://"${BUCKET}/db_backups/"
rm -f ./"doughnut_${DB_BACKUP_DATETIME}.sql.bz2"
EOF

chmod +x /opt/mariadb/scripts/db-backup.sh

mysql -uroot -p${MYSQL_ROOT_PASSWORD} < init_doughnut_db.sql
