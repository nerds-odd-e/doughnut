import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { assertLocalMainIsReadyToReceive } from './notebookCheckoutReadiness.js'
import { inspectUnpublishedLocalHistory } from './notebookLocalCandidate.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_CHECKOUT_CHANGED =
  'Local main changed while the accepted history was downloading. Try again from the unchanged clean main.'

interface AcceptedNotebookReceiveResult {
  kind: 'unchanged' | 'already-based' | 'rebased' | 'absorbed' | 'fast-forward'
  acceptedHead: string
  localHead: string
}

function readHead(directory: string): string {
  return runSystemGitOrThrow(
    ['-C', directory, 'rev-parse', 'HEAD'],
    (detail, status) =>
      `failed to read local HEAD${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trim()
}

function assertCheckoutStillReady(
  directory: string,
  capturedHead: string
): void {
  assertLocalMainIsReadyToReceive(directory)
  if (readHead(directory) !== capturedHead) {
    throw new Error(RECEIVE_CHECKOUT_CHANGED)
  }
}

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

function rebaseUnpublishedCommit(
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
          GIT_EDITOR: 'true',
          GIT_SEQUENCE_EDITOR: 'true',
        },
      }
    )
  } catch (e) {
    const conflictPaths = pausedRebaseConflictPaths(directory)
    if (conflictPaths === undefined) throw e
    const cause = e instanceof Error ? e.message : String(e)
    throw new Error(
      `${describePausedRebaseConflict(directory, conflictPaths)}\n${cause}`
    )
  }
}

/**
 * Downloads accepted history and advances an unchanged, clean local main: equal heads stay
 * unchanged, an eligible already-based unpublished commit stays unpublished, eligible
 * content-only divergence rebases (an empty remaining patch leaves local main at the accepted
 * head), and ancestor checkouts fast-forward. Imported objects do not install a remote or a
 * persistent remote ref.
 */
export async function receiveAcceptedNotebookHead(
  directory: string,
  notebookId: number
): Promise<AcceptedNotebookReceiveResult> {
  const capturedHead = readHead(directory)

  return withDownloadedAcceptedNotebookHistory(
    notebookId,
    (acceptedRepoDir, acceptedHead) => {
      assertCheckoutStillReady(directory, capturedHead)

      if (capturedHead === acceptedHead) {
        return {
          kind: 'unchanged',
          acceptedHead,
          localHead: capturedHead,
        }
      }

      runSystemGitOrThrow(
        [
          '-C',
          acceptedRepoDir,
          'fetch',
          '--quiet',
          '--no-tags',
          path.resolve(directory),
          'refs/heads/main:refs/heads/local-main',
        ],
        (detail, status) =>
          `failed to import local main for ancestry inspection${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )
      const localHistory = inspectUnpublishedLocalHistory(
        acceptedRepoDir,
        capturedHead,
        acceptedHead
      )
      if (localHistory.kind === 'reject') throw new Error(localHistory.message)
      if (localHistory.kind === 'already-based') {
        return {
          kind: 'already-based',
          acceptedHead,
          localHead: capturedHead,
        }
      }

      runSystemGitOrThrow(
        [
          '-C',
          directory,
          'fetch',
          '--quiet',
          '--no-tags',
          '--no-write-fetch-head',
          acceptedRepoDir,
          acceptedHead,
        ],
        (detail, status) =>
          `failed to import accepted notebook history${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      assertCheckoutStillReady(directory, capturedHead)
      if (localHistory.kind === 'rebase') {
        rebaseUnpublishedCommit(
          directory,
          acceptedHead,
          localHistory.localParent
        )
        const localHead = readHead(directory)
        return {
          kind: localHead === acceptedHead ? 'absorbed' : 'rebased',
          acceptedHead,
          localHead,
        }
      }

      runSystemGitOrThrow(
        ['-C', directory, 'merge', '--quiet', '--ff-only', acceptedHead],
        (detail, status) =>
          `failed to fast-forward local main to the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      return {
        kind: 'fast-forward',
        acceptedHead,
        localHead: acceptedHead,
      }
    }
  )
}
