import * as os from 'node:os'

/**
 * The argument comes from the Ink prompt, never a shell, so `~` is never
 * expanded for us. Only the current user's home directory is supported;
 * `~otheruser` is rejected rather than silently treated as a literal
 * directory name.
 */
export function expandTilde(
  path: string
): { path: string } | { error: string } {
  if (path === '~') return { path: os.homedir() }
  if (path.startsWith('~/')) return { path: `${os.homedir()}${path.slice(1)}` }
  if (path.startsWith('~')) {
    return {
      error: `Cannot expand ${path}: only the current user's home directory (~) is supported.`,
    }
  }
  return { path }
}
