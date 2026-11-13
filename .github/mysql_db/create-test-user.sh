#!/usr/bin/env bash
set -euo pipefail

test_user=${MYSQL_BOOTSTRAP_USER//\'/\'\'}
test_password=${MYSQL_BOOTSTRAP_PASSWORD//\'/\'\'}
test_database=${MYSQL_BOOTSTRAP_DATABASE//\`/\`\`}

MYSQL_PWD="$MYSQL_BOOTSTRAP_ROOT_PASSWORD" docker exec -i --env MYSQL_PWD mysql \
  mysql --host=127.0.0.1 --port="$MYSQL_BOOTSTRAP_PORT" --user=root --batch <<SQL
SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES';
CREATE USER '$test_user'@'%' IDENTIFIED BY '$test_password';
GRANT ALL PRIVILEGES ON \`$test_database\`.* TO '$test_user'@'%';
SQL
