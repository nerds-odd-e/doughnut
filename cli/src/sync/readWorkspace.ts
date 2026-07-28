import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, posix, sep } from 'node:path'

const MARKDOWN_SUFFIX = '.md'

function isDirectory(path: string): boolean {
  try {
    return statSync(path).isDirectory()
  } catch {
    return false
  }
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
        readFileSync(path, 'utf8')
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
