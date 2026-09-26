import { spawnSync } from 'node:child_process'
import { exceptionText } from '../../exceptionText.js'
import { isEmptyLfsFile, parseLfsPointer } from './notebookLfsPointer.js'
import { runSystemGitOrThrow } from './systemGit.js'

function isAttachment(path: string): boolean {
  if (path.endsWith('.md')) return false
  if (path === '.keep' || path.endsWith('/.keep')) return false
  if (path === '.gitattributes' || path.endsWith('/.gitattributes')) {
    return false
  }
  return true
}

type ChangedFile = { status: string; dstBlob: string; path: string }

/** Path/change pairs of `git log --raw -z --format=` (or `diff-tree`) output. */
function parseRawChangesZ(output: string): ChangedFile[] {
  const parts = output.split('\0')
  const changes: ChangedFile[] = []
  for (let i = 0; i + 1 < parts.length; i += 2) {
    const [, , , dstBlob, status] = parts[i]!.slice(1).split(' ')
    changes.push({ status: status!, dstBlob: dstBlob!, path: parts[i + 1]! })
  }
  return changes
}

/** Attachment changes the first-parent unpublished commits add or modify. */
function changedAttachments(
  directory: string,
  acceptedHead: string,
  proposedHead: string
): ChangedFile[] {
  return parseRawChangesZ(
    runSystemGitOrThrow(
      [
        '-C',
        directory,
        'log',
        '--first-parent',
        '--raw',
        '-z',
        '--no-renames',
        '--no-abbrev',
        '--format=',
        `${acceptedHead}..${proposedHead}`,
      ],
      (detail, status) =>
        `failed to list unpublished first-parent changes${
          detail ? `: ${detail}` : ` (exit code ${status})`
        }`
    )
  ).filter(({ status, path }) => status !== 'D' && isAttachment(path))
}

/** Contents of `blobIds`, in order, from one `git cat-file --batch`. */
function readBlobs(directory: string, blobIds: string[]): Buffer[] {
  const result = spawnSync('git', ['-C', directory, 'cat-file', '--batch'], {
    input: `${blobIds.join('\n')}\n`,
    maxBuffer: Number.POSITIVE_INFINITY,
  })
  if (result.error) {
    throw new Error(
      `git is required but could not be run: ${exceptionText(result.error)}`
    )
  }
  if (result.status !== 0) {
    throw new Error(
      `failed to read Git blobs: ${result.stderr.toString('utf8').trim()}`
    )
  }
  const out = result.stdout
  const blobs: Buffer[] = []
  let offset = 0
  for (const _ of blobIds) {
    const headerEnd = out.indexOf(0x0a, offset)
    const size = Number(
      out.subarray(offset, headerEnd).toString('utf8').split(' ')[2]
    )
    blobs.push(out.subarray(headerEnd + 1, headerEnd + 1 + size))
    offset = headerEnd + 1 + size + 1
  }
  return blobs
}

/**
 * Digests to upload before bundle submission: the LFS objects that the
 * first-parent unpublished commits add or change.
 */
export function selectRequiredLfsObjectIds(
  directory: string,
  acceptedHead: string,
  proposedHead: string
): string[] {
  const changed = changedAttachments(directory, acceptedHead, proposedHead)
  if (changed.length === 0) return []
  const contents = readBlobs(
    directory,
    changed.map(({ dstBlob }) => dstBlob)
  )
  const sizes = new Map<string, number>()
  changed.forEach(({ path }, i) => {
    const bytes = contents[i]!
    if (isEmptyLfsFile(bytes)) return
    const pointer = parseLfsPointer(bytes)
    if (!pointer) {
      throw new Error(
        `Attachment "${path}" must be a Git LFS pointer or empty file.`
      )
    }
    const seenSize = sizes.get(pointer.sha256Hex)
    if (seenSize !== undefined && seenSize !== pointer.size) {
      throw new Error(
        `Attachment "${path}" references corrupt LFS object sha256:${pointer.sha256Hex}.`
      )
    }
    sizes.set(pointer.sha256Hex, pointer.size)
  })
  return [...sizes.keys()]
}
