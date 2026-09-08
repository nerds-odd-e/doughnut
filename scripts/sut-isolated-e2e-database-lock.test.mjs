import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { setTimeout as delay } from 'node:timers/promises'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { readMysqlInvocation } from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  e2eDatabaseNameForIdentity,
  e2eDatabaseProvisioningSql,
} from './sut-e2e-database.mjs'
import {
  identityAndPortsConfig,
  readIsolatedConfig,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'

const e2eDatabaseModuleHref = new URL('./sut-e2e-database.mjs', import.meta.url)
  .href

const expectedDatabase = e2eDatabaseNameForIdentity(identityAndPortsConfig.id)

function runEnsureProcess(checkout, extraEnv = {}) {
  const child = spawn(
    process.execPath,
    [
      '--input-type=module',
      '-e',
      `import { ensureIsolatedE2eDatabase } from ${JSON.stringify(
        e2eDatabaseModuleHref
      )}
ensureIsolatedE2eDatabase(${JSON.stringify(checkout.root)}, {
  schemaExistsFn: () => false,
})
`,
    ],
    {
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${checkout.binDir}${path.delimiter}${process.env.PATH ?? ''}`,
        ...extraEnv,
      },
    }
  )
  child.stdin.end()
  return child
}

function waitForClose(child) {
  return new Promise((resolve) => {
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (chunk) => {
      stdout += chunk
    })
    child.stderr.on('data', (chunk) => {
      stderr += chunk
    })
    child.on('close', (status) => {
      resolve({ status, stdout, stderr })
    })
  })
}

async function waitForFile(filePath, timeoutMs = 3000) {
  const deadline = Date.now() + timeoutMs
  while (!existsSync(filePath)) {
    if (Date.now() >= deadline) {
      throw new Error(`timed out waiting for ${filePath}`)
    }
    await delay(20)
  }
}

test('two same-checkout first-use starts publish one complete allocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root, identityAndPortsConfig)
  const first = runEnsureProcess(checkout, { MYSQL_HOLD: '1' })
  const firstDone = waitForClose(first)
  await waitForFile(checkout.mysqlReached)

  const second = runEnsureProcess(checkout)
  const secondDone = waitForClose(second)
  await delay(80)

  writeFileSync(checkout.mysqlRelease, 'go\n')
  const [firstResult, secondResult] = await Promise.all([firstDone, secondDone])
  assert.equal(firstResult.status, 0, firstResult.stderr)
  assert.equal(secondResult.status, 0, secondResult.stderr)
  assert.equal(
    readMysqlInvocation(checkout).args.at(-1),
    e2eDatabaseProvisioningSql(expectedDatabase)
  )
  assert.equal(readIsolatedConfig(checkout.root).e2e.database, expectedDatabase)
  assert.equal(readIsolatedConfig(checkout.root).e2e.backendPort, 19081)
  assert.equal(existsSync(`${checkout.root}/.worktree.local.lock`), false)
})
