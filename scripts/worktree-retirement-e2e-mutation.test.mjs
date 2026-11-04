import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  readPublishedE2ePortClaims,
  writePublishedE2ePortClaims,
} from './sut-e2e-port-claims.mjs'
import { makeClaimRoot } from './sut-e2e-port-test-helpers.mjs'
import { writeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import { worktreeLocalConfigPath } from './worktree-identity.mjs'
import {
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
import { runRetire } from './worktree-retirement-test-helpers.mjs'

test('second DROP failure reports partial retirement; retry completes both targets', async (t) => {
  const started = Date.now()
  const checkout = makeLinkedWorktreeCheckout(t)
  const claimRoot = makeClaimRoot(t)
  const claimFixture = {
    wt_e1a6: {
      backendPort: 19081,
      vitePort: 15174,
      lbListenPort: 15173,
    },
  }
  writePublishedE2ePortClaims(claimRoot, claimFixture)
  writeIsolatedConfig(checkout.root, {
    id: 'wt_e1a6',
    e2e: { database: 'doughnut_e2e_wt_e1a6' },
  })
  const mysql = await startDisposableMysql(t)
  const unitDb = 'doughnut_wt_e1a6_test'
  const e2eDb = 'doughnut_e2e_wt_e1a6'
  const peerDb = 'doughnut_peer_sentinel_e2e'
  const unrecordedE2eShaped = 'doughnut_e2e_wt_foreign'
  mysql.execSql(
    `CREATE DATABASE ${unitDb}; CREATE TABLE ${unitDb}.payload (note VARCHAR(64)); INSERT INTO ${unitDb}.payload VALUES ('unit-row');`
  )
  mysql.execSql(
    `CREATE DATABASE ${e2eDb}; CREATE TABLE ${e2eDb}.payload (note VARCHAR(64)); INSERT INTO ${e2eDb}.payload VALUES ('e2e-row');`
  )
  mysql.execSql(
    `CREATE DATABASE ${peerDb}; CREATE TABLE ${peerDb}.sentinel (v VARCHAR(32)); INSERT INTO ${peerDb}.sentinel VALUES ('peer-alive');`
  )
  mysql.execSql(
    `CREATE DATABASE ${unrecordedE2eShaped}; CREATE TABLE ${unrecordedE2eShaped}.sentinel (v VARCHAR(32)); INSERT INTO ${unrecordedE2eShaped}.sentinel VALUES ('untouched');`
  )
  const unbind = bindWorktreeRetirementMysqlTestAdapter({ port: mysql.port })
  t.after(unbind)

  let e2eDropAttempts = 0
  const partial = await runRetire(checkout.root, {
    hooks: {
      beforeDrop(database) {
        if (database === e2eDb) {
          e2eDropAttempts += 1
          if (e2eDropAttempts === 1) {
            throw new Error('injected second DROP failure')
          }
        }
      },
    },
  })
  assert.equal(partial.code, 1)
  assert.match(
    partial.err,
    /Partial worktree database retirement: E2E DROP failed/
  )
  assert.match(partial.err, new RegExp(`${unitDb} — dropped`))
  assert.match(partial.err, /injected second DROP failure/)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
  assert.equal(retirementSchemaExists(unitDb), false)
  assert.equal(retirementSchemaExists(e2eDb), true)
  assert.equal(mysql.query(`SELECT v FROM ${peerDb}.sentinel`), 'peer-alive')
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), claimFixture)

  const retried = await runRetire(checkout.root)
  const elapsedMs = Date.now() - started
  assert.equal(retried.code, 0, retried.err)
  assert.match(retried.out, new RegExp(`${unitDb} — already absent`))
  assert.match(retried.out, new RegExp(`${e2eDb} — dropped`))
  assert.equal(retirementSchemaExists(unitDb), false)
  assert.equal(retirementSchemaExists(e2eDb), false)
  assert.equal(mysql.query(`SELECT v FROM ${peerDb}.sentinel`), 'peer-alive')
  assert.equal(
    mysql.query(`SELECT v FROM ${unrecordedE2eShaped}.sentinel`),
    'untouched'
  )
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), claimFixture)
  assert.equal(isCheckoutRetired(checkout.root), true)
  assert.equal(existsSync(worktreeLocalConfigPath(checkout.root)), true)
  assert.match(mysql.version, /^8\.4\./)
  console.log(
    JSON.stringify({
      proof: 'slice-5-unit-and-e2e-schema-deletion',
      mysqlEngineVersion: mysql.version,
      mysqlPort: mysql.port,
      unitDatabase: unitDb,
      e2eDatabase: e2eDb,
      unitSchemaPresentAfter: retirementSchemaExists(unitDb),
      e2eSchemaPresentAfter: retirementSchemaExists(e2eDb),
      peerSentinel: mysql.query(`SELECT v FROM ${peerDb}.sentinel`),
      unrecordedE2eShapedSentinel: mysql.query(
        `SELECT v FROM ${unrecordedE2eShaped}.sentinel`
      ),
      portClaimsUnchanged: readPublishedE2ePortClaims(claimRoot),
      markerPresent: isCheckoutRetired(checkout.root),
      identityPreserved: existsSync(worktreeLocalConfigPath(checkout.root)),
      elapsedMs,
      literalTestCommand:
        'CURSOR_DEV=true nix develop -c node --test scripts/worktree-retirement*.test.mjs',
    })
  )
})

