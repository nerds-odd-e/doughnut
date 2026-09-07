import { spawnSync, type SpawnSyncOptions } from 'node:child_process'
import { exceptionText } from '../../exceptionText.js'

function gitFailureDetail(stdout: string, stderr: string): string | undefined {
  const streams = [stderr, stdout]
    .map((stream) => stream.trim())
    .filter((stream) => stream !== '')
  if (streams.length === 0) return undefined
  return [...new Set(streams)].join('\n')
}

/**
 * Runs the system `git` executable with `args`, returning stdout, or throwing
 * `describeFailure`'s message (given the trimmed stderr and stdout, when any, and the exit
 * code) if it exits non-zero, or a "git is required" error if the executable itself could not
 * be spawned.
 */
export function runSystemGitOrThrow(
  args: readonly string[],
  describeFailure: (
    detail: string | undefined,
    status: number | null
  ) => string,
  options?: Pick<SpawnSyncOptions, 'env'>
): string {
  const result = spawnSync('git', args, { encoding: 'utf8', ...options })
  if (result.error) {
    throw new Error(
      `git is required but could not be run: ${exceptionText(result.error)}`
    )
  }
  if (result.status !== 0) {
    throw new Error(
      describeFailure(
        gitFailureDetail(result.stdout ?? '', result.stderr ?? ''),
        result.status
      )
    )
  }
  return result.stdout
}
