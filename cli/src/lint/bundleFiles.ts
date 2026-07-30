import { readdirSync } from 'node:fs'
import { join, posix, sep } from 'node:path'

const MARKDOWN_SUFFIX = '.md'

/**
 * A dot folder holds what its tooling owns rather than what an author wrote, so
 * /lint reads nothing in one. The walk below skips them on the way down; this
 * says the same of a path already collected elsewhere.
 */
export function inDotFolder(path: string): boolean {
  return path
    .split(posix.sep)
    .slice(0, -1)
    .some((folder) => folder.startsWith('.'))
}

function collect(directory: string, prefix: string, into: string[]): void {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (entry.name.startsWith('.')) continue
    const path = `${prefix}${entry.name}`
    if (entry.isDirectory()) {
      collect(join(directory, entry.name), `${path}${sep}`, into)
    } else if (!entry.name.endsWith(MARKDOWN_SUFFIX)) {
      into.push(path.split(sep).join(posix.sep))
    }
  }
}

/** Files in the bundle that OKF has no rules for, ordered by path. */
export function nonMarkdownPaths(root: string): string[] {
  const found: string[] = []
  collect(root, '', found)
  return found.sort()
}
