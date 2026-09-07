import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
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
import { setTimeout as delay } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'

const launcherSrc = fileURLToPath(
  new URL('./backend-test-worktree.sh', import.meta.url)
)

const jdbcParams =
  'connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'

export function jdbcUrl(database) {
  return `jdbc:mysql://127.0.0.1:3309/${database}?${jdbcParams}`
}

// Writes a recording stand-in script (e.g. for `gradlew` or `mysql`) and
// makes it executable. `lines` is the shell script body, one array entry per
// line.
function writeStandIn(scriptPath, lines) {
  writeFileSync(scriptPath, [...lines, ''].join('\n'))
  chmodSync(scriptPath, 0o755)
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
  const gradleReached = path.join(root, 'gradle-reached')
  const gradleRelease = path.join(root, 'gradle-release')
  writeStandIn(path.join(root, 'backend', 'gradlew'), [
    '#!/bin/sh',
    'root="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"',
    'record="$root/gradle-invocation"',
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
    'printf \'reached\\n\' > "$root/gradle-reached"',
    'if [ -n "${GRADLE_HOLD:-}" ]; then',
    '  release="$root/gradle-release"',
    '  while [ ! -e "$release" ]; do',
    '    sleep 0.05',
    '  done',
    'fi',
    'exit "${FAKE_GRADLE_EXIT:-0}"',
  ])

  if (config !== undefined) {
    writeFileSync(path.join(root, '.worktree.local.json'), config)
  }

  // A recording stand-in for the `mysql` administration CLI, invoked by
  // backend-test-worktree.sh to create/grant a first-use database. Lives at
  // `<root>/bin/mysql` (mirroring gradlew's `<root>/backend/gradlew` layout)
  // so it can be put ahead of the real `mysql` on PATH. Records every
  // argument and any piped stdin (e.g. the heredoc SQL) to mysqlInvocation,
  // then exits with FAKE_MYSQL_EXIT (default 0).
  const binDir = path.join(root, 'bin')
  mkdirSync(binDir, { recursive: true })
  const mysqlStandIn = path.join(binDir, 'mysql')
  const mysqlInvocation = path.join(root, 'mysql-invocation')
  writeStandIn(mysqlStandIn, [
    '#!/bin/sh',
    'root="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"',
    'record="$root/mysql-invocation"',
    '{',
    '  for arg in "$@"; do',
    '    printf \'arg:%s\\n\' "$arg"',
    '  done',
    '  if [ ! -t 0 ]; then',
    '    printf \'stdin:%s\\n\' "$(cat)"',
    '  fi',
    '} > "$record"',
    'exit "${FAKE_MYSQL_EXIT:-0}"',
  ])

  return {
    root,
    launcher,
    gradleInvocation,
    gradleReached,
    gradleRelease,
    binDir,
    mysqlStandIn,
    mysqlInvocation,
  }
}

function sanitizedChildEnv(env) {
  const childEnv = { ...process.env }
  delete childEnv.SPRING_DATASOURCE_URL
  delete childEnv.DB_URL
  delete childEnv.SPRING_FLYWAY_URL
  delete childEnv.FAKE_GRADLE_EXIT
  delete childEnv.FAKE_MYSQL_EXIT
  Object.assign(childEnv, env)
  return childEnv
}

// Puts the checkout's fake `mysql` stand-in ahead of the real one on PATH so
// the launcher's `mysql` invocation is intercepted, then applies the usual
// sanitization/overrides.
function launcherChildEnv(checkout, env) {
  return sanitizedChildEnv({
    PATH: `${checkout.binDir}${path.delimiter}${process.env.PATH ?? ''}`,
    ...env,
  })
}

export function runLauncher(checkout, { env = {}, args = [] } = {}) {
  return spawnSync(checkout.launcher, args, {
    cwd: checkout.root,
    encoding: 'utf8',
    env: launcherChildEnv(checkout, env),
  })
}

export function outputOf(result) {
  return `${result.stdout}${result.stderr}`
}

async function waitForFile(
  filePath,
  { timeoutMs = 5000, intervalMs = 20 } = {}
) {
  const deadline = Date.now() + timeoutMs
  while (!existsSync(filePath)) {
    if (Date.now() >= deadline) {
      throw new Error(`Timed out waiting for ${filePath}`)
    }
    await delay(intervalMs)
  }
}

// Starts the launcher asynchronously against a foreground Gradle stand-in
// that reaches GRADLE_REACHED and then blocks until explicitly released.
// Returns a handle for observing that one active Gradle owner from outside.
export function runLauncherAsync(checkout, { env = {}, args = [] } = {}) {
  const child = spawn(checkout.launcher, args, {
    cwd: checkout.root,
    env: launcherChildEnv(checkout, { ...env, GRADLE_HOLD: '1' }),
  })

  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (chunk) => {
    stdout += chunk
  })
  child.stderr.on('data', (chunk) => {
    stderr += chunk
  })

  const exited = new Promise((resolve) => {
    child.on('close', (status) => {
      resolve({ status, stdout, stderr })
    })
  })

  return {
    waitForGradleReached: () => waitForFile(checkout.gradleReached),
    release: () => writeFileSync(checkout.gradleRelease, ''),
    waitForExit: () => exited,
  }
}

export function lockPaths(checkout) {
  const dir = path.join(checkout.root, '.worktree.local.lock')
  return { dir, ownerFile: path.join(dir, 'owner.pid') }
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

export function readMysqlInvocation(checkout) {
  const text = readFileSync(checkout.mysqlInvocation, 'utf8')
  const args = []
  let stdin
  for (const line of text.split('\n')) {
    if (line.startsWith('arg:')) args.push(line.slice('arg:'.length))
    else if (line.startsWith('stdin:')) stdin = line.slice('stdin:'.length)
  }
  return { args, stdin }
}
