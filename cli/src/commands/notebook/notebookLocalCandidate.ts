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

const LOCAL_TWO_NOTE_UNSUPPORTED_ACCEPTED =
  'Local main cannot receive the accepted history because the two-note unpublished commit can only rebase over exactly one accepted content save of a different existing ordinary Markdown note. ' +
  'Local work is preserved; reconcile or recreate it as one supported commit directly on accepted history before publication.'

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
 * Eligible one-note content edits rebase over content-only accepted history,
 * including same-note content edits, and over one accepted ordinary-note
 * addition at the root or an already represented folder, optionally followed
 * by one content save of that same newly added note.
 * Eligible two-note content edits rebase only over exactly one accepted
 * content save of a third different existing ordinary note whose sole parent
 * is the local parent.
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
  if (localPaths.length === 2) {
    return decideTwoNoteBatchRebase(
      acceptedRepoDir,
      parent,
      acceptedHead,
      localPaths
    )
  }
  if (localPaths.length !== 1) {
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

function decideTwoNoteBatchRebase(
  acceptedRepoDir: string,
  localParent: string,
  acceptedHead: string,
  localPaths: string[]
): UnpublishedLocalHistoryDecision {
  if (localParent === acceptedHead) {
    return { kind: 'reject', message: LOCAL_NOT_CONTENT_EDIT }
  }

  const interval = listCommits(
    acceptedRepoDir,
    '--reverse',
    acceptedHead,
    '--not',
    localParent
  )
  if (interval.length !== 1) {
    return { kind: 'reject', message: LOCAL_TWO_NOTE_UNSUPPORTED_ACCEPTED }
  }

  const [accepted] = interval
  if (
    accepted === undefined ||
    accepted.parents.length !== 1 ||
    accepted.parents[0] !== localParent
  ) {
    return { kind: 'reject', message: LOCAL_TWO_NOTE_UNSUPPORTED_ACCEPTED }
  }

  const acceptedPaths = ordinaryNoteContentEditPaths(
    acceptedRepoDir,
    localParent,
    accepted.sha
  )
  if (acceptedPaths === undefined) {
    const structural = listCommitChanges(
      acceptedRepoDir,
      localParent,
      accepted.sha
    ).find((change) => !isOrdinaryNoteContentChange(change))
    return {
      kind: 'reject',
      message:
        structural !== undefined
          ? structuralChangeError(structural.path)
          : LOCAL_TWO_NOTE_UNSUPPORTED_ACCEPTED,
    }
  }
  const [acceptedPath] = acceptedPaths
  if (
    acceptedPaths.length !== 1 ||
    acceptedPath === undefined ||
    localPaths.includes(acceptedPath)
  ) {
    return { kind: 'reject', message: LOCAL_TWO_NOTE_UNSUPPORTED_ACCEPTED }
  }
  return { kind: 'rebase', localParent }
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
