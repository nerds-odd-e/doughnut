import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-openai-mock.mjs'
import {
  afterReset,
  afterSeed,
  waitBeforeReset,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_ROLE,
  OPENAI_MOCK_ISOLATION_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_SUGGESTION,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
} from './worktree-reset-isolation-barrier.mjs'
import { runPairedWorktreeResetIsolation } from './worktree-reset-isolation-harness.mjs'
import { withWorktreeResetIsolationBarrierDir } from './worktree-reset-isolation-test-helpers.mjs'

test('openai-mock mode uses the OpenAI completion spec and proof env', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWorktreeResetIsolation({
      peerRoot: '/peer',
      resetterRoot: '/resetter',
      barrierDir: dir,
      mode: 'openai-mock',
      log: () => undefined,
      spawnCypress: (cwd, env) => {
        spawned.push({ cwd, env })
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
      'PEER_OPENAI_REQ_MARKER'
    )
    assert.equal(
      spawned[1].env[OPENAI_MOCK_ISOLATION_REQUEST_MARKER],
      'RESETTER_OPENAI_REQ_MARKER'
    )
    assert.notEqual(
      spawned[0].env[OPENAI_MOCK_ISOLATION_SUGGESTION],
      spawned[1].env[OPENAI_MOCK_ISOLATION_SUGGESTION]
    )
  })
})
