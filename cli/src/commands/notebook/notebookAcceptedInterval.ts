import { runSystemGitOrThrow } from './systemGit.js'
import {
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
} from './notebookAcceptedCommitChanges.js'
import { isEligibleBoundedAcceptedAdditionInterval } from './notebookAcceptedAdditionInterval.js'
import {
  exactAcceptedSubtreeMapping,
  type ExactAcceptedSubtreeMapping,
} from './notebookAcceptedExactSubtreeMapping.js'

export {
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
} from './notebookAcceptedCommitChanges.js'

export type { ExactAcceptedSubtreeMapping }

/**
 * Accepted history since the local parent, classified for pull eligibility.
 * Exact subtree moves are classified; callers may still refuse them as
 * unsupported structural history.
 */
export type AcceptedIntervalInspection =
  | { kind: 'rebaseable' }
  | {
      kind: 'exact-subtree-move'
      mapping: ExactAcceptedSubtreeMapping
      structuralPath: string
    }
  | { kind: 'structural'; path: string }

export function listCommits(
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

export function inspectAcceptedInterval(
  acceptedRepoDir: string,
  localParent: string,
  acceptedHead: string
): AcceptedIntervalInspection {
  const interval = listCommits(
    acceptedRepoDir,
    '--reverse',
    acceptedHead,
    '--not',
    localParent
  )
  if (
    isEligibleBoundedAcceptedAdditionInterval(
      acceptedRepoDir,
      localParent,
      interval
    )
  ) {
    return { kind: 'rebaseable' }
  }

  const structuralPath = firstNonContentChangePath(acceptedRepoDir, interval)
  if (structuralPath === undefined) {
    return { kind: 'rebaseable' }
  }

  const mapping = exactSubtreeMappingForSingleEdge(
    acceptedRepoDir,
    localParent,
    interval
  )
  if (mapping !== undefined) {
    return {
      kind: 'exact-subtree-move',
      mapping,
      structuralPath,
    }
  }
  return { kind: 'structural', path: structuralPath }
}

function exactSubtreeMappingForSingleEdge(
  acceptedRepoDir: string,
  localParent: string,
  interval: { sha: string; parents: string[] }[]
): ExactAcceptedSubtreeMapping | undefined {
  if (interval.length !== 1) return undefined
  const [commit] = interval
  if (
    commit === undefined ||
    commit.parents.length !== 1 ||
    commit.parents[0] !== localParent
  ) {
    return undefined
  }
  return exactAcceptedSubtreeMapping(acceptedRepoDir, localParent, commit.sha)
}

function firstNonContentChangePath(
  acceptedRepoDir: string,
  interval: { sha: string; parents: string[] }[]
): string | undefined {
  for (const commit of interval) {
    for (const parent of commit.parents) {
      for (const change of listCommitChanges(
        acceptedRepoDir,
        parent,
        commit.sha
      )) {
        if (!isOrdinaryNoteContentChange(change)) return change.path
      }
    }
  }
  return undefined
}
