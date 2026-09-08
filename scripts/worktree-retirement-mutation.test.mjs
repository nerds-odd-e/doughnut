import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { writeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import { worktreeLocalConfigPath } from './worktree-identity.mjs'
import {
  RETIREMENT_ADMISSION_GATE_DIR_NAME,
  RETIREMENT_MARKER_NAME,
  acquireRetirementAdmission,
  isCheckoutRetired,
  retirementAdmissionGatePath,
  retirementMarkerPath,
} from './worktree-retirement-admission.mjs'
import { startDisposableMysql } from './worktree-retirement-disposable-mysql.mjs'
import {
  bindWorktreeRetirementMysqlTestAdapter,
  retirementSchemaExists,
} from './worktree-retirement-mysql.mjs'
import {
  clearEvidenceDeps,
  makeWritable,
} from './worktree-retirement-test-helpers.mjs'
import { runWorktreeRetire } from './worktree-retirement.mjs'

async function runRetire(checkoutRoot, extras = {}) {
  const out = makeWritable()
  const err = makeWritable()
  const code = await runWorktreeRetire({
    argv: [],
    checkoutRoot,
    out,
    err,
    evidenceDeps: clearEvidenceDeps,
    ...extras,
  })
  return { code, out: out.content(), err: err.content() }
}

test('recorded E2E allocation refuses mutation before marker or DROP', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, {
    id: 'wt_a7c2',
    e2e: { database: 'doughnut_e2e_wt_a7c2' },
  })
  const drops = []
  const result = await runRetire(checkout.root, {
    mysqlExecFn() {
      drops.push('called')
      return ''
    },
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /recorded E2E database/)
  assert.match(result.err, /not reclaimable yet/)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), false)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  assert.equal(drops.length, 0)
})

test('competing runner cannot pass the admission gate during retirement', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  let sawHeldGate = false
  const result = await runRetire(checkout.root, {
    mysqlExecFn: () => '',
    hooks: {
      afterGateAcquired(root) {
        assert.equal(existsSync(retirementAdmissionGatePath(root)), true)
        assert.throws(
          () => acquireRetirementAdmission(root),
          new RegExp(RETIREMENT_ADMISSION_GATE_DIR_NAME)
        )
        sawHeldGate = true
      },
    },
  })
  assert.equal(sawHeldGate, true)
  assert.equal(result.code, 0, result.err)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  assert.equal(isCheckoutRetired(checkout.root), true)
})

test('interruption after marker: starts blocked; retry completes same targets', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const mysql = await startDisposableMysql(t)
  const unitDb = 'doughnut_wt_a7c2_test'
  const peerDb = 'doughnut_peer_sentinel_retire'
  mysql.execSql(
    `CREATE DATABASE ${unitDb}; CREATE TABLE ${unitDb}.t (id INT); INSERT INTO ${unitDb}.t VALUES (1);`
  )
  mysql.execSql(
    `CREATE DATABASE ${peerDb}; CREATE TABLE ${peerDb}.sentinel (v VARCHAR(32)); INSERT INTO ${peerDb}.sentinel VALUES ('peer-ok');`
  )
  const unbind = bindWorktreeRetirementMysqlTestAdapter({ port: mysql.port })
  t.after(unbind)

  const interrupted = await runRetire(checkout.root, {
    hooks: {
      afterMarkerPublished() {
        throw new Error('injected interruption after marker publication')
      },
    },
  })
  assert.equal(interrupted.code, 1)
  assert.match(interrupted.err, /injected interruption/)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
  assert.equal(retirementSchemaExists(unitDb), true)
  assert.equal(mysql.query(`SELECT v FROM ${peerDb}.sentinel`), 'peer-ok')

  const retried = await runRetire(checkout.root)
  assert.equal(retried.code, 0, retried.err)
  assert.match(retried.out, new RegExp(`${unitDb} — dropped`))
  assert.equal(retirementSchemaExists(unitDb), false)
  assert.equal(mysql.query(`SELECT v FROM ${peerDb}.sentinel`), 'peer-ok')
  assert.equal(isCheckoutRetired(checkout.root), true)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
  assert.equal(
    existsSync(worktreeLocalConfigPath(checkout.root)),
    true,
    'identity must remain'
  )
  assert.match(
    readFileSync(worktreeLocalConfigPath(checkout.root), 'utf8'),
    /wt_a7c2/
  )
})

