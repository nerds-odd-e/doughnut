import {
  chmodSync,
  copyFileSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  symlinkSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptsSrc = fileURLToPath(new URL('.', import.meta.url))
const repoRoot = fileURLToPath(new URL('..', import.meta.url))
const checkoutScriptNames = [
  'backend-test.sh',
  'backend-test-worktree.sh',
  'backend-test-worktree-owner.sh',
  'backend-worktree-gradle-route.sh',
]

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

  for (const name of checkoutScriptNames) {
    const dest = path.join(root, 'scripts', name)
    copyFileSync(path.join(scriptsSrc, name), dest)
    chmodSync(dest, 0o755)
  }
  const launcher = path.join(root, 'scripts', 'backend-test-worktree.sh')

  copyFileSync(path.join(repoRoot, 'gradlew'), path.join(root, 'gradlew'))
  chmodSync(path.join(root, 'gradlew'), 0o755)
  symlinkSync('../gradlew', path.join(root, 'backend', 'gradlew'))
  const pkg = JSON.parse(
    readFileSync(path.join(repoRoot, 'package.json'), 'utf8')
  )
  writeFileSync(
    path.join(root, 'package.json'),
    `${JSON.stringify({
      name: 'donut-worktree-fixture',
      scripts: {
        'backend:format': pkg.scripts['backend:format'],
        'backend:test_only': pkg.scripts['backend:test_only'],
        'backend:test': pkg.scripts['backend:test'],
      },
    })}\n`
  )
  const wrapperDir = path.join(root, 'gradle', 'wrapper')
  mkdirSync(wrapperDir, { recursive: true })
  for (const name of ['gradle-wrapper.jar', 'gradle-wrapper.properties']) {
    copyFileSync(
      path.join(repoRoot, 'gradle', 'wrapper', name),
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
    'count="$root/gradle-invocation-count"',
    'n=1',
    'if [ -f "$count" ]; then',
    '  n=$(($(cat "$count") + 1))',
    'fi',
    'printf \'%s\\n\' "$n" > "$count"',
    'record="$root/gradle-invocation"',
    '{',
    '  printf \'cwd=%s\\n\' "$PWD"',
    '  printf \'SPRING_DATASOURCE_URL=%s\\n\' "${SPRING_DATASOURCE_URL-}"',
    '  printf \'DONUT_WORKTREE_HANDOFF=%s\\n\' "${DONUT_WORKTREE_HANDOFF-}"',
    '  for arg in "$@"; do',
    '    printf \'arg:%s\\n\' "$arg"',
    '  done',
    '} > "$record"',
    'cp "$record" "$root/gradle-invocation.$n"',
    "printf 'GRADLE_STDOUT\\n'",
    "printf 'GRADLE_REACHED\\n' >&2",
    ...holdReleaseLines('GRADLE_HOLD', 'gradle-reached', 'gradle-release'),
    'exit_code="${FAKE_GRADLE_EXIT:-0}"',
    'if [ -n "${FAKE_GRADLE_FAIL_INVOCATION:-}" ] && [ "$n" -ne "${FAKE_GRADLE_FAIL_INVOCATION}" ]; then',
    '  exit_code=0',
    'fi',
    'exit "$exit_code"',
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

function parseGradleInvocation(text) {
  const javaArgs = []
  let cwd
  let url
  let handoff
  for (const line of text.split('\n')) {
    if (line.startsWith('cwd=')) cwd = line.slice('cwd='.length)
    else if (line.startsWith('SPRING_DATASOURCE_URL=')) {
      url = line.slice('SPRING_DATASOURCE_URL='.length)
    } else if (line.startsWith('DONUT_WORKTREE_HANDOFF=')) {
      handoff = line.slice('DONUT_WORKTREE_HANDOFF='.length)
    } else if (line.startsWith('arg:')) javaArgs.push(line.slice('arg:'.length))
  }
  const jarAt = javaArgs.indexOf('-jar')
  const jarPath = jarAt >= 0 ? javaArgs[jarAt + 1] : undefined
  const wrapper = jarPath
    ? path.normalize(path.join(path.dirname(jarPath), '..', '..', 'gradlew'))
    : undefined
  const args = jarAt >= 0 ? javaArgs.slice(jarAt + 2) : javaArgs
  return { wrapper, cwd, url, args, javaArgs, handoff }
}

export function readGradleInvocation(checkout) {
  return parseGradleInvocation(readFileSync(checkout.gradleInvocation, 'utf8'))
}

export function readGradleInvocations(checkout) {
  const count = Number(
    readFileSync(path.join(checkout.root, 'gradle-invocation-count'), 'utf8')
  )
  const invocations = []
  for (let n = 1; n <= count; n += 1) {
    invocations.push(
      parseGradleInvocation(
        readFileSync(path.join(checkout.root, `gradle-invocation.${n}`), 'utf8')
      )
    )
  }
  return invocations
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
