import { runSystemGitOrThrow } from './systemGit.js'
import {
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
} from './notebookAcceptedCommitChanges.js'
import { isEligibleBoundedAcceptedAdditionInterval } from './notebookAcceptedAdditionInterval.js'

export {
  inspectAncestryFailure,
  isOrdinaryNoteContentChange,
  listCommitChanges,
} from './notebookAcceptedCommitChanges.js'

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

export function firstStructuralPathInAcceptedInterval(
  acceptedRepoDir: string,
  localParent: string,
  acceptedHead: string
): string | undefined {
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
    return undefined
  }
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
