import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  initializeWorktreeIdentity,
  WORKTREE_ID_PATTERN,
  worktreeLocalConfigPath,
} from './worktree-identity.mjs'

const identityModuleHref = new URL('./worktree-identity.mjs', import.meta.url)
  .href

function makeCheckout(t) {
  const root = mkdtempSync(path.join(tmpdir(), 'worktree-identity-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  return root
}

function writeLiveBackendRunLock(checkoutRoot) {
  const lockDir = path.join(checkoutRoot, '.worktree.local.lock')
  mkdirSync(lockDir)
  writeFileSync(path.join(lockDir, 'owner.pid'), String(process.pid))
  return lockDir
}

test('initializer creates identity without a backend-test run lock', (t) => {
  const checkoutRoot = makeCheckout(t)
  const identity = initializeWorktreeIdentity(checkoutRoot)
  assert.match(identity.id, WORKTREE_ID_PATTERN)
  assert.deepEqual(
    JSON.parse(readFileSync(worktreeLocalConfigPath(checkoutRoot), 'utf8')),
    { id: identity.id }
  )
  assert.equal(
    existsSync(path.join(checkoutRoot, '.worktree.local.lock')),
    false
  )
  assert.equal(
    existsSync(path.join(checkoutRoot, '.worktree.identity.lock')),
    false
  )
})

test('initializer returns identity while a backend-test run lock is held', (t) => {
  const checkoutRoot = makeCheckout(t)
  const runLockDir = writeLiveBackendRunLock(checkoutRoot)
  const ownerBefore = readFileSync(path.join(runLockDir, 'owner.pid'), 'utf8')

  const identity = initializeWorktreeIdentity(checkoutRoot)
  assert.match(identity.id, WORKTREE_ID_PATTERN)
  assert.equal(
    readFileSync(path.join(runLockDir, 'owner.pid'), 'utf8'),
    ownerBefore
  )
  assert.equal(existsSync(runLockDir), true)
})

test('initializer reuses an existing identity-only config', (t) => {
  const checkoutRoot = makeCheckout(t)
  const existing = { id: 'wt_existing1' }
  writeFileSync(worktreeLocalConfigPath(checkoutRoot), JSON.stringify(existing))
  assert.deepEqual(initializeWorktreeIdentity(checkoutRoot), existing)
  assert.equal(
    readFileSync(worktreeLocalConfigPath(checkoutRoot), 'utf8'),
    JSON.stringify(existing)
  )
})

test('initializer preserves extra fields on an existing identity', (t) => {
  const checkoutRoot = makeCheckout(t)
  const existing = {
    id: 'wt_existing2',
    e2e: { database: 'doughnut_e2e_wt_existing2' },
  }
  const raw = JSON.stringify(existing)
  writeFileSync(worktreeLocalConfigPath(checkoutRoot), raw)
  assert.deepEqual(initializeWorktreeIdentity(checkoutRoot), {
    id: existing.id,
  })
  assert.equal(readFileSync(worktreeLocalConfigPath(checkoutRoot), 'utf8'), raw)
})

test('initializer ignores SUT datasource overrides such as INPUT_DB_URL', (t) => {
  const checkoutRoot = makeCheckout(t)
  const previous = process.env.INPUT_DB_URL
  process.env.INPUT_DB_URL = 'jdbc:mysql://127.0.0.1:3309/doughnut_e2e_test'
  try {
    const identity = initializeWorktreeIdentity(checkoutRoot)
    assert.match(identity.id, WORKTREE_ID_PATTERN)
  } finally {
    if (previous === undefined) delete process.env.INPUT_DB_URL
    else process.env.INPUT_DB_URL = previous
  }
})

function runInitializeProcess(checkoutRoot) {
  const child = spawn(
    process.execPath,
    [
      '--input-type=module',
      '-e',
      `import { initializeWorktreeIdentity } from ${JSON.stringify(
        identityModuleHref
      )}
process.stdout.write(initializeWorktreeIdentity(${JSON.stringify(
        checkoutRoot
      )}).id)
`,
    ],
    { encoding: 'utf8' }
  )
  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (chunk) => {
    stdout += chunk
  })
  child.stderr.on('data', (chunk) => {
    stderr += chunk
  })
  return new Promise((resolve) => {
    child.on('close', (status) => {
      resolve({ status, stdout, stderr })
    })
  })
}

test('concurrent initialize publishes one identity without a run lock', async (t) => {
  const checkoutRoot = makeCheckout(t)
  const [firstResult, secondResult] = await Promise.all([
    runInitializeProcess(checkoutRoot),
    runInitializeProcess(checkoutRoot),
  ])

  assert.equal(firstResult.status, 0, firstResult.stderr)
  assert.equal(secondResult.status, 0, secondResult.stderr)
  assert.equal(firstResult.stdout, secondResult.stdout)
  assert.match(firstResult.stdout, WORKTREE_ID_PATTERN)
  assert.deepEqual(
    JSON.parse(readFileSync(worktreeLocalConfigPath(checkoutRoot), 'utf8')),
    { id: firstResult.stdout }
  )
  assert.equal(
    existsSync(path.join(checkoutRoot, '.worktree.local.lock')),
    false
  )
})
