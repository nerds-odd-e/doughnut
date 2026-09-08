import { readFileSync } from 'node:fs'
import path from 'node:path'

export function mysqlStandInScriptLines(holdReleaseLines) {
  return [
    '#!/bin/sh',
    'root="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"',
    'count="$root/mysql-invocation-count"',
    'n=1',
    'if [ -f "$count" ]; then',
    '  n=$(($(cat "$count") + 1))',
    'fi',
    'printf \'%s\\n\' "$n" > "$count"',
    'record="$root/mysql-invocation.$n"',
    '{',
    '  for arg in "$@"; do',
    '    printf \'arg:%s\\n\' "$arg"',
    '  done',
    '  if [ ! -t 0 ]; then',
    '    printf \'stdin:%s\\n\' "$(cat)"',
    '  fi',
    '} > "$record"',
    'cp "$record" "$root/mysql-invocation"',
    'schema_name=""',
    'for arg in "$@"; do',
    '  case "$arg" in',
    '    *information_schema.SCHEMATA*)',
    "      schema_name=$(printf '%s\\n' \"$arg\" | sed -n \"s/.*SCHEMA_NAME='\\([^\\']*\\)'.*/\\1/p\")",
    '      ;;',
    '  esac',
    'done',
    ...holdReleaseLines('MYSQL_HOLD', 'mysql-reached', 'mysql-release'),
    'if [ -n "$schema_name" ]; then',
    '  if [ -z "${FAKE_SCHEMA_MISSING:-}" ]; then',
    '    printf \'%s\\n\' "$schema_name"',
    '  fi',
    'fi',
    'exit "${FAKE_MYSQL_EXIT:-0}"',
  ]
}

function parseMysqlInvocation(text) {
  const args = []
  let stdin
  for (const line of text.split('\n')) {
    if (line.startsWith('arg:')) args.push(line.slice('arg:'.length))
    else if (line.startsWith('stdin:')) stdin = line.slice('stdin:'.length)
  }
  return { args, stdin }
}

export function readMysqlInvocation(checkout) {
  return parseMysqlInvocation(readFileSync(checkout.mysqlInvocation, 'utf8'))
}

export function readMysqlInvocations(checkout) {
  const count = Number(
    readFileSync(path.join(checkout.root, 'mysql-invocation-count'), 'utf8')
  )
  const invocations = []
  for (let n = 1; n <= count; n += 1) {
    invocations.push(
      parseMysqlInvocation(
        readFileSync(path.join(checkout.root, `mysql-invocation.${n}`), 'utf8')
      )
    )
  }
  return invocations
}
