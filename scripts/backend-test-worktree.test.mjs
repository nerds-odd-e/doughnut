import assert from 'node:assert/strict'
import { existsSync, readFileSync, realpathSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  assertRefusedBeforeGradle,
  outputOf,
  runLauncher,
} from './backend-test-worktree-launcher-fixtures.mjs'

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

test('valid configuration execs one gradle migrate-then-test run', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout)
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0)
  assert.match(
    result.stdout,
    /Selected database: doughnut_wt_a7c2_test[\s\S]*GRADLE_STDOUT/
  )
  assert.match(result.stderr, /GRADLE_REACHED/)

  const invocation = readGradleInvocation(checkout)
  assert.equal(
    realpathSync(invocation.wrapper),
    realpathSync(path.join(checkout.root, 'backend', 'gradlew'))
  )
  assert.equal(realpathSync(invocation.cwd), realpathSync(checkout.root))
  assert.equal(invocation.url, jdbcUrl('doughnut_wt_a7c2_test'))

  const { args } = invocation
  assert.equal(args[args.indexOf('-p') + 1], 'backend')
  assert.equal(args.includes('-PworktreeTestRun'), true)
  assert.equal(args.includes('-Dspring.profiles.active=test'), true)
  assert.equal(args.includes('--rerun-tasks'), true)
  assert.equal(args.includes('--no-build-cache'), true)
  assert.equal(args.includes('--no-daemon'), true)
  assert.equal(args.includes('--continue'), false)
  const migrateAt = args.indexOf('migrateTestDB')
  const testAt = args.indexOf('test')
  assert.ok(migrateAt >= 0, args.join(' '))
  assert.ok(testAt > migrateAt, args.join(' '))
  assert.equal(args.includes('--tests'), false)
})

test('matching URL overrides still exec gradle against the selected database', (t) => {
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
  assert.equal(result.status, 0, result.stderr)
  assert.equal(readGradleInvocation(checkout).url, expected)
})

test('gradle child failure stays nonzero', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, { env: { FAKE_GRADLE_EXIT: '7' } })
  assert.equal(existsSync(checkout.gradleInvocation), true)
  assert.equal(result.status, 7)
})

test('worktreeTestRun opts test into mustRunAfter migrateTestDB', () => {
  const gradle = readFileSync(
    fileURLToPath(new URL('../backend/build.gradle', import.meta.url)),
    'utf8'
  )
  const optIn =
    /if\s*\(\s*project\.hasProperty\(\s*['"]worktreeTestRun['"]\s*\)\s*\)\s*\{\s*tasks\.named\(\s*['"]test['"]\s*\)\s*\{\s*mustRunAfter\s+['"]migrateTestDB['"]\s*\}\s*\}/
  assert.match(gradle, optIn)
  const unguarded = gradle.replace(optIn, '')
  assert.doesNotMatch(unguarded, /mustRunAfter\s+['"]migrateTestDB['"]/)
  assert.doesNotMatch(unguarded, /dependsOn\(?\s*['"]migrateTestDB['"]/)
})

test('focused --tests pattern is one token after the test task', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, { args: ['--tests', '*.FooTest'] })
  assert.equal(result.status, 0, result.stderr)
  const { args } = readGradleInvocation(checkout)
  const testAt = args.indexOf('test')
  assert.equal(args[testAt + 1], '--tests')
  assert.equal(args[testAt + 2], '*.FooTest')
})

test('unmatched --tests filter keeps gradle failure', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runLauncher(checkout, {
    args: ['--tests', 'DoesNotMatch'],
    env: { FAKE_GRADLE_EXIT: '1' },
  })
  assert.equal(existsSync(checkout.gradleInvocation), true)
  assert.equal(result.status, 1)
})

test('missing --tests value refuses before configuration or gradle', (t) => {
  const checkout = makeCheckout(t)
  for (const args of [['--tests'], ['--tests', '']]) {
    const result = runLauncher(checkout, { args })
    assertRefusedBeforeGradle(checkout, result)
    assert.match(outputOf(result), /Usage: pnpm backend:test:worktree/)
    assert.doesNotMatch(outputOf(result), /ENOENT|no such file/i)
  }
})

test('unsupported arguments refuse before configuration or gradle', (t) => {
  const checkout = makeCheckout(t)
  const unsupported = [
    ['--continue'],
    ['test'],
    ['migrateTestDB'],
    ['--tests', 'Foo', 'extra'],
    ['-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3309/other'],
    ['--spring.datasource.url=jdbc:mysql://127.0.0.1:3309/other'],
  ]
  for (const args of unsupported) {
    const result = runLauncher(checkout, { args })
    assertRefusedBeforeGradle(checkout, result)
    assert.match(outputOf(result), /Usage: pnpm backend:test:worktree/)
    assert.doesNotMatch(outputOf(result), /ENOENT|no such file/i)
  }
})
