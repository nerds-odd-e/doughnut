#!/usr/bin/env node
import { spawnSync } from 'node:child_process'
import { existsSync, realpathSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  isLinkedGitWorktree,
  readPresentWorktreeLocalConfig,
} from './browser-worktree-isolation.mjs'
import { e2eDatabaseNameForIdentity } from './sut-e2e-database.mjs'
import {
  assertValidWorktreeId,
  worktreeLocalConfigPath,
} from './worktree-identity.mjs'
import {
  collectRecordedRetirementVetoes,
  formatRecordedEvidenceRefusal,
} from './worktree-retirement-evidence.mjs'

export function unitDatabaseNameForIdentity(worktreeId) {
  assertValidWorktreeId(worktreeId)
  return `doughnut_${worktreeId}_test`
}

function listRegisteredWorktreeRoots(checkoutRoot) {
  const result = spawnSync(
    'git',
    ['-C', checkoutRoot, 'worktree', 'list', '--porcelain'],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) {
    const detail = (result.stderr || result.stdout || '').trim()
    throw new Error(
      `Unable to list registered Git worktrees for ${checkoutRoot}${
        detail ? `: ${detail}` : '.'
      }`
    )
  }
  const roots = []
  for (const line of result.stdout.split('\n')) {
    if (line.startsWith('worktree ')) {
      roots.push(line.slice('worktree '.length))
    }
  }
  return roots
}

function resolveCheckoutPath(checkoutRoot) {
  try {
    return realpathSync(checkoutRoot)
  } catch {
    return path.resolve(checkoutRoot)
  }
}

function findDuplicateIdentityCheckouts(checkoutRoot, worktreeId) {
  const self = resolveCheckoutPath(checkoutRoot)
  const duplicates = []
  for (const root of listRegisteredWorktreeRoots(checkoutRoot)) {
    if (!existsSync(root)) continue
    const resolved = resolveCheckoutPath(root)
    if (resolved === self) continue
    const peer = readPresentWorktreeLocalConfig(resolved)
    if (peer && typeof peer.id === 'string' && peer.id === worktreeId) {
      duplicates.push(resolved)
    }
  }
  return duplicates
}

function recordedE2eDatabase(config) {
  if (
    !config.e2e ||
    typeof config.e2e !== 'object' ||
    Array.isArray(config.e2e)
  ) {
    return
  }
  if (!Object.hasOwn(config.e2e, 'database')) return
  return config.e2e.database
}

/**
 * Resolve exact disposable database targets for this checkout.
 * Inspection only: not authorization or verified idleness.
 */
export function inspectDisposableDatabaseTargets(checkoutRoot) {
  if (!isLinkedGitWorktree(checkoutRoot)) {
    throw new Error(
      `Refusing worktree database retirement inspection: ${checkoutRoot} is not a Git linked worktree.`
    )
  }

  const config = readPresentWorktreeLocalConfig(checkoutRoot)
  if (!config) {
    throw new Error(
      `Refusing worktree database retirement inspection: missing identity file ${worktreeLocalConfigPath(
        checkoutRoot
      )}.`
    )
  }

  try {
    assertValidWorktreeId(config.id)
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error)
    throw new Error(
      `Refusing worktree database retirement inspection: invalid identity in ${worktreeLocalConfigPath(
        checkoutRoot
      )}: ${detail}`
    )
  }

  const duplicates = findDuplicateIdentityCheckouts(checkoutRoot, config.id)
  if (duplicates.length > 0) {
    throw new Error(
      `Refusing worktree database retirement inspection: identity ${
        config.id
      } is also recorded in another registered checkout (${duplicates.join(
        ', '
      )}).`
    )
  }

  const unitDatabase = unitDatabaseNameForIdentity(config.id)
  const expectedE2e = e2eDatabaseNameForIdentity(config.id)
  const e2eDatabase = recordedE2eDatabase(config)
  if (e2eDatabase !== undefined && e2eDatabase !== expectedE2e) {
    throw new Error(
      `Refusing worktree database retirement inspection: recorded E2E database ${JSON.stringify(
        e2eDatabase
      )} is not the canonical ${expectedE2e} for identity ${config.id}.`
    )
  }

  return {
    id: config.id,
    unitDatabase,
    e2eDatabase: e2eDatabase === expectedE2e ? e2eDatabase : undefined,
  }
}

function formatInspection(targets) {
  const lines = [
    'Disposable database targets (inspection only; not authorization or idleness verification).',
    `Worktree id: ${targets.id}`,
    `Unit database: ${targets.unitDatabase}`,
  ]
  if (targets.e2eDatabase) {
    lines.push(`E2E database: ${targets.e2eDatabase}`)
  }
  lines.push(
    'Recorded ownership, listeners, and database sessions: no busy evidence found.',
    'Idleness verification incomplete until orphan process inspection.'
  )
  return `${lines.join('\n')}\n`
}

function parseArgs(argv) {
  if (argv.length === 0) {
    return { mode: 'retire' }
  }
  if (argv.length === 1 && argv[0] === '--check') {
    return { mode: 'check' }
  }
  throw new Error(`Unknown worktree retirement arguments: ${argv.join(' ')}`)
}

export function defaultCheckoutRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

export async function runWorktreeRetire({
  argv = process.argv.slice(2),
  checkoutRoot = defaultCheckoutRoot(),
  out = process.stdout,
  err = process.stderr,
  evidenceDeps = {},
} = {}) {
  try {
    const { mode } = parseArgs(argv)
    if (mode !== 'check') {
      throw new Error(
        'Refusing database retirement: mutation mode is not available yet. Use --check to inspect disposable targets for this checkout.'
      )
    }
    const targets = inspectDisposableDatabaseTargets(checkoutRoot)
    const vetoes = await collectRecordedRetirementVetoes(
      checkoutRoot,
      targets,
      evidenceDeps
    )
    if (vetoes.length > 0) {
      throw new Error(formatRecordedEvidenceRefusal(vetoes))
    }
    out.write(formatInspection(targets))
    return 0
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    err.write(`${message}\n`)
    err.write('Usage: pnpm worktree:retire --check\n')
    return 1
  }
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  runWorktreeRetire().then((code) => process.exit(code))
}
