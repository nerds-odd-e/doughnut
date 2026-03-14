#!/usr/bin/env bash
#
# Setup script for Cursor Cloud Agent environment
# This script installs Java 25, MySQL, and Redis for running backend and e2e tests
# without relying on Nix. Matches nix dev env: MySQL port 3309, Redis port 6380.

set -e

cd /workspace
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

# Set up Java environment (headless for Cloud VM - no display)
export JAVA_HOME=/tmp/java25/zulu25.30.17-ca-jdk25.0.1-linux_x64
export PATH=$JAVA_HOME/bin:$PATH
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.awt.headless=true"
export GRADLE_OPTS="${GRADLE_OPTS:-} -Djava.awt.headless=true"

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

# Install required MySQL dependencies (libncurses6 needed by mysql client)
echo "==> Installing MySQL dependencies..."
sudo apt-get update -qq
sudo apt-get install -y -qq libaio-dev libaio1t64 libnuma1 libncurses6 libtinfo6 2>&1 | grep -v "^Get:" | grep -v "^Reading" | tail -5 || true

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

# MySQL port 3309 matches nix env and db-test.properties
MYSQL_PORT=3309
MYSQL_SOCKET=/tmp/mysql.sock

# Check if MySQL is already running on correct port
MYSQL_RUNNING=false
if [ -S "$MYSQL_SOCKET" ] && "${MYSQL_HOME}/bin/mysqladmin" -u root -S "$MYSQL_SOCKET" ping > /dev/null 2>&1; then
    MYSQL_PORT_CHECK=$("${MYSQL_HOME}/bin/mysql" -u root -S "$MYSQL_SOCKET" -N -e "SELECT @@port" 2>/dev/null || echo "0")
    if [ "$MYSQL_PORT_CHECK" = "$MYSQL_PORT" ]; then
        echo "==> MySQL server already running on port $MYSQL_PORT"
        MYSQL_RUNNING=true
    fi
fi

if [ "$MYSQL_RUNNING" = false ]; then
    echo "==> Starting MySQL server on port $MYSQL_PORT..."
    pkill mysqld 2>/dev/null || true
    sleep 2
    rm -f /tmp/mysql.sock /tmp/mysqlx.sock

    "${MYSQL_HOME}/bin/mysqld" --datadir=/tmp/mysql_data --port=$MYSQL_PORT --socket=$MYSQL_SOCKET > /tmp/mysql.log 2>&1 &
    MYSQL_PID=$!
    sleep 10

    for i in {1..60}; do
        if "${MYSQL_HOME}/bin/mysql" -u root -h 127.0.0.1 -P $MYSQL_PORT -e "SELECT 1" > /dev/null 2>&1; then
            echo "==> MySQL server started successfully (PID: $MYSQL_PID)"
            break
        fi
        if [ $i -eq 60 ]; then
            echo "ERROR: MySQL failed to start. Check /tmp/mysql.log"
            tail -20 /tmp/mysql.log
            exit 1
        fi
        sleep 1
    done
fi

# Initialize doughnut databases (idempotent - safe to run multiple times)
if "${MYSQL_HOME}/bin/mysql" -u root -S "$MYSQL_SOCKET" -e "SHOW DATABASES LIKE 'doughnut_test'" 2>/dev/null | grep -q doughnut_test; then
    echo "==> Doughnut databases already exist"
else
    echo "==> Setting up doughnut databases..."
    "${MYSQL_HOME}/bin/mysql" -u root -S "$MYSQL_SOCKET" < /workspace/scripts/sql/init_doughnut_db.sql
    echo "==> Doughnut databases initialized"
fi

# Export environment variables for tests (port 3309 matches db-test.properties)
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT}/doughnut_test?allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
export SPRING_DATASOURCE_USERNAME="doughnut"
export SPRING_DATASOURCE_PASSWORD="doughnut"

# Export environment variable for e2e tests (backend uses INPUT_DB_URL when running with e2e profile)
export INPUT_DB_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT}/doughnut_e2e_test?allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"

# Use pipe fallback for CLI PTY tests (ESC, arrow keys) - PTY via script is unreliable without real terminal
export CI=1

# Verify e2e database connection
echo "==> Verifying e2e database connection..."
if "${MYSQL_HOME}/bin/mysql" -u doughnut -pdoughnut -S "$MYSQL_SOCKET" -e "USE doughnut_e2e_test; SELECT 1" > /dev/null 2>&1; then
    echo "==> E2E database connection verified"
else
    echo "ERROR: E2E database connection failed"
    exit 1
fi

# Install xvfb for Cypress e2e tests (headless display)
echo "==> Installing xvfb for Cypress..."
sudo apt-get install -y -qq xvfb 2>&1 | tail -3 || true

# Setup Redis on port 6380 (matches nix env)
echo "==> Setting up Redis on port 6380..."
sudo apt-get install -y -qq redis-server 2>&1 | tail -3 || true
if ! redis-cli -p 6380 ping > /dev/null 2>&1; then
    redis-server --port 6380 --daemonize yes --bind 127.0.0.1
    sleep 2
fi
if redis-cli -p 6380 ping > /dev/null 2>&1; then
    echo "==> Redis ready on port 6380"
else
    echo "ERROR: Redis failed to start on port 6380"
    exit 1
fi

# Run database migration for test database (at the end)
echo "==> Running database migration for test database..."
cd /workspace
if ./backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test > /tmp/migrate.log 2>&1; then
    echo "==> Test database migration completed successfully"
else
    echo "ERROR: Test database migration failed. Check /tmp/migrate.log"
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
echo "You can now run:"
echo "  Backend tests: ./backend/gradlew -p backend test -Dspring.profiles.active=test"
echo "  E2E tests:     xvfb-run pnpm cypress run --spec e2e_test/features/<feature>.feature"
echo ""
