#!/usr/bin/env bash
#
# Setup script for Cursor Cloud Agent environment
# This script installs Java 24 and MySQL for running backend unit tests
# without relying on Nix

set -e

echo "==> Setting up Cursor Cloud Agent environment..."

# Install Java 25 if not already installed
if [ ! -d "/tmp/java25/zulu25.30.17-ca-jdk25.0.1-linux_x64" ]; then
    echo "==> Installing Java 25..."
    mkdir -p /tmp/java25
    cd /tmp/java25
    wget -q https://cdn.azul.com/zulu/bin/zulu25.30.17-ca-jdk25.0.1-linux_x64.tar.gz
    tar -xzf zulu25.30.17-ca-jdk25.0.1-linux_x64.tar.gz
    rm zulu25.30.17-ca-jdk25.0.1-linux_x64.tar.gz
    echo "==> Java 25 installed to /tmp/java25/zulu25.30.17-ca-jdk25.0.1-linux_x64"
else
    echo "==> Java 25 already installed"
fi

# Set up Java environment
export JAVA_HOME=/tmp/java25/zulu25.30.17-ca-jdk25.0.1-linux_x64
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java installation
java -version

# Install MySQL if not already set up
if [ ! -d "/tmp/mysql-8.4.3-linux-glibc2.28-x86_64" ]; then
    echo "==> Downloading MySQL 8.4..."
    cd /tmp
    wget -q https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-8.4.3-linux-glibc2.28-x86_64.tar.xz
    tar -xf mysql-8.4.3-linux-glibc2.28-x86_64.tar.xz
    rm mysql-8.4.3-linux-glibc2.28-x86_64.tar.xz
    echo "==> MySQL downloaded and extracted"
fi

# Install required MySQL dependencies
echo "==> Installing MySQL dependencies..."
sudo apt-get update -qq
sudo apt-get install -y -qq libaio-dev libaio1t64 libnuma1 2>&1 | grep -v "^Get:" | grep -v "^Reading" | tail -5 || true

# Create libaio symlink if needed (Ubuntu 24.04 compatibility)
if [ ! -e "/usr/lib/x86_64-linux-gnu/libaio.so.1" ]; then
    echo "==> Creating libaio.so.1 symlink for MySQL compatibility..."
    sudo ln -sf /usr/lib/x86_64-linux-gnu/libaio.so.1t64 /usr/lib/x86_64-linux-gnu/libaio.so.1
fi

# Set up MySQL environment
export MYSQL_HOME=/tmp/mysql-8.4.3-linux-glibc2.28-x86_64
export PATH=$MYSQL_HOME/bin:$PATH

# Initialize MySQL data directory if needed
if [ ! -d "/tmp/mysql_data/mysql" ]; then
    echo "==> Initializing MySQL data directory..."
    rm -rf /tmp/mysql_data
    mkdir -p /tmp/mysql_data
    mysqld --initialize-insecure --datadir=/tmp/mysql_data --user=$(whoami) > /tmp/mysql_init.log 2>&1
    echo "==> MySQL data directory initialized"
fi

# Check if MySQL is already running
MYSQL_RUNNING=false
if mysqladmin -S /tmp/mysql.sock ping > /dev/null 2>&1; then
    echo "==> MySQL server already running"
    MYSQL_RUNNING=true
elif mysql -u root -S /tmp/mysql.sock -e "SELECT 1" > /dev/null 2>&1; then
    echo "==> MySQL server already running (verified with connection test)"
    MYSQL_RUNNING=true
fi

if [ "$MYSQL_RUNNING" = false ]; then
    echo "==> Starting MySQL server..."
    # Clean up any stale socket files
    rm -f /tmp/mysql.sock /tmp/mysqlx.sock

    mysqld --datadir=/tmp/mysql_data --port=3306 --socket=/tmp/mysql.sock > /tmp/mysql.log 2>&1 &
    MYSQL_PID=$!

    # Wait for MySQL to start
    for i in {1..30}; do
        if mysql -u root -S /tmp/mysql.sock -e "SELECT 1" > /dev/null 2>&1; then
            echo "==> MySQL server started successfully (PID: $MYSQL_PID)"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "ERROR: MySQL failed to start. Check /tmp/mysql.log"
            tail -20 /tmp/mysql.log
            exit 1
        fi
        sleep 1
    done
fi

# Initialize doughnut databases (idempotent - safe to run multiple times)
if mysql -u root -S /tmp/mysql.sock -e "SHOW DATABASES LIKE 'doughnut_test'" 2>/dev/null | grep -q doughnut_test; then
    echo "==> Doughnut databases already exist"
else
    echo "==> Setting up doughnut databases..."
    mysql -u root -S /tmp/mysql.sock < /workspace/scripts/sql/init_doughnut_db.sql
    echo "==> Doughnut databases initialized"
fi

# Export environment variables for tests
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/doughnut_test?allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
export SPRING_DATASOURCE_USERNAME="doughnut"
export SPRING_DATASOURCE_PASSWORD="doughnut"

# Export environment variable for e2e tests (backend uses INPUT_DB_URL when running with e2e profile)
export INPUT_DB_URL="jdbc:mysql://localhost:3306/doughnut_e2e_test?allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"

# Run database migration for test database
echo "==> Running database migration for test database..."
cd /workspace
if ./backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test > /tmp/migrate.log 2>&1; then
    echo "==> Test database migration completed successfully"
else
    echo "ERROR: Test database migration failed. Check /tmp/migrate.log"
    exit 1
fi

# Verify e2e database connection (migration will happen automatically when backend starts with e2e profile)
echo "==> Verifying e2e database connection..."
if mysql -u doughnut -pdoughnut -S /tmp/mysql.sock -e "USE doughnut_e2e_test; SELECT 1" > /dev/null 2>&1; then
    echo "==> E2E database connection verified"
else
    echo "ERROR: E2E database connection failed"
    exit 1
fi

echo ""
echo "==> Cloud agent environment setup complete!"
echo ""
echo "Environment variables set:"
echo "  JAVA_HOME=$JAVA_HOME"
echo "  MYSQL_HOME=$MYSQL_HOME"
echo "  SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL"
echo ""
echo "You can now run backend tests with:"
echo "  ./backend/gradlew -p backend test -Dspring.profiles.active=test"
echo ""
