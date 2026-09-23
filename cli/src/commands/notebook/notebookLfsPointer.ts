/**
 * Standard Git LFS v1 pointer classification for local publish selection.
 * Matches the server's NotebookGitLfsPointer contract for empty files and canonical pointers.
 */

const MAX_POINTER_BYTES = 1024
const POINTER =
  /^version https:\/\/git-lfs\.github\.com\/spec\/v1\noid sha256:([0-9a-f]{64})\nsize (0|[1-9][0-9]*)\n$/

export type ParsedLfsPointer = { sha256Hex: string; size: number }

export function isEmptyLfsFile(bytes: Buffer): boolean {
  return bytes.length === 0
}

export function parseLfsPointer(bytes: Buffer): ParsedLfsPointer | undefined {
  if (bytes.length === 0 || bytes.length >= MAX_POINTER_BYTES) {
    return undefined
  }
  for (const b of bytes) {
    if ((b < 0x20 && b !== 0x0a) || b > 0x7e) {
      return undefined
    }
  }
  const text = bytes.toString('ascii')
  const match = POINTER.exec(text)
  if (!match) {
    return undefined
  }
  return { sha256Hex: match[1]!, size: Number(match[2]) }
}
