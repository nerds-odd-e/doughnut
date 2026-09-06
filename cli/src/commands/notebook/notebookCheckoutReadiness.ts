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

function assertAttachedCleanMain(
  directory: string,
  purpose: CheckoutPurpose
): void {
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

  const status = readGitOutput(directory, ['status', '--porcelain'])
  const changedLines = status.split('\n').filter((line) => line.trim() !== '')
  if (changedLines.length > 0) {
    throw new Error(
      `${directory} has uncommitted changes (${changedLines.length} file${changedLines.length === 1 ? '' : 's'} not clean, including untracked files) — commit or clean them before ${purpose}.`
    )
  }
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

/**
 * Confirms the bound checkout is eligible to publish from. Read-only — never touches refs,
 * the index, or files.
 */
export function assertLocalMainIsReadyToPublish(directory: string): void {
  assertAttachedCleanMain(directory, 'publishing')
}

/**
 * Confirms the bound checkout is eligible to receive into. In addition to the shared clean-main
 * policy, receive refuses active Git operations because their porcelain status may be empty.
 * Read-only — never touches refs, the index, or files.
 */
export function assertLocalMainIsReadyToReceive(directory: string): void {
  if (gitOperationIsActive(directory)) {
    throw new Error(
      `${directory} has an active Git operation. Finish or abort the active Git operation before receiving.`
    )
  }
  assertAttachedCleanMain(directory, 'receiving')
}
