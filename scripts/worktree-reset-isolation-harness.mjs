#!/usr/bin/env node
/**
 * Temporary paired owned-invocation runner for isolated reset isolation.
 * Each role spawns the owned E2E wrapper (scripts/e2e-runner.mjs), which owns
 * its own SUT stack for the invocation; the harness never starts another stack
 * around it. Uses the harness-local file barrier; not a reusable scheduler.
 *
 * Default (fixture): note-editing DB reset isolation (same spec both roles).
 * OpenAI mock mode: note-content completion with distinct suggestions /
 * request markers and barrier around private mock install/reset.
 * CLI mode: CLI peer spec + browser note-editing resetter.
 */
import { spawn } from 'node:child_process'
import { mkdtemp } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseArgs } from 'node:util'
import {
  SUPPORTED_ISOLATED_CLI_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPEC,
  SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC,
} from './isolated-cypress.mjs'
import {
  OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_SUGGESTION,
  readBarrierEvents,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
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

export function spawnIsolatedE2eRunner(
  cwd,
  extraEnv,
  spawnFn = spawn,
  spec = SUPPORTED_ISOLATED_CYPRESS_SPEC
) {
  return spawnFn('node', ['scripts/e2e-runner.mjs', '--spec', spec], {
    cwd,
    env: { ...process.env, ...extraEnv },
    stdio: 'inherit',
  })
}

const DEFAULT_PEER_PROOF = {
  suggestion: 'It is a peer isolation city.',
  requestMarker: 'PEER_OPENAI_REQ_MARKER',
}

const DEFAULT_RESETTER_PROOF = {
  suggestion: 'It is a resetter isolation city.',
  requestMarker: 'RESETTER_OPENAI_REQ_MARKER',
}

function openAiMockProofEnvOptions(proof, oppositeProof) {
  return {
    barrierAt: WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
    suggestion: proof.suggestion,
    requestMarker: proof.requestMarker,
    foreignRequestMarker: oppositeProof.requestMarker,
  }
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
  const mode = options.mode ?? 'fixture'
  const openaiMock = mode === 'openai-mock'
  const peerSpec =
    mode === 'cli'
      ? SUPPORTED_ISOLATED_CLI_SPEC
      : openaiMock
        ? SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC
        : SUPPORTED_ISOLATED_CYPRESS_SPEC
  const resetterSpec = openaiMock
    ? SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC
    : SUPPORTED_ISOLATED_CYPRESS_SPEC
  const peerProof = options.peerProof ?? DEFAULT_PEER_PROOF
  const resetterProof = options.resetterProof ?? DEFAULT_RESETTER_PROOF
  const spawnCypress =
    options.spawnCypress ??
    ((cwd, env, spec) => spawnIsolatedE2eRunner(cwd, env, spawn, spec))
  const log = options.log ?? ((line) => process.stdout.write(`${line}\n`))
  log(`Reset isolation barrier: ${barrierDir}`)
  log(`Mode: ${mode}`)
  if (peerSpec === resetterSpec) {
    log(`Spec: ${peerSpec}`)
  } else {
    log(`Peer spec: ${peerSpec}`)
    log(`Resetter spec: ${resetterSpec}`)
  }
  log(`Peer checkout: ${peerRoot}`)
  log(`Resetter checkout: ${resetterRoot}`)
  if (openaiMock) {
    log(
      `Peer suggestion/marker: ${peerProof.suggestion} / ${peerProof.requestMarker}`
    )
    log(
      `Resetter suggestion/marker: ${resetterProof.suggestion} / ${resetterProof.requestMarker}`
    )
  }
  const peerEnv = worktreeResetIsolationEnv(
    barrierDir,
    WORKTREE_RESET_ISOLATION_PEER_ROLE,
    openaiMock ? openAiMockProofEnvOptions(peerProof, resetterProof) : {}
  )
  const resetterEnv = worktreeResetIsolationEnv(
    barrierDir,
    WORKTREE_RESET_ISOLATION_RESETTER_ROLE,
    openaiMock ? openAiMockProofEnvOptions(resetterProof, peerProof) : {}
  )
  const peer = spawnCypress(peerRoot, peerEnv, peerSpec)
  const resetter = spawnCypress(resetterRoot, resetterEnv, resetterSpec)
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
  return {
    barrierDir,
    peerExit,
    resetterExit,
    events,
    mode,
    peerSpec,
    resetterSpec,
    peerProof: openaiMock ? peerProof : null,
    resetterProof: openaiMock ? resetterProof : null,
    peerEnv,
    resetterEnv,
  }
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
      mode: { type: 'string', default: 'fixture' },
      'peer-suggestion': { type: 'string' },
      'peer-marker': { type: 'string' },
      'resetter-suggestion': { type: 'string' },
      'resetter-marker': { type: 'string' },
    },
  })
  const peerProof = {
    suggestion: values['peer-suggestion'] ?? DEFAULT_PEER_PROOF.suggestion,
    requestMarker: values['peer-marker'] ?? DEFAULT_PEER_PROOF.requestMarker,
  }
  const resetterProof = {
    suggestion:
      values['resetter-suggestion'] ?? DEFAULT_RESETTER_PROOF.suggestion,
    requestMarker:
      values['resetter-marker'] ?? DEFAULT_RESETTER_PROOF.requestMarker,
  }
  runPairedWorktreeResetIsolation({
    peerRoot: values.peer,
    resetterRoot: values.resetter,
    mode: values.mode,
    peerProof,
    resetterProof,
  })
    .then((result) => {
      if (result.mode === 'openai-mock') {
        process.stdout.write(
          `OpenAI mock isolation OK. Peer env marker=${result.peerEnv[OPENAI_MOCK_ISOLATION_REQUEST_MARKER]} ` +
            `foreign marker=${result.peerEnv[OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER]} ` +
            `suggestion=${result.peerEnv[OPENAI_MOCK_ISOLATION_SUGGESTION]}\n`
        )
        process.stdout.write(
          `Resetter env marker=${result.resetterEnv[OPENAI_MOCK_ISOLATION_REQUEST_MARKER]} ` +
            `foreign marker=${result.resetterEnv[OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER]} ` +
            `suggestion=${result.resetterEnv[OPENAI_MOCK_ISOLATION_SUGGESTION]}\n`
        )
      }
    })
    .catch(failLoudly)
}
