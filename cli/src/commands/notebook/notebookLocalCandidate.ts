import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_ANCESTRY_ERROR =
  'Local main cannot receive the accepted history because it contains unpublished or unrelated commits. ' +
  'Publish or reconcile those commits, then try again.'

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

function samePathOverlapError(changedPath: string): string {
  return (
    `Local main cannot receive the accepted history because accepted history also edited "${changedPath}". ` +
    'Same-note reconciliation is not supported yet.'
  )
}

const REGULAR_FILE_MODE = '100644'

/**
 * Returns undefined when local main is already an ancestor of accepted (fast-forward).
 * Otherwise returns specific guidance for an unsupported local history shape, same-path
 * overlap in accepted history, or the receive-gate message when the unpublished work is
 * one existing ordinary-note content edit of a disjoint path.
 */
export function unpublishedLocalHistoryRejection(
  acceptedRepoDir: string,
  localHead: string,
  acceptedHead: string
): string | undefined {
  if (historiesAreUnrelated(acceptedRepoDir, localHead, acceptedHead)) {
    return LOCAL_UNRELATED
  }

  const unpublished = listCommits(
    acceptedRepoDir,
    localHead,
    '--not',
    acceptedHead
  )
  if (unpublished.length === 0) return undefined
  if (unpublished.length > 1) return LOCAL_MULTIPLE_COMMITS

  const [candidate] = unpublished
  const parent = candidate.parents[0]
  if (parent === undefined || candidate.parents.length !== 1) {
    return LOCAL_MERGE
  }
  const localPath = ordinaryNoteContentEditPath(
    acceptedRepoDir,
    parent,
    candidate.sha
  )
  if (localPath === undefined) return LOCAL_NOT_CONTENT_EDIT
  if (
    acceptedIntervalTouchesPath(
      acceptedRepoDir,
      parent,
      acceptedHead,
      localPath
    )
  ) {
    return samePathOverlapError(localPath)
  }
  return RECEIVE_ANCESTRY_ERROR
}

function inspectAncestryFailure(
  detail: string | undefined,
  status: number | null
): string {
  return `failed to inspect local main's ancestry${detail ? `: ${detail}` : ` (exit code ${status})`}`
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

function listCommitChanges(
  acceptedRepoDir: string,
  parent: string,
  commit: string
): { srcMode: string; dstMode: string; status: string; path: string }[] {
  const raw = runSystemGitOrThrow(
    [
      '-C',
      acceptedRepoDir,
      '-c',
      'diff.renames=false',
      'diff-tree',
      '-r',
      '--raw',
      '-z',
      '--no-renames',
      '--no-commit-id',
      parent,
      commit,
    ],
    inspectAncestryFailure
  )
  return parseDiffTreeRawZ(raw)
}

function ordinaryNoteContentEditPath(
  acceptedRepoDir: string,
  parent: string,
  commit: string
): string | undefined {
  const changes = listCommitChanges(acceptedRepoDir, parent, commit)
  if (changes.length !== 1) return undefined
  const [change] = changes
  if (
    change.status === 'M' &&
    change.srcMode === REGULAR_FILE_MODE &&
    change.dstMode === REGULAR_FILE_MODE &&
    isOrdinaryNotePath(change.path)
  ) {
    return change.path
  }
  return undefined
}

function acceptedIntervalTouchesPath(
  acceptedRepoDir: string,
  localParent: string,
  acceptedHead: string,
  changedPath: string
): boolean {
  const interval = listCommits(
    acceptedRepoDir,
    acceptedHead,
    '--not',
    localParent
  )
  return interval.some((commit) =>
    commit.parents.some((parent) =>
      listCommitChanges(acceptedRepoDir, parent, commit.sha).some(
        (change) => change.path === changedPath
      )
    )
  )
}

function parseDiffTreeRawZ(
  output: string
): { srcMode: string; dstMode: string; status: string; path: string }[] {
  if (output === '') return []
  const parts = output.split('\0')
  if (parts[parts.length - 1] === '') parts.pop()
  if (parts.length % 2 !== 0) {
    throw new Error(
      "failed to inspect local main's ancestry: malformed diff-tree output"
    )
  }
  const changes: {
    srcMode: string
    dstMode: string
    status: string
    path: string
  }[] = []
  for (let i = 0; i < parts.length; i += 2) {
    const meta = parts[i]
    const path = parts[i + 1]
    const match =
      /^:(\d{6}) (\d{6}) ([0-9a-f]+) ([0-9a-f]+) ([A-Z][0-9]*)$/.exec(meta)
    if (!match || path === undefined || path === '') {
      throw new Error(
        "failed to inspect local main's ancestry: malformed diff-tree output"
      )
    }
    changes.push({
      srcMode: match[1],
      dstMode: match[2],
      status: match[5],
      path,
    })
  }
  return changes
}

function isOrdinaryNotePath(changedPath: string): boolean {
  if (!changedPath.endsWith('.md')) return false
  const segments = changedPath.split('/')
  if (
    segments.some(
      (segment) => segment === '' || segment === '.' || segment === '..'
    )
  ) {
    return false
  }
  return segments[segments.length - 1] !== 'README.md'
}
