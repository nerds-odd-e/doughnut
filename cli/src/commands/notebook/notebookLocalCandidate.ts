import { runSystemGitOrThrow } from './systemGit.js'

const LOCAL_UNRELATED =
  'Local main cannot receive the accepted history because it does not share Git history with the accepted notebook. ' +
  'Clone the notebook with "donut notebook clone", then try again.'

const LOCAL_MERGE =
  'Local main cannot receive the accepted history because an unpublished commit is a merge. ' +
  'Recreate the local work as ordinary commits, then try again.'

export type UnpublishedLocalHistoryDecision =
  | { kind: 'fast-forward' }
  | { kind: 'already-based' }
  | { kind: 'rebase'; mergeBase: string }
  | { kind: 'reject'; message: string }

function inspectAncestryFailure(
  detail: string | undefined,
  status: number | null
): string {
  return `failed to inspect local main's ancestry${detail ? `: ${detail}` : ` (exit code ${status})`}`
}

function listCommits(
  acceptedRepoDir: string,
  ...revListArgs: string[]
): { sha: string; parents: string[] }[] {
  const text = runSystemGitOrThrow(
    ['-C', acceptedRepoDir, 'rev-list', '--parents', ...revListArgs],
    inspectAncestryFailure
  ).trim()
  if (text === '') return []
  return text.split('\n').map((line) => {
    const [sha, ...parents] = line.split(/\s+/).filter((part) => part !== '')
    if (!sha) {
      throw new Error("failed to inspect local main's ancestry")
    }
    return { sha, parents }
  })
}

/**
 * Local main that shares history with accepted main fast-forwards when it has
 * nothing unpublished, stays as-is when its unpublished work is already based
 * on accepted main, and otherwise rebases its unpublished commits over the
 * accepted history, leaving Git to decide what replays. Unpublished history
 * must be a linear chain of single-parent commits; what those commits change
 * is left to publication validation.
 */
export function inspectUnpublishedLocalHistory(
  acceptedRepoDir: string,
  localHead: string,
  acceptedHead: string
): UnpublishedLocalHistoryDecision {
  if (historiesAreUnrelated(acceptedRepoDir, localHead, acceptedHead)) {
    return { kind: 'reject', message: LOCAL_UNRELATED }
  }

  const unpublished = listCommits(
    acceptedRepoDir,
    localHead,
    '--not',
    acceptedHead
  )
  if (unpublished.length === 0) return { kind: 'fast-forward' }
  if (unpublished.some((commit) => commit.parents.length !== 1)) {
    return { kind: 'reject', message: LOCAL_MERGE }
  }

  const mergeBase = runSystemGitOrThrow(
    ['-C', acceptedRepoDir, 'merge-base', localHead, acceptedHead],
    inspectAncestryFailure
  ).trim()
  if (mergeBase === acceptedHead) {
    return { kind: 'already-based' }
  }
  return { kind: 'rebase', mergeBase }
}

function commitCount(acceptedRepoDir: string, ...revs: string[]): number {
  const text = runSystemGitOrThrow(
    ['-C', acceptedRepoDir, 'rev-list', '--count', ...revs],
    inspectAncestryFailure
  ).trim()
  const count = Number(text)
  if (!Number.isInteger(count) || count < 0) {
    throw new Error(
      `failed to inspect local main's ancestry: unexpected revision count "${text}"`
    )
  }
  return count
}

function historiesAreUnrelated(
  acceptedRepoDir: string,
  localHead: string,
  acceptedHead: string
): boolean {
  const localCount = commitCount(acceptedRepoDir, localHead)
  const acceptedCount = commitCount(acceptedRepoDir, acceptedHead)
  const unionCount = commitCount(acceptedRepoDir, localHead, acceptedHead)
  return unionCount === localCount + acceptedCount
}
