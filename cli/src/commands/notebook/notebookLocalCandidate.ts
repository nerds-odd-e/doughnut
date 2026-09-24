import { runSystemGitOrThrow } from './systemGit.js'
import {
  inspectAcceptedInterval,
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
  listCommits,
  type ExactAcceptedSubtreeMapping,
} from './notebookAcceptedInterval.js'
import { mapPathUnderExactSubtree } from './notebookAcceptedExactSubtreeMapping.js'

const LOCAL_UNRELATED =
  'Local main cannot receive the accepted history because it does not share Git history with the accepted notebook. ' +
  'Clone the notebook with "donut notebook clone", then try again.'

const LOCAL_MERGE =
  'Local main cannot receive the accepted history because an unpublished commit is a merge. ' +
  'Recreate the local work as ordinary commits, then try again.'

const LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION =
  'Local work is preserved, but it cannot be published from this checkout. To publish it, clone the notebook fresh elsewhere ("donut notebook clone") and move the unpublished work across as new commits there.'

const LOCAL_NON_LINEAR_ACCEPTED =
  'Local main cannot receive the accepted history because the accepted history since the local parent is not one contiguous chain of saves. ' +
  LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION

function structuralChangeError(changedPath: string): string {
  return (
    `Local main cannot receive the accepted history because accepted history includes a structural change at "${changedPath}". ` +
    LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION
  )
}

export type UnpublishedLocalHistoryDecision =
  | { kind: 'fast-forward' }
  | { kind: 'already-based' }
  | { kind: 'rebase'; mergeBase: string }
  | {
      kind: 'exact-subtree-move-replay'
      mapping: ExactAcceptedSubtreeMapping
      localPath: string
    }
  | { kind: 'reject'; message: string }

/**
 * Returns fast-forward when local main is already an ancestor of accepted.
 * Unpublished history must be a linear chain of single-parent commits; what
 * those commits change is left to publication validation.
 * Unpublished work whose merge base is accepted main stays as-is.
 * Unpublished work rebases over a contiguous single-parent chain of accepted
 * content-only edits, independent of accepted commit count or accepted path
 * count per commit; and over a contiguous chain in which each accepted commit
 * contains only ordinary-note content saves and/or ordinary note additions at
 * the root or a folder already represented in that commit's preceding
 * accepted tree, independent of how many additions or saves each commit
 * carries or how they are grouped across commits.
 * One local commit editing one descendant note under one accepted exact
 * same-name subtree relocation replays onto the mapped path; any other local
 * work refuses that accepted history as structural.
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

  const acceptedInterval = inspectAcceptedInterval(
    acceptedRepoDir,
    mergeBase,
    acceptedHead
  )
  if (acceptedInterval.kind === 'non-linear') {
    return { kind: 'reject', message: LOCAL_NON_LINEAR_ACCEPTED }
  }
  if (acceptedInterval.kind === 'exact-subtree-move') {
    const localPath = singleNoteContentEditPath(
      acceptedRepoDir,
      mergeBase,
      localHead
    )
    if (
      unpublished.length !== 1 ||
      localPath === undefined ||
      mapPathUnderExactSubtree(localPath, acceptedInterval.mapping) ===
        undefined
    ) {
      return {
        kind: 'reject',
        message: structuralChangeError(acceptedInterval.structuralPath),
      }
    }
    return {
      kind: 'exact-subtree-move-replay',
      mapping: acceptedInterval.mapping,
      localPath,
    }
  }
  if (acceptedInterval.kind !== 'rebaseable') {
    return {
      kind: 'reject',
      message: structuralChangeError(acceptedInterval.path),
    }
  }
  return { kind: 'rebase', mergeBase }
}

function singleNoteContentEditPath(
  acceptedRepoDir: string,
  parent: string,
  commit: string
): string | undefined {
  const [change, ...others] = listCommitChanges(acceptedRepoDir, parent, commit)
  if (change === undefined || others.length > 0) return undefined
  return isOrdinaryNoteContentChange(change) ? change.path : undefined
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
