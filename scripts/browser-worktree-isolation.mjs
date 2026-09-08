import { spawnSync } from 'node:child_process'
import { readFileSync, statSync } from 'node:fs'
import path from 'node:path'

const WORKTREE_LOCAL_CONFIG_NAME = '.worktree.local.json'

function worktreeLocalConfigPath(checkoutRoot) {
  return path.join(checkoutRoot, WORKTREE_LOCAL_CONFIG_NAME)
}

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
function worktreeIsolationApplies(checkoutRoot) {
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

export function refuseUnsupportedIsolatedBrowserCommand({
  checkoutRoot,
  command,
}) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  if (isRegularFile(configPath)) {
    readWorktreeLocalConfig(checkoutRoot)
  }
  if (!worktreeIsolationApplies(checkoutRoot)) {
    return
  }
  throw new Error(
    'Isolated worktree browser commands are not supported yet. ' +
      `Refusing ${command} so it does not use shared local defaults ` +
      '(ports 5173/5174/9081, mountebank 2525, or the shared E2E database).'
  )
}

export function guardCypressNodeSetup(checkoutRoot) {
  refuseUnsupportedIsolatedBrowserCommand({
    checkoutRoot,
    command: 'pnpm cypress run --spec',
  })
}
