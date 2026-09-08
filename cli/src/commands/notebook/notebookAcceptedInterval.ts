import { runSystemGitOrThrow } from './systemGit.js'

const REGULAR_FILE_MODE = '100644'

interface CommitChange {
  srcMode: string
  dstMode: string
  status: string
  path: string
}

export function inspectAncestryFailure(
  detail: string | undefined,
  status: number | null
): string {
  return `failed to inspect local main's ancestry${detail ? `: ${detail}` : ` (exit code ${status})`}`
}

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

export function listCommitChanges(
  acceptedRepoDir: string,
  parent: string,
  commit: string
): CommitChange[] {
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

export function isOrdinaryNoteContentChange(change: CommitChange): boolean {
  return (
    change.status === 'M' &&
    change.srcMode === REGULAR_FILE_MODE &&
    change.dstMode === REGULAR_FILE_MODE &&
    isOrdinaryNotePath(change.path)
  )
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
    isEligibleOneOrdinaryNoteAddition(acceptedRepoDir, localParent, interval)
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

function isOrdinaryNoteAddition(change: CommitChange): boolean {
  return (
    change.status === 'A' &&
    change.srcMode === '000000' &&
    change.dstMode === REGULAR_FILE_MODE &&
    isOrdinaryNotePath(change.path)
  )
}

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

function isEligibleOneOrdinaryNoteAddition(
  acceptedRepoDir: string,
  localParent: string,
  interval: { sha: string; parents: string[] }[]
): boolean {
  if (interval.length !== 1) return false
  const [commit] = interval
  if (commit === undefined) return false
  const parent = commit.parents[0]
  if (parent === undefined || commit.parents.length !== 1) {
    return false
  }
  if (parent !== localParent) return false
  const changes = listCommitChanges(acceptedRepoDir, parent, commit.sha)
  if (changes.length !== 1) return false
  const [change] = changes
  return (
    change !== undefined &&
    isOrdinaryNoteAddition(change) &&
    destinationParentIsRootOrRepresented(
      acceptedRepoDir,
      localParent,
      change.path
    )
  )
}

function parseDiffTreeRawZ(output: string): CommitChange[] {
  if (output === '') return []
  const parts = output.split('\0')
  if (parts[parts.length - 1] === '') parts.pop()
  if (parts.length % 2 !== 0) {
    throw new Error(
      "failed to inspect local main's ancestry: malformed diff-tree output"
    )
  }
  const changes: CommitChange[] = []
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
