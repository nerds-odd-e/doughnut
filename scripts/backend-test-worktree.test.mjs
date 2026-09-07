import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import {
  chmodSync,
  copyFileSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const launcherSrc = fileURLToPath(
  new URL('./backend-test-worktree.sh', import.meta.url)
)

const jdbcParams =
  'connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'

function jdbcUrl(database) {
  return `jdbc:mysql://127.0.0.1:3309/${database}?${jdbcParams}`
}

function makeCheckout(t, { config } = {}) {
  const root = mkdtempSync(path.join(tmpdir(), 'backend-test-worktree-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  mkdirSync(path.join(root, 'scripts'), { recursive: true })
  mkdirSync(path.join(root, 'backend'), { recursive: true })

  const launcher = path.join(root, 'scripts', 'backend-test-worktree.sh')
  copyFileSync(launcherSrc, launcher)
  chmodSync(launcher, 0o755)

  const gradleMarker = path.join(root, 'gradle-reached')
  writeFileSync(
    path.join(root, 'backend', 'gradlew'),
    [
      '#!/bin/sh',
      "printf 'GRADLE_REACHED\\n' >&2",
      `touch ${JSON.stringify(gradleMarker)}`,
      'exit 99',
      '',
    ].join('\n')
  )
  chmodSync(path.join(root, 'backend', 'gradlew'), 0o755)

  if (config !== undefined) {
    writeFileSync(path.join(root, '.worktree.local.json'), config)
  }

  return { root, launcher, gradleMarker }
}

function runLauncher(checkout, { env = {}, args = [] } = {}) {
  const childEnv = { ...process.env }
  delete childEnv.SPRING_DATASOURCE_URL
  delete childEnv.DB_URL
  delete childEnv.SPRING_FLYWAY_URL
  Object.assign(childEnv, env)
  return spawnSync(checkout.launcher, args, {
    cwd: checkout.root,
    encoding: 'utf8',
    env: childEnv,
  })
}

function outputOf(result) {
  return `${result.stdout}${result.stderr}`
}

function assertRefusedBeforeGradle(checkout, result) {
  assert.equal(result.error, undefined, result.stderr)
  assert.notEqual(result.status, 0)
  assert.equal(existsSync(checkout.gradleMarker), false)
  assert.doesNotMatch(outputOf(result), /GRADLE_REACHED/)
}

test('missing configuration refuses before gradle', (t) => {
  const checkout = makeCheckout(t)
  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /ENOENT|no such file/i)
})

test('malformed configuration refuses before gradle', (t) => {
  const checkout = makeCheckout(t, { config: '{"id":' })
  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /SyntaxError/i)
})

test('invalid id refuses before gradle', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'not-a-worktree-id' }),
  })
  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /wt_\[a-z0-9_\]\{1,32\}/)
})

test('conflicting SPRING_DATASOURCE_URL refuses before gradle', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, {
    env: {
      SPRING_DATASOURCE_URL: jdbcUrl('doughnut_test'),
    },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /SPRING_DATASOURCE_URL/)
  assert.doesNotMatch(outputOf(result), /Selected database/)
})

test('conflicting DB_URL refuses before gradle', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, {
    env: { DB_URL: jdbcUrl('doughnut_test') },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /DB_URL/)
})

test('conflicting SPRING_FLYWAY_URL refuses before gradle', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, {
    env: { SPRING_FLYWAY_URL: jdbcUrl('doughnut_test') },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /SPRING_FLYWAY_URL/)
})

test('valid id shows derived database and refuses execution', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.match(result.stdout, /Selected database: doughnut_wt_a7c2_test/)
  assert.match(result.stderr, /not enabled yet/i)
})

test('matching URL overrides reach the same temporary refusal', (t) => {
  const expected = jdbcUrl('doughnut_wt_a7c2_test')
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, {
    env: {
      SPRING_DATASOURCE_URL: expected,
      DB_URL: expected,
      SPRING_FLYWAY_URL: expected,
    },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(result.stdout, /Selected database: doughnut_wt_a7c2_test/)
  assert.match(result.stderr, /not enabled yet/i)
})

test('command arguments are refused before configuration or gradle', (t) => {
  const checkout = makeCheckout(t)
  const result = runLauncher(checkout, { args: ['--tests', 'Foo'] })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /Usage: pnpm backend:test:worktree/)
  assert.doesNotMatch(outputOf(result), /ENOENT|no such file/i)
})
