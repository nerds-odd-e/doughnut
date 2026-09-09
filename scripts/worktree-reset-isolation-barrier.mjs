/**
 * Temporary paired-run barrier for isolated Cypress reset isolation.
 * Not a reusable scheduler: one file handshake for this proof only.
 *
 * Barrier placement (`WORKTREE_RESET_ISOLATION_BARRIER_AT`):
 * - `fixture` (default): wait/signal around DB fixture reset
 * - `openai-mock`: wait/signal around private OpenAI mock install/reset
 */
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import {
  OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_SUGGESTION,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_FIXTURE,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
  WORKTREE_RESET_ISOLATION_BARRIER_DIR,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_RESETTER_ROLE,
  WORKTREE_RESET_ISOLATION_ROLE,
  WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
} from './worktree-isolation-constants.mjs'

export {
  OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_REQUEST_MARKER,
  OPENAI_MOCK_ISOLATION_SUGGESTION,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_FIXTURE,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
  WORKTREE_RESET_ISOLATION_BARRIER_DIR,
  WORKTREE_RESET_ISOLATION_PEER_ROLE,
  WORKTREE_RESET_ISOLATION_RESETTER_ROLE,
  WORKTREE_RESET_ISOLATION_ROLE,
  WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
}

const PEER_SEEDED_FILE = 'peer-seeded'
const RESETTER_RESET_FILE = 'resetter-reset'

function isolationRole(env) {
  const role = env[WORKTREE_RESET_ISOLATION_ROLE]
  if (!role) return null
  if (
    role !== WORKTREE_RESET_ISOLATION_PEER_ROLE &&
    role !== WORKTREE_RESET_ISOLATION_RESETTER_ROLE
  ) {
    throw new Error(
      `Unknown ${WORKTREE_RESET_ISOLATION_ROLE}=${role}. ` +
        `Use ${WORKTREE_RESET_ISOLATION_PEER_ROLE} or ` +
        `${WORKTREE_RESET_ISOLATION_RESETTER_ROLE}.`
    )
  }
  const dir = env[WORKTREE_RESET_ISOLATION_BARRIER_DIR]
  if (!dir) {
    throw new Error(
      `${WORKTREE_RESET_ISOLATION_BARRIER_DIR} is required when ` +
        `${WORKTREE_RESET_ISOLATION_ROLE} is set.`
    )
  }
  return { role, dir }
}

export function isolationBarrierAt(env = process.env) {
  const at =
    env[WORKTREE_RESET_ISOLATION_BARRIER_AT] ??
    WORKTREE_RESET_ISOLATION_BARRIER_AT_FIXTURE
  if (
    at !== WORKTREE_RESET_ISOLATION_BARRIER_AT_FIXTURE &&
    at !== WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK
  ) {
    throw new Error(
      `Unknown ${WORKTREE_RESET_ISOLATION_BARRIER_AT}=${at}. ` +
        `Use ${WORKTREE_RESET_ISOLATION_BARRIER_AT_FIXTURE} or ` +
        `${WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK}.`
    )
  }
  return at
}

export function openAiMockIsolationProofParams(env = process.env) {
  const suggestion = env[OPENAI_MOCK_ISOLATION_SUGGESTION]
  const requestMarker = env[OPENAI_MOCK_ISOLATION_REQUEST_MARKER]
  const foreignRequestMarker = env[OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER]
  if (!(suggestion || requestMarker || foreignRequestMarker)) return null
  if (!(suggestion && requestMarker)) {
    throw new Error(
      `${OPENAI_MOCK_ISOLATION_SUGGESTION} and ` +
        `${OPENAI_MOCK_ISOLATION_REQUEST_MARKER} must be set together.`
    )
  }
  return foreignRequestMarker
    ? { suggestion, requestMarker, foreignRequestMarker }
    : { suggestion, requestMarker }
}

