import {
  type DirectoryArgument,
  parseDirectoryArgument,
} from './directoryArgument.js'

const USAGE = 'Usage: /export <destination directory>'

export function parseExportDestination(
  argument: string | undefined
): DirectoryArgument {
  const trimmed = (argument ?? '').trim()
  if (trimmed === '') return { error: USAGE }
  return parseDirectoryArgument(trimmed)
}
