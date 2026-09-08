import { runSystemGitOrThrow } from './systemGit.js'
import {
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

export function isEligibleBoundedAcceptedAdditionInterval(
  acceptedRepoDir: string,
  localParent: string,
  interval: { sha: string; parents: string[] }[]
): boolean {
  return (
    isEligibleOneOrdinaryNoteAddition(acceptedRepoDir, localParent, interval) ||
    isEligibleOneOrdinaryNoteAdditionThenSave(
      acceptedRepoDir,
      localParent,
      interval
    )
  )
}

function isEligibleOneOrdinaryNoteAddition(
  acceptedRepoDir: string,
  localParent: string,
  interval: { sha: string; parents: string[] }[]
): boolean {
  if (interval.length !== 1) return false
  const [commit] = interval
  if (commit === undefined) return false
  const addedPath = singleOrdinaryNoteAdditionPath(
    acceptedRepoDir,
    localParent,
    commit
  )
  return (
    addedPath !== undefined &&
    destinationParentIsRootOrRepresented(
      acceptedRepoDir,
      localParent,
      addedPath
    )
  )
}

/** Exactly one addition of B on the shared base, then one content-only save of B. */
function isEligibleOneOrdinaryNoteAdditionThenSave(
  acceptedRepoDir: string,
  localParent: string,
  interval: { sha: string; parents: string[] }[]
): boolean {
  if (interval.length !== 2) return false
  const [creation, save] = interval
  if (creation === undefined || save === undefined) return false
  const addedPath = singleOrdinaryNoteAdditionPath(
    acceptedRepoDir,
    localParent,
    creation
  )
  if (
    addedPath === undefined ||
    !destinationParentIsRootOrRepresented(
      acceptedRepoDir,
      localParent,
      addedPath
    )
  ) {
    return false
  }
  const saveParent = save.parents[0]
  if (saveParent === undefined || save.parents.length !== 1) return false
  if (saveParent !== creation.sha) return false
  const saveChanges = listCommitChanges(acceptedRepoDir, saveParent, save.sha)
  if (saveChanges.length !== 1) return false
  const [saveChange] = saveChanges
  return (
    saveChange !== undefined &&
    isOrdinaryNoteContentChange(saveChange) &&
    saveChange.path === addedPath
  )
}

function singleOrdinaryNoteAdditionPath(
  acceptedRepoDir: string,
  expectedParent: string,
  commit: { sha: string; parents: string[] }
): string | undefined {
  const parent = commit.parents[0]
  if (parent === undefined || commit.parents.length !== 1) return undefined
  if (parent !== expectedParent) return undefined
  const changes = listCommitChanges(acceptedRepoDir, parent, commit.sha)
  if (changes.length !== 1) return undefined
  const [change] = changes
  if (change === undefined || !isOrdinaryNoteAddition(change)) return undefined
  return change.path
}
