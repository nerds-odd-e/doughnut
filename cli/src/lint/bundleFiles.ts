import { readdirSync } from 'node:fs'
import { join, posix, sep } from 'node:path'

const MARKDOWN_SUFFIX = '.md'

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

/**
 * Files in the bundle that OKF has no rules for, ordered by path. Dot folders
 * hold what their tooling owns, so nothing in one is a file the author wrote.
 */
export function nonMarkdownPaths(root: string): string[] {
  const found: string[] = []
  collect(root, '', found)
  return found.sort()
}
