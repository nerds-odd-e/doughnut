import { spawnSync } from 'node:child_process'
import { exceptionText } from '../../exceptionText.js'

/**
 * Runs the system `git` executable with `args`, returning stdout, or throwing
 * `describeFailure`'s message (given the trimmed stderr, when any, and the exit code) if it
 * exits non-zero, or a "git is required" error if the executable itself could not be spawned.
 */
export function runSystemGitOrThrow(
  args: readonly string[],
  describeFailure: (detail: string | undefined, status: number | null) => string
): string {
  const result = spawnSync('git', args, { encoding: 'utf8' })
  if (result.error) {
    throw new Error(
      `git is required but could not be run: ${exceptionText(result.error)}`
    )
  }
  if (result.status !== 0) {
    throw new Error(describeFailure(result.stderr?.trim(), result.status))
  }
  return result.stdout
}
