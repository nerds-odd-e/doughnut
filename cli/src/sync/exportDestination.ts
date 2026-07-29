import * as os from 'node:os'
import { resolve } from 'node:path'
import { isDirectory } from './isDirectory.js'
import { stripSurroundingQuotes } from './stripSurroundingQuotes.js'

const USAGE = 'Usage: /export <destination directory>'

export type ExportDestination =
  | { readonly directory: string; readonly error?: undefined }
  | { readonly error: string; readonly directory?: undefined }

/**
 * The argument comes from the Ink prompt, never a shell, so `~` is never
 * expanded for us. Only the current user's home directory is supported;
 * `~otheruser` is rejected rather than silently treated as a literal
 * directory name.
 */
function expandTilde(path: string): { path: string } | { error: string } {
  if (path === '~') return { path: os.homedir() }
  if (path.startsWith('~/')) return { path: `${os.homedir()}${path.slice(1)}` }
  if (path.startsWith('~')) {
    return {
      error: `Cannot expand ${path}: only the current user's home directory (~) is supported.`,
    }
  }
  return { path }
}

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
