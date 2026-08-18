import { inflateRawSync } from 'node:zlib'
import { readFileSync } from 'node:fs'

const CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50
const END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50

/** Reads a ZIP from disk into path → UTF-8 content, via the central directory. */
export function readZipEntries(filePath: string): Record<string, string> {
  const buf = readFileSync(filePath)
  const eocd = findEndOfCentralDirectory(buf)
  const entryCount = buf.readUInt16LE(eocd + 10)
  let offset = buf.readUInt32LE(eocd + 16)
  const entries: Record<string, string> = {}
  for (let i = 0; i < entryCount; i++) {
    if (buf.readUInt32LE(offset) !== CENTRAL_DIRECTORY_SIGNATURE) {
      throw new Error(
        `Invalid ZIP central directory at offset ${offset} in ${filePath}`
      )
    }
    const method = buf.readUInt16LE(offset + 10)
    const compressedSize = buf.readUInt32LE(offset + 20)
    const nameLen = buf.readUInt16LE(offset + 28)
    const extraLen = buf.readUInt16LE(offset + 30)
    const commentLen = buf.readUInt16LE(offset + 32)
    const localHeaderOffset = buf.readUInt32LE(offset + 42)
    const name = buf
      .subarray(offset + 46, offset + 46 + nameLen)
      .toString('utf8')
    const localNameLen = buf.readUInt16LE(localHeaderOffset + 26)
    const localExtraLen = buf.readUInt16LE(localHeaderOffset + 28)
    const dataStart = localHeaderOffset + 30 + localNameLen + localExtraLen
    const compressed = buf.subarray(dataStart, dataStart + compressedSize)
    entries[name] = inflateZipEntry(method, compressed, name).toString('utf8')
    offset += 46 + nameLen + extraLen + commentLen
  }
  return entries
}

function inflateZipEntry(
  method: number,
  compressed: Buffer,
  name: string
): Buffer {
  if (method === 0) {
    return compressed
  }
  if (method === 8) {
    return inflateRawSync(compressed)
  }
  throw new Error(`Unsupported ZIP compression method ${method} for ${name}`)
}

function findEndOfCentralDirectory(buf: Buffer): number {
  const minEocd = 22
  const maxComment = 65535
  const start = buf.length - minEocd
  const stop = Math.max(0, buf.length - minEocd - maxComment)
  for (let i = start; i >= stop; i--) {
    if (buf.readUInt32LE(i) !== END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
      continue
    }
    const commentLen = buf.readUInt16LE(i + 20)
    if (i + minEocd + commentLen === buf.length) {
      return i
    }
  }
  throw new Error('ZIP end of central directory not found')
}
