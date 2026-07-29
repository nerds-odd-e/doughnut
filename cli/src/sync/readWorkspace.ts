import { readdirSync, readFileSync } from 'node:fs'
import { join, posix, sep } from 'node:path'
import { isDirectory } from './isDirectory.js'

const MARKDOWN_SUFFIX = '.md'

/**
 * Read note content the way the export writes it, with Unix line endings.
 *
 * An editor on Windows saves CRLF, which differs from every exported line and
 * would otherwise report each line of such a note as changed on every run. A
 * note whose content really did change is written back with the line endings the
 * export uses, so a workspace saved as CRLF drifts to LF as notes change.
 */
function noteContent(path: string): string {
  return readFileSync(path, 'utf8').replace(/\r\n/g, '\n')
}

function collect(
  directory: string,
  prefix: string,
  into: Map<string, string>
): void {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) {
      collect(path, `${prefix}${entry.name}${sep}`, into)
    } else if (entry.name.endsWith(MARKDOWN_SUFFIX)) {
      into.set(
        `${prefix}${entry.name}`.split(sep).join(posix.sep),
        noteContent(path)
      )
    }
  }
}

/**
 * Read every Markdown note in a directory tree, keyed by its path relative to
 * the root and ordered by that path. Paths use forward slashes so that a
 * comparison reads the same on every platform.
 */
export function readWorkspace(root: string): Map<string, string> {
  if (!isDirectory(root)) {
    throw new Error(`No directory at ${root}.`)
  }
  const collected = new Map<string, string>()
  collect(root, '', collected)
  return new Map(
    [...collected].sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
  )
}