test('idle unit-only allocation drops target schema and preserves peer sentinel', async (t) => {
  const started = Date.now()
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_b8d3' })
  const mysql = await startDisposableMysql(t)
  const unitDb = 'doughnut_wt_b8d3_test'
  const peerDb = 'doughnut_peer_sentinel_unit'
  mysql.execSql(
    `CREATE DATABASE ${unitDb}; CREATE TABLE ${unitDb}.payload (note VARCHAR(64)); INSERT INTO ${unitDb}.payload VALUES ('target-row');`
  )
  mysql.execSql(
    `CREATE DATABASE ${peerDb}; CREATE TABLE ${peerDb}.sentinel (v VARCHAR(32)); INSERT INTO ${peerDb}.sentinel VALUES ('peer-alive');`
  )
  const unbind = bindWorktreeRetirementMysqlTestAdapter({ port: mysql.port })
  t.after(unbind)

  const result = await runRetire(checkout.root)
  const elapsedMs = Date.now() - started
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /Worktree id: wt_b8d3/)
  assert.match(result.out, new RegExp(`${unitDb} — dropped`))
  assert.equal(retirementSchemaExists(unitDb), false)
  assert.equal(mysql.query(`SELECT v FROM ${peerDb}.sentinel`), 'peer-alive')
  assert.equal(isCheckoutRetired(checkout.root), true)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
  assert.equal(existsSync(worktreeLocalConfigPath(checkout.root)), true)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)

  // Proof metadata for the PLAN (external MySQL runtime exception).
  assert.match(mysql.version, /^8\.4\./)
  console.log(
    JSON.stringify({
      proof: 'slice-4-unit-schema-deletion',
      mysqlEngineVersion: mysql.version,
      mysqlPort: mysql.port,
      unitDatabase: unitDb,
      unitSchemaPresentAfter: retirementSchemaExists(unitDb),
      peerSentinel: mysql.query(`SELECT v FROM ${peerDb}.sentinel`),
      markerPresent: isCheckoutRetired(checkout.root),
      identityPreserved: existsSync(worktreeLocalConfigPath(checkout.root)),
      elapsedMs,
      literalTestCommand:
        'CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement*.test.mjs',
    })
  )
})

test('missing unit schema counts as already absent; still marks retired', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_c9e4' })
  const mysql = await startDisposableMysql(t)
  const unbind = bindWorktreeRetirementMysqlTestAdapter({ port: mysql.port })
  t.after(unbind)
  assert.equal(retirementSchemaExists('doughnut_wt_c9e4_test'), false)

  const result = await runRetire(checkout.root)
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /already absent/)
  assert.equal(isCheckoutRetired(checkout.root), true)
  assert.equal(existsSync(worktreeLocalConfigPath(checkout.root)), true)
})

test('DROP failure after marker reports partial progress and retains marker', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_d0f5' })
  const result = await runRetire(checkout.root, {
    mysqlExecFn(_file, args) {
      if (String(args.at(-1)).includes('SCHEMATA')) {
        return 'doughnut_wt_d0f5_test\n'
      }
      throw new Error('simulated mysql DROP failure')
    },
  })
  assert.equal(result.code, 1)
  assert.match(result.err, /Partial worktree database retirement/)
  assert.match(result.err, /doughnut_wt_d0f5_test/)
  assert.match(result.err, /simulated mysql DROP failure/)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
})
