import { spawnSync } from 'node:child_process'
import { readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import {
  assertValidWorktreeId,
  worktreeLocalConfigPath,
} from './worktree-identity.mjs'

function isRegularFile(filePath) {
  try {
    return statSync(filePath).isFile()
  } catch {
    return false
  }
}

function resolveGitPath(checkoutRoot, revParseArg) {
  const result = spawnSync(
    'git',
    ['-C', checkoutRoot, 'rev-parse', revParseArg],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) return null
  return path.resolve(checkoutRoot, result.stdout.trim())
}

// Linked worktrees have a distinct git-dir under the shared common dir.
function isLinkedGitWorktree(checkoutRoot) {
  const gitDir = resolveGitPath(checkoutRoot, '--git-dir')
  const commonDir = resolveGitPath(checkoutRoot, '--git-common-dir')
  if (!(gitDir && commonDir)) return false
  return gitDir !== commonDir
}

// Isolation applies when the identity file exists or this checkout is a linked git worktree.
export function worktreeIsolationApplies(checkoutRoot) {
  return (
    isRegularFile(worktreeLocalConfigPath(checkoutRoot)) ||
    isLinkedGitWorktree(checkoutRoot)
  )
}

function readWorktreeLocalConfig(checkoutRoot) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  const raw = readFileSync(configPath, 'utf8')
  try {
    return JSON.parse(raw)
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error)
    throw new Error(
      `Worktree configuration ${configPath} is not valid JSON: ${detail}`
    )
  }
}

export function readPresentWorktreeLocalConfig(checkoutRoot) {
  if (!isRegularFile(worktreeLocalConfigPath(checkoutRoot))) return null
  return readWorktreeLocalConfig(checkoutRoot)
}

const SUPPORTED_ISOLATED_SUT_COMMANDS = new Set([
  'pnpm sut',
  'pnpm sut:healthcheck',
  'pnpm sut:restart',
])

const E2E_ALLOCATION_FIELDS = [
  'e2e.database',
  'e2e.backendPort',
  'e2e.vitePort',
  'e2e.lbListenPort',
]

function missingAllocationError(checkoutRoot, missing) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  return new Error(
    `Isolated worktree SUT needs a complete E2E allocation in ${configPath} ` +
      '(id, e2e.database, e2e.backendPort, e2e.vitePort, e2e.lbListenPort). ' +
      `Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.`
  )
}

export function loadCompleteIsolatedE2eAllocation(checkoutRoot) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  if (!isRegularFile(configPath)) {
    throw missingAllocationError(checkoutRoot, [
      'identity (.worktree.local.json)',
      ...E2E_ALLOCATION_FIELDS,
    ])
  }
  const config = readWorktreeLocalConfig(checkoutRoot)
  assertValidWorktreeId(config.id)
  const e2e = config.e2e
  const missing = []
  if (!e2e || typeof e2e !== 'object') {
    missing.push(...E2E_ALLOCATION_FIELDS)
  } else {
    if (
      typeof e2e.database !== 'string' ||
      !/^[A-Za-z0-9_]+$/.test(e2e.database)
    ) {
      missing.push('e2e.database')
    }
    for (const field of ['backendPort', 'vitePort', 'lbListenPort']) {
      if (!Number.isInteger(e2e[field]) || e2e[field] <= 0) {
        missing.push(`e2e.${field}`)
      }
    }
  }
  if (missing.length > 0) {
    throw missingAllocationError(checkoutRoot, missing)
  }
  return config
}

export function refuseUnsupportedIsolatedBrowserCommand({
  checkoutRoot,
  command,
}) {
  readPresentWorktreeLocalConfig(checkoutRoot)
  if (!worktreeIsolationApplies(checkoutRoot)) {
    return
  }
  if (SUPPORTED_ISOLATED_SUT_COMMANDS.has(command)) {
    loadCompleteIsolatedE2eAllocation(checkoutRoot)
    return
  }
  throw new Error(
    'Isolated worktree browser commands are not supported yet. ' +
      `Refusing ${command} so it does not use shared local defaults ` +
      '(ports 5173/5174/9081, mountebank 2525, or the shared E2E database).'
  )
}