export function appendCucumberExposeTag(config, extraTag) {
  const existingTags =
    typeof config.expose.tags === 'string' && config.expose.tags.length > 0
      ? config.expose.tags
      : 'not @ignore'
  config.expose.tags = `(${existingTags}) and ${extraTag}`
}

function barrierPath(dir, name) {
  return path.join(dir, name)
}

function signalBarrier(filePath) {
  writeFileSync(filePath, `${Date.now()}\n`)
}

async function waitForBarrierFile(filePath, options = {}) {
  const timeoutMs =
    options.timeoutMs ?? WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS
  const pollMs = options.pollMs ?? 50
  const exists = options.existsFn ?? existsSync
  const sleep =
    options.sleepFn ??
    ((ms) => new Promise((resolve) => setTimeout(resolve, ms)))
  const now = options.nowFn ?? Date.now
  const deadline = now() + timeoutMs
  while (!exists(filePath)) {
    if (now() >= deadline) {
      throw new Error(
        `Timed out waiting for worktree reset isolation barrier file ${filePath}`
      )
    }
    await sleep(pollMs)
  }
}

export async function waitBeforeReset(options = {}) {
  const env = options.env ?? process.env
  const resolved = isolationRole(env)
  if (!resolved || resolved.role !== WORKTREE_RESET_ISOLATION_RESETTER_ROLE) {
    return
  }
  await waitForBarrierFile(barrierPath(resolved.dir, PEER_SEEDED_FILE), options)
}

export function afterReset(options = {}) {
  const env = options.env ?? process.env
  const resolved = isolationRole(env)
  if (!resolved || resolved.role !== WORKTREE_RESET_ISOLATION_RESETTER_ROLE) {
    return
  }
  signalBarrier(barrierPath(resolved.dir, RESETTER_RESET_FILE))
}

export async function afterSeed(options = {}) {
  const env = options.env ?? process.env
  const resolved = isolationRole(env)
  if (!resolved || resolved.role !== WORKTREE_RESET_ISOLATION_PEER_ROLE) {
    return
  }
  signalBarrier(barrierPath(resolved.dir, PEER_SEEDED_FILE))
  await waitForBarrierFile(
    barrierPath(resolved.dir, RESETTER_RESET_FILE),
    options
  )
}

export function worktreeResetIsolationCypressTasks(env = process.env) {
  return {
    worktreeResetIsolationWaitBeforeReset() {
      return waitBeforeReset({ env }).then(() => null)
    },
    worktreeResetIsolationAfterReset() {
      afterReset({ env })
      return null
    },
    worktreeResetIsolationAfterSeed() {
      return afterSeed({ env }).then(() => null)
    },
    openAiMockIsolationProofParams() {
      return openAiMockIsolationProofParams(env)
    },
  }
}

export function readBarrierEvents(dir) {
  return {
    peerSeededAt: readFileSync(
      barrierPath(dir, PEER_SEEDED_FILE),
      'utf8'
    ).trim(),
    resetterResetAt: readFileSync(
      barrierPath(dir, RESETTER_RESET_FILE),
      'utf8'
    ).trim(),
  }
}

export function worktreeResetIsolationEnv(dir, role, options = {}) {
  const env = {
    [WORKTREE_RESET_ISOLATION_BARRIER_DIR]: dir,
    [WORKTREE_RESET_ISOLATION_ROLE]: role,
  }
  if (options.barrierAt) {
    env[WORKTREE_RESET_ISOLATION_BARRIER_AT] = options.barrierAt
  }
  if (options.suggestion) {
    env[OPENAI_MOCK_ISOLATION_SUGGESTION] = options.suggestion
  }
  if (options.requestMarker) {
    env[OPENAI_MOCK_ISOLATION_REQUEST_MARKER] = options.requestMarker
  }
  if (options.foreignRequestMarker) {
    env[OPENAI_MOCK_ISOLATION_FOREIGN_REQUEST_MARKER] =
      options.foreignRequestMarker
  }
  return env
}
