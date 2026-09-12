import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import {
  SUPPORTED_ISOLATED_CLI_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
} from './isolated-cypress.mjs'
import {
  afterReset,
  afterSeed,
  readBarrierEvents,
  waitBeforeReset,
  WORKTREE_RESET_ISOLATION_BARRIER_DIR,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_RESETTER_ROLE,
  WORKTREE_RESET_ISOLATION_ROLE,
  worktreeResetIsolationEnv,
} from './worktree-reset-isolation-barrier.mjs'
import {
  runPairedWorktreeResetIsolation,
  spawnIsolatedE2eRunner,
} from './worktree-reset-isolation-harness.mjs'
import { withWorktreeResetIsolationBarrierDir } from './worktree-reset-isolation-test-helpers.mjs'

test('barrier steps are no-ops without a paired-run role', async () => {
  const order = ['start']
  await waitBeforeReset({ env: {} })
  afterReset({ env: {} })
  await afterSeed({ env: {} })
  order.push('done')
  assert.deepEqual(order, ['start', 'done'])
})

test('resetter waits until the peer seeds, then the peer continues', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const peerEnv = worktreeResetIsolationEnv(
      dir,
      WORKTREE_RESET_ISOLATION_PEER_ROLE
    )
    const resetterEnv = worktreeResetIsolationEnv(
      dir,
      WORKTREE_RESET_ISOLATION_RESETTER_ROLE
    )
    const order = []
    const resetter = (async () => {
      await waitBeforeReset({
        env: resetterEnv,
        timeoutMs: 2_000,
        pollMs: 10,
      })
      order.push('resetter-reset')
      afterReset({ env: resetterEnv })
    })()
    const peer = (async () => {
      order.push('peer-seed')
      await afterSeed({ env: peerEnv, timeoutMs: 2_000, pollMs: 10 })
      order.push('peer-after-reset')
    })()
    await Promise.all([resetter, peer])
    assert.deepEqual(order, ['peer-seed', 'resetter-reset', 'peer-after-reset'])
    const events = readBarrierEvents(dir)
    assert.ok(Number(events.resetterResetAt) >= Number(events.peerSeededAt))
  })
})

test('resetter times out loudly when the peer never seeds', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    await assert.rejects(
      () =>
        waitBeforeReset({
          env: worktreeResetIsolationEnv(
            dir,
            WORKTREE_RESET_ISOLATION_RESETTER_ROLE
          ),
          timeoutMs: 30,
          pollMs: 5,
        }),
      /Timed out waiting for worktree reset isolation barrier file/
    )
  })
})

test('unknown isolation role fails before waiting', async () => {
  await assert.rejects(
    () =>
      waitBeforeReset({
        env: {
          [WORKTREE_RESET_ISOLATION_BARRIER_DIR]: '/tmp',
          [WORKTREE_RESET_ISOLATION_ROLE]: 'scheduler',
        },
      }),
    /Unknown WORKTREE_RESET_ISOLATION_ROLE=scheduler/
  )
})

test('role without a barrier directory fails loudly', async () => {
  await assert.rejects(
    () =>
      waitBeforeReset({
        env: {
          [WORKTREE_RESET_ISOLATION_ROLE]:
            WORKTREE_RESET_ISOLATION_RESETTER_ROLE,
        },
      }),
    /WORKTREE_RESET_ISOLATION_BARRIER_DIR is required/
  )
})

test('paired harness starts both runners against one barrier', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWorktreeResetIsolation({
      peerRoot: '/peer',
      resetterRoot: '/resetter',
      barrierDir: dir,
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
    assert.equal(result.peerExit.code, 0)
    assert.equal(result.resetterExit.code, 0)
    assert.equal(spawned.length, 2)
    assert.equal(spawned[0].cwd, '/peer')
    assert.equal(spawned[1].cwd, '/resetter')
    assert.equal(
      spawned[0].env[WORKTREE_RESET_ISOLATION_ROLE],
      WORKTREE_RESET_ISOLATION_PEER_ROLE
    )
    assert.equal(
      spawned[1].env[WORKTREE_RESET_ISOLATION_ROLE],
      WORKTREE_RESET_ISOLATION_RESETTER_ROLE
    )
    assert.equal(spawned[0].env[WORKTREE_RESET_ISOLATION_BARRIER_DIR], dir)
    assert.equal(spawned[1].env[WORKTREE_RESET_ISOLATION_BARRIER_DIR], dir)
    assert.ok(
      Number(result.events.resetterResetAt) >=
        Number(result.events.peerSeededAt)
    )
  })
})

