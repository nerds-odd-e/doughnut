import { runSystemGitOrThrow } from './systemGit.js'
import { inspectAncestryFailure } from './notebookAcceptedCommitChanges.js'

const REGULAR_FILE_MODE = '100644'

/** One exact same-name subtree relocation accepted between a shared base and accepted head. */
export interface ExactAcceptedSubtreeMapping {
  sourcePrefix: string
  destPrefix: string
}

interface InspectedRegularFile {
  path: string
  baseBlobId: string | null
  acceptedBlobId: string | null
}

/**
 * Recognizes one complete same-name README-backed subtree mapping between two
 * commits, matching the server's NotebookGitProposalFolderShape contract.
 * Returns undefined when the trees are not that unique exact relocation.
 */
export function exactAcceptedSubtreeMapping(
  acceptedRepoDir: string,
  baseCommit: string,
  acceptedCommit: string
): ExactAcceptedSubtreeMapping | undefined {
  const baseEntries = listRegularBlobEntries(acceptedRepoDir, baseCommit)
  const acceptedEntries = listRegularBlobEntries(
    acceptedRepoDir,
    acceptedCommit
  )
  if (baseEntries === undefined || acceptedEntries === undefined) {
    return undefined
  }
  return requireExactOrEmpty(
    inspectRegularFilesBetween(baseEntries, acceptedEntries)
  )
}

function listRegularBlobEntries(
  acceptedRepoDir: string,
  commit: string
): Map<string, string> | undefined {
  const raw = runSystemGitOrThrow(
    ['-C', acceptedRepoDir, 'ls-tree', '-r', '-z', commit],
    inspectAncestryFailure
  )
  if (raw === '') return new Map()
  const parts = raw.split('\0')
  if (parts[parts.length - 1] === '') parts.pop()
  const entries = new Map<string, string>()
  for (const part of parts) {
    const match = /^(\d{6}) blob ([0-9a-f]+)\t(.+)$/.exec(part)
    if (!match) {
      throw new Error(
        "failed to inspect local main's ancestry: malformed ls-tree output"
      )
    }
    const [, mode, blobId, path] = match
    if (
      mode !== REGULAR_FILE_MODE ||
      path === undefined ||
      blobId === undefined
    ) {
      return undefined
    }
    entries.set(path, blobId)
  }
  return entries
}

function inspectRegularFilesBetween(
  baseEntries: Map<string, string>,
  acceptedEntries: Map<string, string>
): InspectedRegularFile[] {
  const paths = [...new Set([...baseEntries.keys(), ...acceptedEntries.keys()])]
  paths.sort()
  return paths.map((path) => ({
    path,
    baseBlobId: baseEntries.get(path) ?? null,
    acceptedBlobId: acceptedEntries.get(path) ?? null,
  }))
}

function requireExactOrEmpty(
  files: InspectedRegularFile[]
): ExactAcceptedSubtreeMapping | undefined {
  const removedReadmes: string[] = []
  const addedReadmes: string[] = []
  for (const file of files) {
    if (!isReadme(file.path)) continue
    if (file.baseBlobId !== null && file.acceptedBlobId === null) {
      removedReadmes.push(file.path)
    } else if (file.baseBlobId === null && file.acceptedBlobId !== null) {
      addedReadmes.push(file.path)
    }
  }
  if (removedReadmes.length === 0 || addedReadmes.length === 0) {
    return undefined
  }

  const sourcePrefixes = outermost(prefixesOf(removedReadmes))
  const destPrefixes = outermost(prefixesOf(addedReadmes))
  const complete: ExactAcceptedSubtreeMapping[] = []
  for (const source of sourcePrefixes) {
    for (const dest of destPrefixes) {
      if (folderName(source) !== folderName(dest)) continue
      if (correspondenceBreaker(files, source, dest) === null) {
        complete.push({ sourcePrefix: source, destPrefix: dest })
      }
    }
  }

  if (complete.length !== 1) return undefined
  const mapping = complete[0]
  if (mapping === undefined) return undefined
  if (
    firstChangeOutside(files, mapping.sourcePrefix, mapping.destPrefix) !== null
  ) {
    return undefined
  }
  return mapping
}

function correspondenceBreaker(
  files: InspectedRegularFile[],
  sourcePrefix: string,
  destPrefix: string
): string | null {
  const baseRelative = new Map<string, string>()
  const acceptedRelative = new Map<string, string>()
  let leftover: string | null = null
  for (const file of files) {
    if (file.baseBlobId !== null && under(file.path, sourcePrefix)) {
      baseRelative.set(relative(file.path, sourcePrefix), file.baseBlobId)
      if (file.acceptedBlobId !== null && leftover === null) {
        leftover = file.path
      }
    }
    if (file.acceptedBlobId !== null && under(file.path, destPrefix)) {
      acceptedRelative.set(relative(file.path, destPrefix), file.acceptedBlobId)
    }
  }
  if (leftover !== null) return leftover
  const relatives = new Set([
    ...baseRelative.keys(),
    ...acceptedRelative.keys(),
  ])
  for (const rel of relatives) {
    const baseBlob = baseRelative.get(rel)
    const acceptedBlob = acceptedRelative.get(rel)
    if (
      baseBlob === undefined ||
      acceptedBlob === undefined ||
      baseBlob !== acceptedBlob
    ) {
      return join(destPrefix, rel)
    }
  }
  return null
}

function firstChangeOutside(
  files: InspectedRegularFile[],
  sourcePrefix: string,
  destPrefix: string
): string | null {
  for (const file of files) {
    if (
      unchanged(file) ||
      under(file.path, sourcePrefix) ||
      under(file.path, destPrefix)
    ) {
      continue
    }
    return file.path
  }
  return null
}

function unchanged(file: InspectedRegularFile): boolean {
  return (
    file.baseBlobId !== null &&
    file.acceptedBlobId !== null &&
    file.baseBlobId === file.acceptedBlobId
  )
}

function prefixesOf(readmePaths: string[]): string[] {
  return readmePaths.map(prefix)
}

function outermost(prefixes: string[]): string[] {
  const result: string[] = []
  for (const candidate of prefixes) {
    const nested = prefixes.some(
      (other) => other !== candidate && under(candidate, other)
    )
    if (!(nested || result.includes(candidate))) result.push(candidate)
  }
  return result
}

function isReadme(path: string): boolean {
  const slash = path.lastIndexOf('/')
  return (slash < 0 ? path : path.slice(slash + 1)) === 'README.md'
}

function prefix(readmePath: string): string {
  const slash = readmePath.lastIndexOf('/')
  return slash < 0 ? '' : readmePath.slice(0, slash)
}

function folderName(folderPrefix: string): string {
  const slash = folderPrefix.lastIndexOf('/')
  return slash < 0 ? folderPrefix : folderPrefix.slice(slash + 1)
}

function under(path: string, folderPrefix: string): boolean {
  if (folderPrefix === '') return true
  return path.startsWith(`${folderPrefix}/`)
}

function relative(path: string, folderPrefix: string): string {
  return folderPrefix === '' ? path : path.slice(folderPrefix.length + 1)
}

function join(folderPrefix: string, rel: string): string {
  return folderPrefix === '' ? rel : `${folderPrefix}/${rel}`
}
