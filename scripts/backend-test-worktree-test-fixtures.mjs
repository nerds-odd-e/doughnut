import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import {
  chmodSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const launcherSrc = fileURLToPath(
  new URL('./backend-test-worktree.sh', import.meta.url)
)

const jdbcParams =
  'connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'

export function jdbcUrl(database) {
  return `jdbc:mysql://127.0.0.1:3309/${database}?${jdbcParams}`
}

export function makeCheckout(t, { config } = {}) {
  const root = mkdtempSync(path.join(tmpdir(), 'backend-test-worktree-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  mkdirSync(path.join(root, 'scripts'), { recursive: true })
  mkdirSync(path.join(root, 'backend'), { recursive: true })

  const launcher = path.join(root, 'scripts', 'backend-test-worktree.sh')
  copyFileSync(launcherSrc, launcher)
  chmodSync(launcher, 0o755)

  const gradleInvocation = path.join(root, 'gradle-invocation')
  writeFileSync(
    path.join(root, 'backend', 'gradlew'),
    [
      '#!/bin/sh',
      'record="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)/gradle-invocation"',
      '{',
      '  printf \'wrapper=%s\\n\' "$0"',
      '  printf \'cwd=%s\\n\' "$PWD"',
      '  printf \'SPRING_DATASOURCE_URL=%s\\n\' "${SPRING_DATASOURCE_URL-}"',
      '  for arg in "$@"; do',
      '    printf \'arg:%s\\n\' "$arg"',
      '  done',
      '} > "$record"',
      "printf 'GRADLE_STDOUT\\n'",
      "printf 'GRADLE_REACHED\\n' >&2",
      'exit "${FAKE_GRADLE_EXIT:-0}"',
      '',
    ].join('\n')
  )
  chmodSync(path.join(root, 'backend', 'gradlew'), 0o755)

  if (config !== undefined) {
    writeFileSync(path.join(root, '.worktree.local.json'), config)
  }

  return { root, launcher, gradleInvocation }
}

export function runLauncher(checkout, { env = {}, args = [] } = {}) {
  const childEnv = { ...process.env }
  delete childEnv.SPRING_DATASOURCE_URL
  delete childEnv.DB_URL
  delete childEnv.SPRING_FLYWAY_URL
  delete childEnv.FAKE_GRADLE_EXIT
  Object.assign(childEnv, env)
  return spawnSync(checkout.launcher, args, {
    cwd: checkout.root,
    encoding: 'utf8',
    env: childEnv,
  })
}

export function outputOf(result) {
  return `${result.stdout}${result.stderr}`
}

export function assertRefusedBeforeGradle(checkout, result) {
  assert.equal(result.error, undefined, result.stderr)
  assert.notEqual(result.status, 0)
  assert.equal(existsSync(checkout.gradleInvocation), false)
  assert.doesNotMatch(outputOf(result), /GRADLE_REACHED/)
}

export function readGradleInvocation(checkout) {
  const text = readFileSync(checkout.gradleInvocation, 'utf8')
  const args = []
  let wrapper
  let cwd
  let url
  for (const line of text.split('\n')) {
    if (line.startsWith('wrapper=')) wrapper = line.slice('wrapper='.length)
    else if (line.startsWith('cwd=')) cwd = line.slice('cwd='.length)
    else if (line.startsWith('SPRING_DATASOURCE_URL=')) {
      url = line.slice('SPRING_DATASOURCE_URL='.length)
    } else if (line.startsWith('arg:')) args.push(line.slice('arg:'.length))
  }
  return { wrapper, cwd, url, args }
}
