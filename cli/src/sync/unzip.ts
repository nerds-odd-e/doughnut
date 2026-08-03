import { inflateRawSync } from 'node:zlib'

const END_OF_CENTRAL_DIRECTORY = 0x06054b50
const CENTRAL_FILE_HEADER = 0x02014b50
const LOCAL_FILE_HEADER = 0x04034b50

const STORED = 0
const DEFLATED = 8

const NOT_A_ZIP = 'The export was not a readable zip.'

/** Offset of the end-of-central-directory record, searched from the tail. */
function endOfCentralDirectory(zip: Buffer): number {
  for (let at = zip.length - 22; at >= 0; at--) {
    if (zip.readUInt32LE(at) === END_OF_CENTRAL_DIRECTORY) return at
  }
  throw new Error(NOT_A_ZIP)
}

function contentAt(
  zip: Buffer,
  localHeaderOffset: number,
  method: number,
  size: number
): Buffer {
  if (zip.readUInt32LE(localHeaderOffset) !== LOCAL_FILE_HEADER) {
    throw new Error(NOT_A_ZIP)
  }
  const nameLength = zip.readUInt16LE(localHeaderOffset + 26)
  const extraLength = zip.readUInt16LE(localHeaderOffset + 28)
  const from = localHeaderOffset + 30 + nameLength + extraLength
  const stored = zip.subarray(from, from + size)
  if (method === STORED) return stored
  if (method === DEFLATED) return inflateRawSync(stored)
  throw new Error(NOT_A_ZIP)
}

/**
 * File entry names in central-directory order, including duplicates. Directory
 * entries are skipped. Prefer this when duplicate / case-clash detection must
 * see every name before a Map collapse.
 */
export function listZipFileNames(zip: Buffer): string[] {
  if (zip.length < 22) throw new Error(NOT_A_ZIP)

  const end = endOfCentralDirectory(zip)
  const count = zip.readUInt16LE(end + 10)
  let at = zip.readUInt32LE(end + 16)
  const names: string[] = []

  for (let read = 0; read < count; read++) {
    if (zip.readUInt32LE(at) !== CENTRAL_FILE_HEADER) throw new Error(NOT_A_ZIP)
    const nameLength = zip.readUInt16LE(at + 28)
    const extraLength = zip.readUInt16LE(at + 30)
    const commentLength = zip.readUInt16LE(at + 32)
    const name = zip.subarray(at + 46, at + 46 + nameLength).toString('utf8')
    if (!name.endsWith('/')) names.push(name)
    at += 46 + nameLength + extraLength + commentLength
  }
  return names
}

/**
 * Read a zip into its file entries, keyed by the path recorded in the archive
 * and in the order the archive lists them. Directory entries are skipped: the
 * paths of the files describe the tree. Duplicate names collapse to the last
 * content (callers that need duplicates should use `listZipFileNames` first).
 */
export function unzipToEntries(zip: Buffer): Map<string, string> {
  if (zip.length < 22) throw new Error(NOT_A_ZIP)

  const end = endOfCentralDirectory(zip)
  const count = zip.readUInt16LE(end + 10)
  let at = zip.readUInt32LE(end + 16)

  const entries = new Map<string, string>()
  for (let read = 0; read < count; read++) {
    if (zip.readUInt32LE(at) !== CENTRAL_FILE_HEADER) throw new Error(NOT_A_ZIP)
    const method = zip.readUInt16LE(at + 10)
    const compressedSize = zip.readUInt32LE(at + 20)
    const nameLength = zip.readUInt16LE(at + 28)
    const extraLength = zip.readUInt16LE(at + 30)
    const commentLength = zip.readUInt16LE(at + 32)
    const localHeaderOffset = zip.readUInt32LE(at + 42)
    const name = zip.subarray(at + 46, at + 46 + nameLength).toString('utf8')

    if (!name.endsWith('/')) {
      entries.set(
        name,
        contentAt(zip, localHeaderOffset, method, compressedSize).toString(
          'utf8'
        )
      )
    }
    at += 46 + nameLength + extraLength + commentLength
  }
  return entries
}
