import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-cypress.mjs'
import {
  afterReset,
  afterSeed,
  openAiMockIsolationProofParams,
  waitBeforeReset,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_ROLE,
  OPENAI_MOCK_ISOLATION_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_SUGGESTION,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
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

test('openai-mock mode uses the OpenAI completion spec and proof env', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWorktreeResetIsolation({
      peerRoot: '/peer',
      resetterRoot: '/resetter',
      barrierDir: dir,
      mode: 'openai-mock',
      loadAllocationFn: fakeAllocation,
      isPortOccupiedFn: async () => false,
      peerProof: {
        suggestion: 'A custom peer suggestion.',
        requestMarker: 'CUSTOM_PEER_MARKER',
      },
      resetterProof: {
        suggestion: 'A custom resetter suggestion.',
        requestMarker: 'CUSTOM_RESETTER_MARKER',
      },
      log: () => undefined,
      spawnCypress: (cwd, env) => {
        spawned.push({
          cwd,
          env,
          proofParams: openAiMockIsolationProofParams(env),
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
    })
    assert.equal(result.peerSpec, SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC)
    assert.equal(result.resetterSpec, SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC)
    assert.equal(result.mode, 'openai-mock')
    assert.equal(
      spawned[0].env[WORKTREE_RESET_ISOLATION_BARRIER_AT],
      WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK
    )
    assert.equal(
      spawned[0].env[OPENAI_MOCK_ISOLATION_REQUEST_MARKER],
      'CUSTOM_PEER_MARKER'
    )
    assert.equal(
      spawned[1].env[OPENAI_MOCK_ISOLATION_REQUEST_MARKER],
      'CUSTOM_RESETTER_MARKER'
    )
    assert.equal(
      spawned[0].env[OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER],
      'CUSTOM_RESETTER_MARKER'
    )
    assert.equal(
      spawned[1].env[OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER],
      'CUSTOM_PEER_MARKER'
    )
    assert.deepEqual(spawned[0].proofParams, {
      suggestion: 'A custom peer suggestion.',
      requestMarker: 'CUSTOM_PEER_MARKER',
      foreignRequestMarker: 'CUSTOM_RESETTER_MARKER',
    })
    assert.deepEqual(spawned[1].proofParams, {
      suggestion: 'A custom resetter suggestion.',
      requestMarker: 'CUSTOM_RESETTER_MARKER',
      foreignRequestMarker: 'CUSTOM_PEER_MARKER',
    })
    assert.notEqual(
      spawned[0].env[OPENAI_MOCK_ISOLATION_SUGGESTION],
      spawned[1].env[OPENAI_MOCK_ISOLATION_SUGGESTION]
    )
  })
})
