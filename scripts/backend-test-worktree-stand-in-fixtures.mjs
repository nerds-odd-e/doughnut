import {
  chmodSync,
  copyFileSync,
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
const repoBackend = fileURLToPath(new URL('../backend', import.meta.url))

const jdbcParams =
  'connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'

export function jdbcUrl(database) {
  return `jdbc:mysql://127.0.0.1:3309/${database}?${jdbcParams}`
}

// Writes a recording stand-in script (e.g. for `java` or `mysql`) and
// makes it executable. `lines` is the shell script body, one array entry per
// line.
function writeStandIn(scriptPath, lines) {
  writeFileSync(scriptPath, [...lines, ''].join('\n'))
  chmodSync(scriptPath, 0o755)
}

// Shell lines implementing a stand-in's hold/release protocol: when `envVar`
// is set, the stand-in announces it has been reached (writing to
// <root>/<reachedName>) and then blocks until <root>/<releaseName> appears,
// polling every 50ms. Lets a test pause a stand-in mid-invocation to inject
// external activity at that exact point. Shared by the java and mysql
// stand-ins below.
function holdReleaseLines(envVar, reachedName, releaseName) {
  return [
    `if [ -n "\${${envVar}:-}" ]; then`,
    `  printf 'reached\\n' > "$root/${reachedName}"`,
    `  release="$root/${releaseName}"`,
    '  while [ ! -e "$release" ]; do',
    '    sleep 0.05',
    '  done',
    'fi',
  ]
}

export function makeCheckout(t, { config } = {}) {
  const root = mkdtempSync(path.join(tmpdir(), 'backend-test-worktree-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  mkdirSync(path.join(root, 'scripts'), { recursive: true })
  mkdirSync(path.join(root, 'backend'), { recursive: true })

  const launcher = path.join(root, 'scripts', 'backend-test-worktree.sh')
  copyFileSync(launcherSrc, launcher)
  chmodSync(launcher, 0o755)

  copyFileSync(
    path.join(repoBackend, 'gradlew'),
    path.join(root, 'backend', 'gradlew')
  )
  chmodSync(path.join(root, 'backend', 'gradlew'), 0o755)
  const wrapperDir = path.join(root, 'backend', 'gradle', 'wrapper')
  mkdirSync(wrapperDir, { recursive: true })
  for (const name of ['gradle-wrapper.jar', 'gradle-wrapper.properties']) {
    copyFileSync(
      path.join(repoBackend, 'gradle', 'wrapper', name),
      path.join(wrapperDir, name)
    )
  }

  const gradleInvocation = path.join(root, 'gradle-invocation')
  const gradleReached = path.join(root, 'gradle-reached')
  const gradleRelease = path.join(root, 'gradle-release')
  // The real wrapper execs `$JAVA_HOME/bin/java` when JAVA_HOME is set (Nix
  // sets it). PATH-only `java` interception would miss that endpoint.
  const javaHome = path.join(root, 'java-home')
  mkdirSync(path.join(javaHome, 'bin'), { recursive: true })
  writeStandIn(path.join(javaHome, 'bin', 'java'), [
    '#!/bin/sh',
    'root="$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)"',
    'record="$root/gradle-invocation"',
    '{',
    '  printf \'cwd=%s\\n\' "$PWD"',
    '  printf \'SPRING_DATASOURCE_URL=%s\\n\' "${SPRING_DATASOURCE_URL-}"',
    '  for arg in "$@"; do',
    '    printf \'arg:%s\\n\' "$arg"',
    '  done',
    '} > "$record"',
    "printf 'GRADLE_STDOUT\\n'",
    "printf 'GRADLE_REACHED\\n' >&2",
    ...holdReleaseLines('GRADLE_HOLD', 'gradle-reached', 'gradle-release'),
    'exit "${FAKE_GRADLE_EXIT:-0}"',
  ])

  if (config !== undefined) {
    writeFileSync(path.join(root, '.worktree.local.json'), config)
  }

  // A recording stand-in for the `mysql` administration CLI, invoked by
  // backend-test-worktree.sh to create/grant a first-use database. Lives at
  // `<root>/bin/mysql` so it can be put ahead of the real `mysql` on PATH.
  // Records every argument and any piped stdin (e.g. the heredoc SQL) to
  // mysqlInvocation, then exits with FAKE_MYSQL_EXIT (default 0).
  const binDir = path.join(root, 'bin')
  mkdirSync(binDir, { recursive: true })
  const mysqlStandIn = path.join(binDir, 'mysql')
  const mysqlInvocation = path.join(root, 'mysql-invocation')
  const mysqlReached = path.join(root, 'mysql-reached')
  const mysqlRelease = path.join(root, 'mysql-release')
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
    ...holdReleaseLines('MYSQL_HOLD', 'mysql-reached', 'mysql-release'),
    'exit "${FAKE_MYSQL_EXIT:-0}"',
  ])

  return {
    root,
    launcher,
    javaHome,
    gradleInvocation,
    gradleReached,
    gradleRelease,
    binDir,
    mysqlStandIn,
    mysqlInvocation,
    mysqlReached,
    mysqlRelease,
  }
}

export function readGradleInvocation(checkout) {
  const text = readFileSync(checkout.gradleInvocation, 'utf8')
  const javaArgs = []
  let cwd
  let url
  for (const line of text.split('\n')) {
    if (line.startsWith('cwd=')) cwd = line.slice('cwd='.length)
    else if (line.startsWith('SPRING_DATASOURCE_URL=')) {
      url = line.slice('SPRING_DATASOURCE_URL='.length)
    } else if (line.startsWith('arg:')) javaArgs.push(line.slice('arg:'.length))
  }
  const jarAt = javaArgs.indexOf('-jar')
  const jarPath = jarAt >= 0 ? javaArgs[jarAt + 1] : undefined
  const wrapper = jarPath
    ? path.normalize(path.join(path.dirname(jarPath), '..', '..', 'gradlew'))
    : undefined
  const args = jarAt >= 0 ? javaArgs.slice(jarAt + 2) : javaArgs
  return { wrapper, cwd, url, args, javaArgs }
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
