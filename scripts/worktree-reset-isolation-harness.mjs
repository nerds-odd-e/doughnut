#!/usr/bin/env node
/**
 * Temporary paired Cypress runner for isolated reset isolation.
 * Uses the harness-local file barrier; not a reusable scheduler.
 */
import { spawn } from 'node:child_process'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseArgs } from 'node:util'
import { SUPPORTED_ISOLATED_CYPRESS_SPEC } from './isolated-cypress.mjs'
import {
  readBarrierEvents,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_RESETTER_ROLE,
  worktreeResetIsolationEnv,
} from './worktree-reset-isolation-barrier.mjs'

function waitForChildExit(child) {
  return new Promise((resolve, reject) => {
    child.once('error', reject)
    child.once('close', (code, signal) => {
      resolve({ code, signal })
    })
  })
}

export function spawnIsolatedCypress(cwd, extraEnv, spawnFn = spawn) {
  return spawnFn(
    'pnpm',
    ['cypress', 'run', '--spec', SUPPORTED_ISOLATED_CYPRESS_SPEC],
    {
      cwd,
      env: { ...process.env, ...extraEnv },
      stdio: 'inherit',
    }
  )
}

export async function runPairedWorktreeResetIsolation(options) {
  const peerRoot = options.peerRoot
  const resetterRoot = options.resetterRoot
  if (!(peerRoot && resetterRoot)) {
    throw new Error('Paired reset isolation requires --peer and --resetter.')
  }
  const barrierDir =
    options.barrierDir ??
    (await mkdtemp(path.join(tmpdir(), 'worktree-reset-isolation-')))
  const spawnCypress = options.spawnCypress ?? spawnIsolatedCypress
  const log = options.log ?? ((line) => process.stdout.write(`${line}\n`))
  log(`Reset isolation barrier: ${barrierDir}`)
  log(`Peer checkout: ${peerRoot}`)
  log(`Resetter checkout: ${resetterRoot}`)
  const peer = spawnCypress(
    peerRoot,
    worktreeResetIsolationEnv(barrierDir, WORKTREE_RESET_ISOLATION_PEER_ROLE)
  )
  const resetter = spawnCypress(
    resetterRoot,
    worktreeResetIsolationEnv(
      barrierDir,
      WORKTREE_RESET_ISOLATION_RESETTER_ROLE
    )
  )
  const [peerExit, resetterExit] = await Promise.all([
    waitForChildExit(peer),
    waitForChildExit(resetter),
  ])
  if (peerExit.code !== 0 || resetterExit.code !== 0) {
    throw new Error(
      `Paired Cypress reset isolation failed ` +
        `(peer exit ${peerExit.code}, resetter exit ${resetterExit.code}).`
    )
  }
  const events = readBarrierEvents(barrierDir)
  log(`Peer seeded at ${events.peerSeededAt}`)
  log(`Resetter reset at ${events.resetterResetAt}`)
  if (Number(events.resetterResetAt) < Number(events.peerSeededAt)) {
    throw new Error(
      'Resetter reset happened before the peer seeded; the barrier did not hold.'
    )
  }
  return { barrierDir, peerExit, resetterExit, events }
}

function failLoudly(error) {
  process.stderr.write(
    `${error instanceof Error ? error.message : String(error)}\n`
  )
  process.exit(1)
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  const { values } = parseArgs({
    options: {
      peer: { type: 'string' },
      resetter: { type: 'string' },
    },
  })
  runPairedWorktreeResetIsolation({
    peerRoot: values.peer,
    resetterRoot: values.resetter,
  }).catch(failLoudly)
}
