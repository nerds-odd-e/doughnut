import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import { SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC } from './isolated-cypress.mjs'
import {
  afterReset,
  afterSeed,
  waitBeforeReset,
  wikidataMockIsolationProofParams,
  WIKIDATA_MOCK_ISOLATION_ENTITY_LABEL,
  WIKIDATA_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER,
  WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_WIKIDATA_MOCK,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_ROLE,
} from './worktree-reset-isolation-barrier.mjs'
import { runPairedWorktreeResetIsolation } from './worktree-reset-isolation-harness.mjs'
import { withWorktreeResetIsolationBarrierDir } from './worktree-reset-isolation-test-helpers.mjs'

const fakeAllocation = (root) => ({
  e2e: {
    backendPort: root === '/peer' ? 40001 : 50001,
    vitePort: root === '/peer' ? 40002 : 50002,
    lbListenPort: root === '/peer' ? 40003 : 50003,
  },
})

// Two concurrent worktree invocations using the newly admitted Wikidata
// feature complete independently; reinstalling one Wikidata imposter leaves
// the peer's note and mock response unchanged. Reuses the same paired reset
// barrier harness and ownership/coordination logic as the OpenAI peer proof
// (a regression there); this is the minimal Wikidata response observation.
const runPairedWikidataMock = ({ spawned, ...rest }) =>
  runPairedWorktreeResetIsolation({
    peerRoot: '/peer',
    resetterRoot: '/resetter',
    mode: 'wikidata-mock',
    loadAllocationFn: fakeAllocation,
    isPortOccupiedFn: async () => false,
    log: () => undefined,
    spawnCypress: (cwd, env) => {
      spawned.push({
        cwd,
        env,
        proofParams: wikidataMockIsolationProofParams(env),
      })
      const child = new EventEmitter()
      queueMicrotask(async () => {
        if (
          env[WORKTREE_RESET_ISOLATION_ROLE] ===
          WORKTREE_RESET_ISOLATION_PEER_ROLE
        ) {
          await afterSeed({ env, timeoutMs: 2_000, pollMs: 10 })
        } else {
          await waitBeforeReset({ env, timeoutMs: 2_000, pollMs: 10 })
          afterReset({ env })
        }
        child.emit('close', 0)
      })
      return child
    },
    ...rest,
  })

test('wikidata-mock mode uses the Wikidata spec and proof env', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWikidataMock({
      spawned,
      peerWikidataProof: {
        entityLabel: 'A custom peer entity.',
        requestMarker: 'CUSTOM_PEER_MARKER',
      },
      resetterWikidataProof: {
        entityLabel: 'A custom resetter entity.',
        requestMarker: 'CUSTOM_RESETTER_MARKER',
      },
    })
    assert.equal(result.peerSpec, SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC)
    assert.equal(result.resetterSpec, SUPPORTED_ISOLATED_WIKIDATA_MOCK_SPEC)
    assert.equal(result.mode, 'wikidata-mock')
    assert.equal(
      spawned[0].env[WORKTREE_RESET_ISOLATION_BARRIER_AT],
      WORKTREE_RESET_ISOLATION_BARRIER_AT_WIKIDATA_MOCK
    )
    assert.equal(
      spawned[0].env[WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER],
      'CUSTOM_PEER_MARKER'
    )
    assert.equal(
      spawned[1].env[WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER],
      'CUSTOM_RESETTER_MARKER'
    )
    assert.equal(
      spawned[0].env[WIKIDATA_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER],
      'CUSTOM_RESETTER_MARKER'
    )
    assert.equal(
      spawned[1].env[WIKIDATA_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER],
      'CUSTOM_PEER_MARKER'
    )
    assert.deepEqual(spawned[0].proofParams, {
      entityLabel: 'A custom peer entity.',
      requestMarker: 'CUSTOM_PEER_MARKER',
      foreignRequestMarker: 'CUSTOM_RESETTER_MARKER',
    })
    assert.deepEqual(spawned[1].proofParams, {
      entityLabel: 'A custom resetter entity.',
      requestMarker: 'CUSTOM_RESETTER_MARKER',
      foreignRequestMarker: 'CUSTOM_PEER_MARKER',
    })
    assert.notEqual(
      spawned[0].env[WIKIDATA_MOCK_ISOLATION_ENTITY_LABEL],
      spawned[1].env[WIKIDATA_MOCK_ISOLATION_ENTITY_LABEL]
    )
  })
})

test('paired wikidata-mock invocations keep distinct origins and leave no owned survivors', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWikidataMock({ spawned })
    // Paired command: exactly two owned invocations against one barrier.
    assert.equal(spawned.length, 2)
    // Origins: distinct checkouts, peer seeds before resetter resets.
    assert.equal(spawned[0].cwd, '/peer')
    assert.equal(spawned[1].cwd, '/resetter')
    assert.ok(
      Number(result.events.resetterResetAt) >=
        Number(result.events.peerSeededAt),
      'resetter reset must not precede peer seed'
    )
    // Responses: each role records only its own marker; the foreign marker
    // is the peer's, so reinstalling one imposter cannot change the peer's
    // recorded response.
    assert.notEqual(
      spawned[0].env[WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER],
      spawned[1].env[WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER]
    )
    assert.equal(
      spawned[0].env[WIKIDATA_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER],
      spawned[1].env[WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER]
    )
    assert.equal(
      spawned[1].env[WIKIDATA_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER],
      spawned[0].env[WIKIDATA_MOCK_ISOLATION_REQUEST_MARKER]
    )
    // No owned survivors: both roles' allocated application ports are free.
    assert.deepEqual(result.ownedListenersRemaining, {
      peer: [],
      resetter: [],
    })
    assert.equal(result.peerExit.code, 0)
    assert.equal(result.resetterExit.code, 0)
  })
})