test('paired harness fails when a Cypress runner exits non-zero', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    await assert.rejects(
      () =>
        runPairedWorktreeResetIsolation({
          peerRoot: '/peer',
          resetterRoot: '/resetter',
          barrierDir: dir,
          log: () => undefined,
          spawnCypress: (cwd) => {
            const child = new EventEmitter()
            queueMicrotask(() => {
              child.emit('close', cwd === '/peer' ? 1 : 0)
            })
            return child
          },
        }),
      /peer exit 1, resetter exit 0/
    )
  })
})

test('ordinary isolated Cypress spawn uses the allowlisted spec', () => {
  const calls = []
  spawnIsolatedE2eRunner('/checkout', { MARK: '1' }, (cmd, args, opts) => {
    calls.push({ cmd, args, opts })
    return new EventEmitter()
  })
  assert.equal(calls[0].cmd, 'node')
  assert.deepEqual(calls[0].args, [
    'scripts/e2e-runner.mjs',
    '--spec',
    SUPPORTED_ISOLATED_CYPRESS_SPEC,
  ])
  assert.equal(calls[0].opts.cwd, '/checkout')
  assert.equal(calls[0].opts.env.MARK, '1')
})

test('default spawnCypress delegates service ownership to the owned invocation wrapper', () => {
  const calls = []
  spawnIsolatedE2eRunner('/checkout', { MARK: '1' }, (cmd, args) => {
    calls.push({ cmd, args })
    return new EventEmitter()
  })
  assert.equal(calls[0].cmd, 'node')
  assert.deepEqual(calls[0].args, [
    'scripts/e2e-runner.mjs',
    '--spec',
    SUPPORTED_ISOLATED_CYPRESS_SPEC,
  ])
})

test('paired harness spawns exactly two owned invocations with role barrier env', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWorktreeResetIsolation({
      peerRoot: '/peer',
      resetterRoot: '/resetter',
      barrierDir: dir,
      log: () => undefined,
      spawnCypress: (cwd, env, spec) => {
        spawned.push({ cwd, env, spec })
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
    assert.equal(spawned.length, 2)
    for (const entry of spawned) {
      assert.equal(entry.spec, SUPPORTED_ISOLATED_CYPRESS_SPEC)
      assert.equal(entry.env[WORKTREE_RESET_ISOLATION_BARRIER_DIR], dir)
    }
    assert.equal(spawned[0].cwd, '/peer')
    assert.equal(spawned[1].cwd, '/resetter')
    assert.equal(
      spawned[0].env[WORKTREE_RESET_ISOLATION_ROLE],
      WORKTREE_RESET_ISOLATION_PEER_ROLE
    )
    assert.equal(
      spawned[1].env[WORKTREE_RESET_ISOLATION_ROLE],
      WORKTREE_RESET_ISOLATION_RESETTER_ROLE
    )
    assert.equal(result.peerExit.code, 0)
    assert.equal(result.resetterExit.code, 0)
  })
})

test('cli mode spawns the CLI spec on the peer and note-editing on the resetter', async () => {
  await withWorktreeResetIsolationBarrierDir(async (dir) => {
    const spawned = []
    const result = await runPairedWorktreeResetIsolation({
      peerRoot: '/peer',
      resetterRoot: '/resetter',
      barrierDir: dir,
      mode: 'cli',
      log: () => undefined,
      spawnCypress: (cwd, env, spec) => {
        spawned.push({ cwd, env, spec })
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
    assert.equal(result.mode, 'cli')
    assert.equal(result.peerSpec, SUPPORTED_ISOLATED_CLI_SPEC)
    assert.equal(result.resetterSpec, SUPPORTED_ISOLATED_CYPRESS_SPEC)
    assert.equal(spawned[0].spec, SUPPORTED_ISOLATED_CLI_SPEC)
    assert.equal(spawned[1].spec, SUPPORTED_ISOLATED_CYPRESS_SPEC)
  })
})
