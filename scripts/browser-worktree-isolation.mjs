import { spawnSync } from 'node:child_process'
import { readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import {
  E2E_PORT_CONFIG_FIELDS,
  collectMissingE2ePorts,
  collectPresentInvalidE2ePortFields,
  refusePartialIsolatedE2ePorts,
} from './sut-e2e-ports.mjs'
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
export function isLinkedGitWorktree(checkoutRoot) {
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

const SUPPORTED_ISOLATED_SUT_COMMANDS = new Set(['pnpm sut:healthcheck'])

const E2E_ALLOCATION_FIELDS = ['e2e.database', ...E2E_PORT_CONFIG_FIELDS]

export function isRecordedE2eDatabase(database) {
  return typeof database === 'string' && /^[A-Za-z0-9_]+$/.test(database)
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function allocationError(checkoutRoot, missing, { start } = {}) {
  if (start) {
    return new Error(
      `Isolated worktree SUT needs identity in ${worktreeLocalConfigPath(
        checkoutRoot
      )} (id). Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.`
    )
  }
  return new Error(
    `Isolated worktree SUT needs a complete E2E allocation in ${worktreeLocalConfigPath(
      checkoutRoot
    )} (id, ${E2E_ALLOCATION_FIELDS.join(', ')}). ` +
      `Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.`
  )
}

function collectPresentInvalidE2eFields(config) {
  if (!Object.hasOwn(config, 'e2e')) return []
  if (!isPlainObject(config.e2e)) return ['e2e']
  const missing = []
  if (
    Object.hasOwn(config.e2e, 'database') &&
    !isRecordedE2eDatabase(config.e2e.database)
  ) {
    missing.push('e2e.database')
  }
  missing.push(...collectPresentInvalidE2ePortFields(config.e2e))
  return missing
}

export function refusePresentInvalidIsolatedE2eAllocation(
  checkoutRoot,
  config,
  errorOpts
) {
  const missing = collectPresentInvalidE2eFields(config)
  if (missing.length > 0) {
    throw allocationError(checkoutRoot, missing, errorOpts)
  }
}

function readRequiredWorktreeConfig(
  checkoutRoot,
  missingWhenAbsent,
  errorOpts
) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  if (!isRegularFile(configPath)) {
    throw allocationError(checkoutRoot, missingWhenAbsent, errorOpts)
  }
  const config = readWorktreeLocalConfig(checkoutRoot)
  assertValidWorktreeId(config.id)
  return config
}

export function loadIsolatedE2eStartAllocation(checkoutRoot) {
  const config = readRequiredWorktreeConfig(
    checkoutRoot,
    ['identity (.worktree.local.json)'],
    { start: true }
  )
  refusePresentInvalidIsolatedE2eAllocation(checkoutRoot, config, {
    start: true,
  })
  refusePartialIsolatedE2ePorts(checkoutRoot, config.e2e)
  return config
}

export function loadCompleteIsolatedE2eAllocation(checkoutRoot) {
  const config = readRequiredWorktreeConfig(checkoutRoot, [
    'identity (.worktree.local.json)',
    ...E2E_ALLOCATION_FIELDS,
  ])
  refusePresentInvalidIsolatedE2eAllocation(checkoutRoot, config)
  const missing = []
  if (!isRecordedE2eDatabase(config.e2e?.database)) {
    missing.push('e2e.database')
  }
  missing.push(...collectMissingE2ePorts(config.e2e))
  if (missing.length > 0) {
    throw allocationError(checkoutRoot, missing)
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
  if (command === 'pnpm sut') {
    loadIsolatedE2eStartAllocation(checkoutRoot)
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