test('idle unit+E2E allocation drops both schemas and preserves peer and claims', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const claimRoot = makeClaimRoot(t)
  const claimFixture = {
    wt_f2b7: {
      backendPort: 19181,
      vitePort: 15274,
      lbListenPort: 15273,
    },
  }
  writePublishedE2ePortClaims(claimRoot, claimFixture)
  writeIsolatedConfig(checkout.root, {
    id: 'wt_f2b7',
    e2e: { database: 'doughnut_e2e_wt_f2b7' },
  })
  const mysql = await startDisposableMysql(t)
  const unitDb = 'doughnut_wt_f2b7_test'
  const e2eDb = 'doughnut_e2e_wt_f2b7'
  const peerDb = 'doughnut_peer_sentinel_both'
  mysql.execSql(
    `CREATE DATABASE ${unitDb}; CREATE TABLE ${unitDb}.payload (note VARCHAR(64)); INSERT INTO ${unitDb}.payload VALUES ('unit-row');`
  )
  mysql.execSql(
    `CREATE DATABASE ${e2eDb}; CREATE TABLE ${e2eDb}.payload (note VARCHAR(64)); INSERT INTO ${e2eDb}.payload VALUES ('e2e-row');`
  )
  mysql.execSql(
    `CREATE DATABASE ${peerDb}; CREATE TABLE ${peerDb}.sentinel (v VARCHAR(32)); INSERT INTO ${peerDb}.sentinel VALUES ('peer-ok');`
  )
  const unbind = bindWorktreeRetirementMysqlTestAdapter({ port: mysql.port })
  t.after(unbind)

  const result = await runRetire(checkout.root)
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /Worktree database retirement complete/)
  assert.match(result.out, new RegExp(`${unitDb} — dropped`))
  assert.match(result.out, new RegExp(`${e2eDb} — dropped`))
  assert.equal(retirementSchemaExists(unitDb), false)
  assert.equal(retirementSchemaExists(e2eDb), false)
  assert.equal(mysql.query(`SELECT v FROM ${peerDb}.sentinel`), 'peer-ok')
  assert.deepEqual(readPublishedE2ePortClaims(claimRoot), claimFixture)
  assert.equal(isCheckoutRetired(checkout.root), true)
  assert.equal(existsSync(worktreeLocalConfigPath(checkout.root)), true)
})

test('unrecorded E2E-shaped database stays untouched when only unit is recorded', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_g3c8' })
  const mysql = await startDisposableMysql(t)
  const unitDb = 'doughnut_wt_g3c8_test'
  const unrecorded = 'doughnut_e2e_wt_g3c8'
  mysql.execSql(
    `CREATE DATABASE ${unitDb}; CREATE TABLE ${unitDb}.t (id INT); INSERT INTO ${unitDb}.t VALUES (1);`
  )
  mysql.execSql(
    `CREATE DATABASE ${unrecorded}; CREATE TABLE ${unrecorded}.sentinel (v VARCHAR(32)); INSERT INTO ${unrecorded}.sentinel VALUES ('keep-me');`
  )
  const unbind = bindWorktreeRetirementMysqlTestAdapter({ port: mysql.port })
  t.after(unbind)

  const result = await runRetire(checkout.root)
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, new RegExp(`${unitDb} — dropped`))
  assert.equal(result.out.includes(unrecorded), false)
  assert.equal(retirementSchemaExists(unitDb), false)
  assert.equal(retirementSchemaExists(unrecorded), true)
  assert.equal(mysql.query(`SELECT v FROM ${unrecorded}.sentinel`), 'keep-me')
})
