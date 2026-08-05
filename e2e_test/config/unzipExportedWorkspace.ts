/**
 * Read a notebook export zip into its file entries.
 *
 * Kept under `e2e_test` so scenarios can assert zip contents without importing
 * product code. The web catalog export path is the producer under test.
 */
import { inflateRawSync } from 'node:zlib'

const END_OF_CENTRAL_DIRECTORY = 0x06054b50
const CENTRAL_FILE_HEADER = 0x02014b50

const STORED = 0
const DEFLATED = 8

const NOT_A_ZIP = 'The notebook export was not a readable zip.'

function endOfCentralDirectory(zip: Buffer): number {
  for (let at = zip.length - 22; at >= 0; at--) {
    if (zip.readUInt32LE(at) === END_OF_CENTRAL_DIRECTORY) return at
  }
  throw new Error(NOT_A_ZIP)
}

export function unzipExportedWorkspace(zip: Buffer): Map<string, string> {
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
      const localNameLength = zip.readUInt16LE(localHeaderOffset + 26)
      const localExtraLength = zip.readUInt16LE(localHeaderOffset + 28)
      const from = localHeaderOffset + 30 + localNameLength + localExtraLength
      const raw = zip.subarray(from, from + compressedSize)
      if (method !== STORED && method !== DEFLATED) throw new Error(NOT_A_ZIP)
      entries.set(
        name,
        (method === STORED ? raw : inflateRawSync(raw)).toString('utf8')
      )
    }
    at += 46 + nameLength + extraLength + commentLength
  }
  return entries
}
