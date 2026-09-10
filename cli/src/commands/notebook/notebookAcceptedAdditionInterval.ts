import { runSystemGitOrThrow } from './systemGit.js'
import {
  hasSingleParent,
  inspectAncestryFailure,
  isOrdinaryNoteAddition,
  isOrdinaryNoteContentChange,
  listCommitChanges,
} from './notebookAcceptedCommitChanges.js'

function isRepresentedFolder(
  acceptedRepoDir: string,
  commit: string,
  folderPath: string
): boolean {
  const listed = runSystemGitOrThrow(
    [
      '-C',
      acceptedRepoDir,
      'ls-tree',
      '-d',
      '--name-only',
      commit,
      '--',
      folderPath,
    ],
    inspectAncestryFailure
  ).trim()
  return listed === folderPath
}

function destinationParentIsRootOrRepresented(
  acceptedRepoDir: string,
  baseCommit: string,
  notePath: string
): boolean {
  const slash = notePath.lastIndexOf('/')
  if (slash === -1) return true
  return isRepresentedFolder(
    acceptedRepoDir,
    baseCommit,
    notePath.slice(0, slash)
  )
}

/**
 * An accepted interval is eligible through this addition path when every edge
 * (each commit against the accepted tree immediately before it in this same
 * interval, starting from `localParent`) contains only ordinary note
 * additions at a destination already represented at that point, and ordinary
 * content-only saves. A destination folder created by an earlier addition in
 * this same interval counts as represented, since it is already committed to
 * the accepted tree by that point; a folder first created within the same
 * commit as an addition into it does not count (new-folder receipt is a
 * separate, deferred capability). A save's target already existing at that
 * point is guaranteed by its diff status ('M' only occurs when the path was
 * present in the preceding tree). At least one addition must be present,
 * otherwise a purely content-only interval defers to the content-only
 * contiguity path in `notebookAcceptedInterval.ts`.
 */
export function isEligibleAcceptedAdditionInterval(
  acceptedRepoDir: string,
  localParent: string,
  interval: { sha: string; parents: string[] }[]
): boolean {
  let precedingTree = localParent
  let hasAddition = false
  for (const commit of interval) {
    if (!hasSingleParent(commit, precedingTree)) {
      return false
    }
    for (const change of listCommitChanges(
      acceptedRepoDir,
      precedingTree,
      commit.sha
    )) {
      if (isOrdinaryNoteAddition(change)) {
        if (
          !destinationParentIsRootOrRepresented(
            acceptedRepoDir,
            precedingTree,
            change.path
          )
        ) {
          return false
        }
        hasAddition = true
      } else if (!isOrdinaryNoteContentChange(change)) {
        return false
      }
    }
    precedingTree = commit.sha
  }
  return hasAddition
}
