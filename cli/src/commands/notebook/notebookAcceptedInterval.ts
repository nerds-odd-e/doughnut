import { runSystemGitOrThrow } from './systemGit.js'
import {
  hasSingleParent,
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
} from './notebookAcceptedCommitChanges.js'
import { isEligibleAcceptedAdditionInterval } from './notebookAcceptedAdditionInterval.js'
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
 * Exact subtree moves carry correspondence for one local descendant content
 * edit to replay onto the mapped path.
 */
export type AcceptedIntervalInspection =
  | { kind: 'rebaseable' }
  | {
      kind: 'exact-subtree-move'
      mapping: ExactAcceptedSubtreeMapping
      structuralPath: string
    }
  | { kind: 'structural'; path: string }
  | { kind: 'non-linear' }

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
  mergeBase: string,
  acceptedHead: string
): AcceptedIntervalInspection {
  const interval = listCommits(
    acceptedRepoDir,
    '--reverse',
    acceptedHead,
    '--not',
    mergeBase
  )
  if (
    isEligibleAcceptedAdditionInterval(acceptedRepoDir, mergeBase, interval)
  ) {
    return { kind: 'rebaseable' }
  }

  const structuralPath = firstNonContentChangePath(acceptedRepoDir, interval)
  if (structuralPath === undefined) {
    if (!isContiguousSingleParentChain(interval, mergeBase, acceptedHead)) {
      return { kind: 'non-linear' }
    }
    return { kind: 'rebaseable' }
  }

  const mapping = exactSubtreeMappingForSingleEdge(
    acceptedRepoDir,
    mergeBase,
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
  mergeBase: string,
  interval: { sha: string; parents: string[] }[]
): ExactAcceptedSubtreeMapping | undefined {
  if (interval.length !== 1) return undefined
  const [commit] = interval
  if (commit === undefined || !hasSingleParent(commit, mergeBase)) {
    return undefined
  }
  return exactAcceptedSubtreeMapping(acceptedRepoDir, mergeBase, commit.sha)
}

/**
 * A content-only accepted interval is only eligible when it forms one
 * contiguous single-parent chain from `mergeBase` to `acceptedHead`: each
 * commit has exactly one parent, that parent is the previous commit in the
 * chain (the first commit's parent is `mergeBase`), and the chain's last
 * commit is `acceptedHead`. This rejects a merge or any other non-linear
 * shape reachable from `acceptedHead` that a per-edge content-only check
 * alone cannot distinguish from a genuine linear save history.
 */
function isContiguousSingleParentChain(
  interval: { sha: string; parents: string[] }[],
  mergeBase: string,
  acceptedHead: string
): boolean {
  let expectedParent = mergeBase
  for (const commit of interval) {
    if (!hasSingleParent(commit, expectedParent)) {
      return false
    }
    expectedParent = commit.sha
  }
  return expectedParent === acceptedHead
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
