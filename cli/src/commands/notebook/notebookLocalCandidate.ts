import { runSystemGitOrThrow } from './systemGit.js'
import {
  firstStructuralPathInAcceptedInterval,
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
  listCommits,
} from './notebookAcceptedInterval.js'

const LOCAL_UNRELATED =
  'Local main cannot receive the accepted history because it does not share Git history with the accepted notebook. ' +
  'Clone the notebook with "donut notebook clone", then try again.'

const LOCAL_MULTIPLE_COMMITS =
  'Local main cannot receive the accepted history because it contains more than one unpublished commit. ' +
  'Reduce local work to one unpublished commit that edits one existing ordinary Markdown note, then try again.'

const LOCAL_MERGE =
  'Local main cannot receive the accepted history because the unpublished commit is a merge. ' +
  'Recreate the change as one ordinary commit that edits one existing ordinary Markdown note, then try again.'

const LOCAL_NOT_CONTENT_EDIT =
  'Local main cannot receive the accepted history because the unpublished commit is not one existing-note content edit. ' +
  'Recreate it as one unpublished commit that edits one existing ordinary Markdown note at an unchanged path, then try again.'

function structuralChangeError(changedPath: string): string {
  return (
    `Local main cannot receive the accepted history because accepted history includes a structural change at "${changedPath}". ` +
    'Divergent structural history is not supported yet.'
  )
}

export type UnpublishedLocalHistoryDecision =
  | { kind: 'fast-forward' }
  | { kind: 'already-based' }
  | { kind: 'rebase'; localParent: string }
  | { kind: 'reject'; message: string }

/**
 * Returns fast-forward when local main is already an ancestor of accepted.
 * Eligible one-note content edits already based on accepted main stay as-is.
 * Eligible one-note content edits over content-only accepted history rebase,
 * including same-note content edits.
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
  if (unpublished.length > 1) {
    return { kind: 'reject', message: LOCAL_MULTIPLE_COMMITS }
  }

  const [candidate] = unpublished
  const parent = candidate.parents[0]
  if (parent === undefined || candidate.parents.length !== 1) {
    return { kind: 'reject', message: LOCAL_MERGE }
  }
  if (!isOneOrdinaryNoteContentEdit(acceptedRepoDir, parent, candidate.sha)) {
    return { kind: 'reject', message: LOCAL_NOT_CONTENT_EDIT }
  }
  const structuralPath = firstStructuralPathInAcceptedInterval(
    acceptedRepoDir,
    parent,
    acceptedHead
  )
  if (structuralPath !== undefined) {
    return { kind: 'reject', message: structuralChangeError(structuralPath) }
  }
  if (parent === acceptedHead) {
    return { kind: 'already-based' }
  }
  return { kind: 'rebase', localParent: parent }
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

function isOneOrdinaryNoteContentEdit(
  acceptedRepoDir: string,
  parent: string,
  commit: string
): boolean {
  const changes = listCommitChanges(acceptedRepoDir, parent, commit)
  if (changes.length !== 1) return false
  const [change] = changes
  return change !== undefined && isOrdinaryNoteContentChange(change)
}
