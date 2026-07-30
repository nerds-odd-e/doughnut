import { resolve } from 'node:path'
import { expandTilde } from './expandTilde.js'
import { isDirectory } from './isDirectory.js'
import { stripSurroundingQuotes } from './stripSurroundingQuotes.js'

export type DirectoryArgument =
  | { readonly directory: string; readonly error?: undefined }
  | { readonly error: string; readonly directory?: undefined }

/**
 * A directory as the user typed it at the prompt: quoted the way a shell would
 * accept, `~` unexpanded, relative to wherever the process was started. The
 * directory has to be there already — a command reading or writing one has no
 * business creating a directory for a typo.
 */
export function parseDirectoryArgument(argument: string): DirectoryArgument {
  const expanded = expandTilde(stripSurroundingQuotes(argument.trim()))
  if ('error' in expanded) return expanded

  const directory = resolve(process.cwd(), expanded.path)
  return isDirectory(directory)
    ? { directory }
    : { error: `No directory at ${directory}.` }
}
