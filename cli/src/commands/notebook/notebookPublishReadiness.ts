import { spawnSync } from 'node:child_process'

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

/**
 * Confirms the bound checkout at `directory` is eligible to publish from: HEAD is on branch
 * `main` (not detached, not another branch), and the index and worktree are completely clean
 * (no staged changes, no unstaged modifications, no untracked files). Read-only — never touches
 * refs or files. Throws an actionable error naming the unmet local prerequisite otherwise.
 */
export function assertLocalMainIsCleanAndCommitted(directory: string): void {
  const branch = readGitOutput(directory, [
    'rev-parse',
    '--abbrev-ref',
    'HEAD',
  ]).trim()

  if (branch !== 'main') {
    const where = branch === 'HEAD' ? 'a detached HEAD' : `branch "${branch}"`
    throw new Error(
      `${directory} is not on branch main — currently on ${where}. Switch to main before publishing.`
    )
  }

  const status = readGitOutput(directory, ['status', '--porcelain'])
  const changedLines = status.split('\n').filter((line) => line.trim() !== '')
  if (changedLines.length > 0) {
    throw new Error(
      `${directory} has uncommitted changes (${changedLines.length} file${changedLines.length === 1 ? '' : 's'} not clean, including untracked files) — commit or clean them before publishing.`
    )
  }
}
