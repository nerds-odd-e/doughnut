import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import path from 'node:path'

type CheckoutPurpose = 'publishing' | 'receiving'

/** Runs a read-only Git query against `directory`, throwing if it fails. */
function readGitOutput(directory: string, args: string[]): string {
  const result = spawnSync('git', ['-C', directory, ...args], {
    encoding: 'utf8',
  })
  if (result.error || result.status !== 0) {
    throw new Error(
      `${directory} is not a usable Git repository (git ${args.join(' ')} failed).`
    )
  }
  return result.stdout
}

function attachedMainDirtyFileCount(
  directory: string,
  purpose: CheckoutPurpose
): number {
  const branch = readGitOutput(directory, [
    'rev-parse',
    '--abbrev-ref',
    'HEAD',
  ]).trim()

  if (branch !== 'main') {
    const where = branch === 'HEAD' ? 'a detached HEAD' : `branch "${branch}"`
    throw new Error(
      `${directory} is not on branch main — currently on ${where}. Switch to main before ${purpose}.`
    )
  }

  return readGitOutput(directory, ['status', '--porcelain'])
    .split('\n')
    .filter((line) => line.trim() !== '').length
}

function gitOperationIsActive(directory: string): boolean {
  const operationMarkers = [
    'MERGE_HEAD',
    'rebase-merge',
    'rebase-apply',
    'sequencer',
    'CHERRY_PICK_HEAD',
    'REVERT_HEAD',
  ]

  return operationMarkers.some((marker) => {
    const gitPath = readGitOutput(directory, [
      'rev-parse',
      '--git-path',
      marker,
    ]).trim()
    return fs.existsSync(path.resolve(directory, gitPath))
  })
}

/** Refuses active operations first — porcelain may be empty or dirty, and HEAD may be detached. */
function readyCheckoutDirtyFileCount(
  directory: string,
  purpose: CheckoutPurpose
): number {
  if (gitOperationIsActive(directory)) {
    throw new Error(
      `${directory} has an active Git operation. Finish or abort the active Git operation before ${purpose}.`
    )
  }
  return attachedMainDirtyFileCount(directory, purpose)
}

function dirtyCheckoutDetail(directory: string, changedFiles: number): string {
  return `${directory} has uncommitted changes (${changedFiles} file${changedFiles === 1 ? '' : 's'} not clean, including untracked files)`
}

/**
 * Confirms the bound checkout is eligible to publish from. Read-only — never touches refs,
 * the index, or files.
 */
export function assertLocalMainIsReadyToPublish(directory: string): void {
  const dirtyFileCount = readyCheckoutDirtyFileCount(directory, 'publishing')
  if (dirtyFileCount > 0) {
    console.error(
      `donut: warning: ${dirtyCheckoutDetail(directory, dirtyFileCount)} — publishing committed main; local changes are not included.`
    )
  }
}

/**
 * Confirms the bound checkout is eligible to receive into. Read-only — never touches refs,
 * the index, or files.
 */
export function assertLocalMainIsReadyToReceive(directory: string): void {
  const dirtyFileCount = readyCheckoutDirtyFileCount(directory, 'receiving')
  if (dirtyFileCount > 0) {
    throw new Error(
      `${dirtyCheckoutDetail(directory, dirtyFileCount)} — commit or clean them before receiving.`
    )
  }
}
