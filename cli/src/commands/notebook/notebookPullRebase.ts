import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { runSystemGitOrThrow } from './systemGit.js'

function unpublishedCommitAuthor(directory: string): {
  name: string
  email: string
} {
  const text = runSystemGitOrThrow(
    ['-C', directory, 'log', '-1', '--format=%an%n%ae'],
    (detail, status) =>
      `failed to read the unpublished local commit identity${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trimEnd()
  const [name = '', email = ''] = text.split('\n')
  return { name, email }
}

function gitDirPathExists(directory: string, marker: string): boolean {
  const result = spawnSync(
    'git',
    ['-C', directory, 'rev-parse', '--git-path', marker],
    { encoding: 'utf8' }
  )
  if (result.error || result.status !== 0) return false
  return fs.existsSync(path.resolve(directory, result.stdout.trim()))
}

function uniqueUnmergedPaths(directory: string): string[] {
  const result = spawnSync('git', ['-C', directory, 'ls-files', '-u', '-z'], {
    encoding: 'utf8',
  })
  if (result.error || result.status !== 0) return []
  const paths = new Set<string>()
  for (const record of result.stdout.split('\0')) {
    const tab = record.indexOf('\t')
    if (tab === -1) continue
    paths.add(record.slice(tab + 1))
  }
  return [...paths]
}

function pausedRebaseConflictPaths(directory: string): string[] | undefined {
  if (
    !(
      gitDirPathExists(directory, 'rebase-merge') ||
      gitDirPathExists(directory, 'rebase-apply')
    )
  ) {
    return undefined
  }
  const paths = uniqueUnmergedPaths(directory)
  return paths.length > 0 ? paths : undefined
}

function quoteForGitCommand(relativePath: string): string {
  return `'${relativePath.replace(/'/g, `'\\''`)}'`
}

function describePausedRebaseConflict(
  directory: string,
  conflictPaths: string[]
): string {
  const named = conflictPaths.map((p) => `"${p}"`).join(', ')
  const addExamples = conflictPaths
    .map((p) => `git add -- ${quoteForGitCommand(p)}`)
    .join(', then ')
  return (
    `Git paused a rebase with a conflict in ${named}. ` +
    'Inspect the checkout with git status. ' +
    `Edit the conflicted file to the chosen text, then ${addExamples} ` +
    'and git rebase --continue. ' +
    'After it finishes, inspect the result and run ' +
    `"donut notebook publish ${directory}" if unpublished work remains. ` +
    'To abandon resolution, run git rebase --abort.'
  )
}

const REBASE_EDITOR_ENV = {
  GIT_EDITOR: 'true',
  GIT_SEQUENCE_EDITOR: 'true',
} as const

function stageBlobBytes(
  directory: string,
  stage: 2 | 3,
  relativePath: string
): string | undefined {
  const result = spawnSync(
    'git',
    ['-C', directory, 'show', `:${stage}:${relativePath}`],
    { encoding: 'utf8' }
  )
  if (result.error || result.status !== 0) return undefined
  return result.stdout
}

function isFinalLfOnlyEquivalent(accepted: string, local: string): boolean {
  return accepted === `${local}\n` || local === `${accepted}\n`
}

function tryAbsorbFinalLfOnlyConflict(
  directory: string,
  conflictPaths: string[]
): boolean {
  if (conflictPaths.length !== 1) return false
  const relativePath = conflictPaths[0]
  if (relativePath === undefined || !relativePath.endsWith('.md')) return false
  const accepted = stageBlobBytes(directory, 2, relativePath)
  const local = stageBlobBytes(directory, 3, relativePath)
  if (accepted === undefined || local === undefined) return false
  if (!isFinalLfOnlyEquivalent(accepted, local)) return false

  runSystemGitOrThrow(
    ['-C', directory, 'checkout', '--ours', '--', relativePath],
    (detail, status) =>
      `failed to keep the accepted note while absorbing a final-LF-only conflict${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
  runSystemGitOrThrow(
    ['-C', directory, 'add', '--', relativePath],
    (detail, status) =>
      `failed to stage the accepted note while absorbing a final-LF-only conflict${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
  runSystemGitOrThrow(
    ['-C', directory, 'rebase', '--skip'],
    (detail, status) =>
      `failed to discard the redundant local replay after a final-LF-only conflict${detail ? `: ${detail}` : ` (exit code ${status})`}`,
    { env: { ...process.env, ...REBASE_EDITOR_ENV } }
  )
  return true
}

/**
 * Rebases the single unpublished local commit onto acceptedHead, preserving
 * the unpublished author identity. Surfaces a paused conflict with recovery
 * guidance when Git stops mid-rebase.
 */
export function rebaseUnpublishedCommit(
  directory: string,
  acceptedHead: string,
  localParent: string
): void {
  const { name, email } = unpublishedCommitAuthor(directory)
  try {
    runSystemGitOrThrow(
      [
        '-C',
        directory,
        '-c',
        `user.name=${name}`,
        '-c',
        `user.email=${email}`,
        'rebase',
        '--onto',
        acceptedHead,
        localParent,
      ],
      (detail, status) =>
        `failed to rebase the unpublished local commit onto the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`,
      {
        env: {
          ...process.env,
          ...REBASE_EDITOR_ENV,
        },
      }
    )
  } catch (e) {
    const conflictPaths = pausedRebaseConflictPaths(directory)
    if (conflictPaths === undefined) throw e
    if (tryAbsorbFinalLfOnlyConflict(directory, conflictPaths)) return
    const cause = e instanceof Error ? e.message : String(e)
    throw new Error(
      `${describePausedRebaseConflict(directory, conflictPaths)}\n${cause}`
    )
  }
}
