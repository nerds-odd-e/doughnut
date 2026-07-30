import { resolve } from 'node:path'
import { expandTilde } from './expandTilde.js'
import { isDirectory } from './isDirectory.js'
import { stripSurroundingQuotes } from './stripSurroundingQuotes.js'

const USAGE = 'Usage: /export <destination directory>'

export type ExportDestination =
  | { readonly directory: string; readonly error?: undefined }
  | { readonly error: string; readonly directory?: undefined }

/**
 * The destination must already exist: `/export` writes a notebook into it, it
 * does not go looking for a typo to create a directory for.
 */
export function parseExportDestination(
  argument: string | undefined
): ExportDestination {
  const trimmed = (argument ?? '').trim()
  if (trimmed === '') return { error: USAGE }

  const expanded = expandTilde(stripSurroundingQuotes(trimmed))
  if ('error' in expanded) return expanded

  const directory = resolve(process.cwd(), expanded.path)
  if (!isDirectory(directory)) {
    return { error: `No directory at ${directory}.` }
  }
  return { directory }
}
