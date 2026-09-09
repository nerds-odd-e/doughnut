/**
 * Per-role MCP note markers and a two-file seeded handshake for concurrent
 * isolated Cypress proof. Not a scheduler or lease.
 */
import { existsSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { appendCucumberExposeTag } from './worktree-reset-isolation-barrier.mjs'
import {
  MCP_ISOLATION_BARRIER_DIR,
  MCP_ISOLATION_FOREIGN_MARKER,
  MCP_ISOLATION_NOTE_MARKER,
  WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
} from './worktree-isolation-constants.mjs'

export function mcpIsolationProofParams(env = process.env) {
  const own = env[MCP_ISOLATION_NOTE_MARKER]
  const foreign = env[MCP_ISOLATION_FOREIGN_MARKER]
  const barrierDir = env[MCP_ISOLATION_BARRIER_DIR]
  if (!(own || foreign || barrierDir)) return null
  if (!(own && foreign && barrierDir)) {
    throw new Error(
      `${MCP_ISOLATION_NOTE_MARKER}, ${MCP_ISOLATION_FOREIGN_MARKER}, and ` +
        `${MCP_ISOLATION_BARRIER_DIR} must be set together.`
    )
  }
  if (own === foreign) {
    throw new Error(
      `${MCP_ISOLATION_NOTE_MARKER} and ${MCP_ISOLATION_FOREIGN_MARKER} must differ.`
    )
  }
  return { own, foreign, barrierDir }
}

export function applyMcpIsolationCypressExpose(config) {
  if (mcpIsolationProofParams()) return
  appendCucumberExposeTag(config, 'not @mcpIsolationProof')
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
        `Timed out waiting for MCP isolation barrier file ${filePath}`
      )
    }
    await sleep(pollMs)
  }
}

export async function mcpIsolationRendezvousAfterSeed(options = {}) {
  const env = options.env ?? process.env
  const params = mcpIsolationProofParams(env)
  if (!params) return
  const ownFile = path.join(params.barrierDir, `${params.own}.seeded`)
  const foreignFile = path.join(params.barrierDir, `${params.foreign}.seeded`)
  writeFileSync(ownFile, `${Date.now()}\n`)
  await waitForBarrierFile(foreignFile, options)
}

export function mcpIsolationCypressTasks(env = process.env) {
  return {
    mcpIsolationProofParams() {
      return mcpIsolationProofParams(env)
    },
    mcpIsolationRendezvousAfterSeed() {
      return mcpIsolationRendezvousAfterSeed({ env }).then(() => null)
    },
  }
}
