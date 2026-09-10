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

const LOCAL_MULTIPLE_COMMITS =
  'Local main cannot receive the accepted history because it contains more than one unpublished commit. ' +
  'Reduce local work to one unpublished commit that edits one existing ordinary Markdown note, then try again.'

const LOCAL_MERGE =
  'Local main cannot receive the accepted history because the unpublished commit is a merge. ' +
  'Recreate the change as one ordinary commit that edits one existing ordinary Markdown note, then try again.'

const LOCAL_NOT_CONTENT_EDIT =
  'Local main cannot receive the accepted history because the unpublished commit is not one existing-note content edit. ' +
  'Recreate it as one unpublished commit that edits one existing ordinary Markdown note at an unchanged path, then try again.'

export const LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION =
  'Local work is preserved; reconcile or recreate it as one supported commit directly on accepted history before publication.'

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
  | { kind: 'rebase'; localParent: string }
  | {
      kind: 'exact-subtree-move-replay'
      mapping: ExactAcceptedSubtreeMapping
      localPath: string
    }
  | { kind: 'reject'; message: string }

/**
 * Returns fast-forward when local main is already an ancestor of accepted.
 * Any nonempty ordinary-content-edit unpublished commit already based on
 * accepted main stays as-is, independent of its path count.
 * Any nonempty ordinary-content-edit unpublished commit rebases over a
 * contiguous single-parent chain of accepted content-only edits, independent
 * of local path count, accepted commit count, or accepted path count per
 * commit; and over a contiguous chain in which each accepted commit contains
 * only ordinary-note content saves and/or ordinary note additions at the
 * root or a folder already represented in that commit's preceding accepted
 * tree, independent of how many additions or saves each commit carries or
 * how they are grouped across commits.
 * Eligible one-note content edits of a descendant under one accepted exact
 * same-name subtree relocation replay onto the mapped path; batches of more
 * than one local path do not use that replay.
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

  const localPaths = ordinaryNoteContentEditPaths(
    acceptedRepoDir,
    parent,
    candidate.sha
  )
  if (localPaths === undefined) {
    return { kind: 'reject', message: LOCAL_NOT_CONTENT_EDIT }
  }
  if (parent === acceptedHead) {
    return { kind: 'already-based' }
  }

  const acceptedInterval = inspectAcceptedInterval(
    acceptedRepoDir,
    parent,
    acceptedHead
  )
  if (acceptedInterval.kind === 'non-linear') {
    return { kind: 'reject', message: LOCAL_NON_LINEAR_ACCEPTED }
  }
  if (acceptedInterval.kind === 'exact-subtree-move') {
    const [localPath] = localPaths
    if (
      localPaths.length !== 1 ||
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
  return { kind: 'rebase', localParent: parent }
}

function ordinaryNoteContentEditPaths(
  acceptedRepoDir: string,
  parent: string,
  commit: string
): string[] | undefined {
  const changes = listCommitChanges(acceptedRepoDir, parent, commit)
  if (
    changes.length === 0 ||
    !changes.every((change) => isOrdinaryNoteContentChange(change))
  ) {
    return undefined
  }
  return changes.map((change) => change.path)
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
