import { spawnSync } from 'node:child_process'
import { exceptionText } from '../../exceptionText.js'
import { isEmptyLfsFile, parseLfsPointer } from './notebookLfsPointer.js'
import { runSystemGitOrThrow } from './systemGit.js'

/** Inclusive attachment size limit; must match NotebookGitAttachmentSizeAdmission.LIMIT_BYTES. */
const LIMIT_BYTES = 10_485_760

function isAttachment(path: string): boolean {
  if (path.endsWith('.md')) return false
  if (path === '.keep' || path.endsWith('/.keep')) return false
  if (path === '.gitattributes' || path.endsWith('/.gitattributes')) {
    return false
  }
  return true
}

function readBlobBytes(directory: string, blobId: string): Buffer {
  const result = spawnSync(
    'git',
    ['-C', directory, 'cat-file', 'blob', blobId],
    { encoding: 'buffer' }
  )
  if (result.error) {
    throw new Error(
      `git is required but could not be run: ${exceptionText(result.error)}`
    )
  }
  if (result.status !== 0) {
    const detail = Buffer.concat([
      result.stderr ?? Buffer.alloc(0),
      result.stdout ?? Buffer.alloc(0),
    ])
      .toString('utf8')
      .trim()
    throw new Error(
      `failed to read Git blob ${blobId}${
        detail ? `: ${detail}` : ` (exit code ${result.status})`
      }`
    )
  }
  return result.stdout as Buffer
}

function attachmentBlobIds(
  directory: string,
  commitId: string
): Map<string, string> {
  const raw = runSystemGitOrThrow(
    ['-C', directory, 'ls-tree', '-r', commitId],
    (detail, status) =>
      `failed to list tree at ${commitId}${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`
  )
  const blobs = new Map<string, string>()
  for (const line of raw.split('\n')) {
    if (line === '') continue
    const tab = line.indexOf('\t')
    if (tab < 0) continue
    const meta = line.slice(0, tab)
    const filePath = line.slice(tab + 1)
    if (!isAttachment(filePath)) continue
    const parts = meta.split(' ')
    if (parts.length < 3 || parts[1] !== 'blob') continue
    blobs.set(filePath, parts[2]!)
  }
  return blobs
}

function attachmentPayloadDigestsAt(
  directory: string,
  commitId: string
): Map<string, number> {
  const digests = new Map<string, number>()
  for (const [, blobId] of attachmentBlobIds(directory, commitId)) {
    const bytes = readBlobBytes(directory, blobId)
    if (isEmptyLfsFile(bytes)) continue
    const parsed = parseLfsPointer(bytes)
    if (!parsed) {
      throw new Error(
        `Attachment at ${commitId} must be a Git LFS pointer or empty file when the notebook uses LFS.`
      )
    }
    digests.set(parsed.sha256Hex, parsed.size)
  }
  return digests
}

function attachmentPayloadDigestsInHistory(
  directory: string,
  head: string
): Set<string> {
  const digests = new Set<string>()
  const commits = runSystemGitOrThrow(
    ['-C', directory, 'rev-list', head],
    (detail, status) =>
      `failed to walk accepted history from ${head}${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`
  )
    .split('\n')
    .filter((line) => line !== '')
  for (const commitId of commits) {
    for (const digest of attachmentPayloadDigestsAt(
      directory,
      commitId
    ).keys()) {
      digests.add(digest)
    }
  }
  return digests
}

function firstParentRangeExclusiveStart(
  directory: string,
  acceptedHead: string,
  proposedHead: string
): string[] {
  if (acceptedHead === proposedHead) return []
  return runSystemGitOrThrow(
    [
      '-C',
      directory,
      'rev-list',
      '--first-parent',
      '--reverse',
      `${acceptedHead}..${proposedHead}`,
    ],
    (detail, status) =>
      `failed to list unpublished first-parent range${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`
  )
    .split('\n')
    .filter((line) => line !== '')
}

function isNewOversizedIntermediateOnly(
  claimedSize: number,
  previouslyAccepted: boolean,
  referencedAtTip: boolean
): boolean {
  return claimedSize > LIMIT_BYTES && !previouslyAccepted && !referencedAtTip
}

/**
 * Digests that must be uploaded before bundle submission. Aligns with
 * NotebookGitAttachmentSizeAdmission: omit only new oversized intermediate-only
 * payloads; previously accepted and tip-referenced digests stay required.
 */
export function selectRequiredLfsObjectIds(
  directory: string,
  acceptedHead: string,
  proposedHead: string
): string[] {
  const grandfathered = attachmentPayloadDigestsInHistory(
    directory,
    acceptedHead
  )
  const tipDigests = new Set(
    attachmentPayloadDigestsAt(directory, proposedHead).keys()
  )
  const inspectedSizes = new Map<string, number>()
  const required: string[] = []
  for (const commitId of firstParentRangeExclusiveStart(
    directory,
    acceptedHead,
    proposedHead
  )) {
    for (const [filePath, blobId] of attachmentBlobIds(directory, commitId)) {
      const bytes = readBlobBytes(directory, blobId)
      if (isEmptyLfsFile(bytes)) continue
      const parsed = parseLfsPointer(bytes)
      if (!parsed) {
        throw new Error(
          `Attachment "${filePath}" must be a Git LFS pointer or empty file when the notebook uses LFS.`
        )
      }
      const seenSize = inspectedSizes.get(parsed.sha256Hex)
      if (seenSize !== undefined) {
        if (seenSize !== parsed.size) {
          throw new Error(
            `Attachment "${filePath}" references corrupt LFS object sha256:${parsed.sha256Hex}.`
          )
        }
        continue
      }
      inspectedSizes.set(parsed.sha256Hex, parsed.size)
      if (
        isNewOversizedIntermediateOnly(
          parsed.size,
          grandfathered.has(parsed.sha256Hex),
          tipDigests.has(parsed.sha256Hex)
        )
      ) {
        continue
      }
      required.push(parsed.sha256Hex)
    }
  }
  return required
}
