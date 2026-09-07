import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  assertRefusedBeforeGradle,
  outputOf,
  runWrapper,
} from './backend-test-worktree-launcher-fixtures.mjs'

const assignedId = 'wt_a7c2'
const assignedDatabase = `doughnut_${assignedId}_test`
const assignedUrl = jdbcUrl(assignedDatabase)
const otherUrl = jdbcUrl('doughnut_test')

function configuredCheckout(t) {
  return makeCheckout(t, {
    config: JSON.stringify({ id: assignedId }),
  })
}

function runIsolatedMigrate(checkout, { args = [], env = {} } = {}) {
  return runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'migrateTestDB', ...args],
    env,
  })
}

test('configured wrapper conflicting SPRING_DATASOURCE_URL refuses before gradle', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    env: { SPRING_DATASOURCE_URL: otherUrl },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.match(outputOf(result), /SPRING_DATASOURCE_URL/)
  assert.doesNotMatch(outputOf(result), /Selected database/)
})

test('configured wrapper conflicting DB_URL refuses before gradle', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    env: { DB_URL: otherUrl },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /DB_URL/)
})

test('configured wrapper conflicting SPRING_FLYWAY_URL refuses before gradle', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    env: { SPRING_FLYWAY_URL: otherUrl },
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /SPRING_FLYWAY_URL/)
})

test('configured wrapper matching URL env still execs against the assigned database', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    env: {
      SPRING_DATASOURCE_URL: assignedUrl,
      DB_URL: assignedUrl,
      SPRING_FLYWAY_URL: assignedUrl,
    },
  })
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(readGradleInvocation(checkout).url, assignedUrl)
})

test('configured wrapper conflicting -Dspring.datasource.url refuses before gradle', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    args: [`-Dspring.datasource.url=${otherUrl}`],
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.match(outputOf(result), /spring\.datasource\.url/)
})

test('configured wrapper matching -Dspring.datasource.url still execs against the assigned database', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    args: [`-Dspring.datasource.url=${assignedUrl}`],
  })
  assert.equal(result.status, 0, outputOf(result))
  const invocation = readGradleInvocation(checkout)
  assert.equal(invocation.url, assignedUrl)
  assert.equal(
    invocation.args.includes(`-Dspring.datasource.url=${assignedUrl}`),
    true
  )
})

test('configured wrapper conflicting -Dspring.flyway.url refuses before gradle', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    args: [`-Dspring.flyway.url=${otherUrl}`],
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /spring\.flyway\.url/)
})

test('configured wrapper matching -Dspring.flyway.url still execs against the assigned database', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    args: [`-Dspring.flyway.url=${assignedUrl}`],
  })
  assert.equal(result.status, 0, outputOf(result))
  const invocation = readGradleInvocation(checkout)
  assert.equal(invocation.url, assignedUrl)
  assert.equal(
    invocation.args.includes(`-Dspring.flyway.url=${assignedUrl}`),
    true
  )
})

test('configured wrapper conflicting --spring.datasource.url refuses before gradle', (t) => {
  const checkout = configuredCheckout(t)
  const result = runIsolatedMigrate(checkout, {
    args: [`--spring.datasource.url=${otherUrl}`],
  })
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /spring\.datasource\.url/)
})
