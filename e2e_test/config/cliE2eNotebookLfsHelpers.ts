/**
 * Shared Git process and LFS path helpers for CLI E2E Cypress tasks.
 */

import { createHash } from 'node:crypto'
import { rmSync } from 'node:fs'
import { join } from 'node:path'
import { spawnSync } from 'node:child_process'

export function runOrThrow(
  command: string,
  args: string[],
  cwd: string,
  env?: NodeJS.ProcessEnv
): string {
  const result = spawnSync(command, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
  })
  if (result.status !== 0) {
    throw new Error(
      `${command} ${args.join(' ')} failed (status ${result.status}):\n` +
        `${result.stdout ?? ''}\n${result.stderr ?? ''}`
    )
  }
  return `${result.stdout ?? ''}${result.stderr ?? ''}`
}

export function sha256Hex(bytes: Buffer): string {
  return createHash('sha256').update(bytes).digest('hex')
}

/** Local Git LFS object path: `.git/lfs/objects/<aa>/<bb>/<oid>`. */
export function lfsObjectPath(checkoutDir: string, oid: string): string {
  return join(
    checkoutDir,
    '.git',
    'lfs',
    'objects',
    oid.slice(0, 2),
    oid.slice(2, 4),
    oid
  )
}

export function clearLfsObjectCache(checkoutDir: string): void {
  rmSync(join(checkoutDir, '.git', 'lfs', 'objects'), {
    recursive: true,
    force: true,
  })
}

/** Commit with the fixed Donut E2E author identity used by CLI notebook fixtures. */
export function commitWithE2eAuthor(
  checkoutDir: string,
  message: string
): string {
  runOrThrow(
    'git',
    [
      '-c',
      'user.name=Donut E2E',
      '-c',
      'user.email=donut-e2e@example.com',
      'commit',
      '-m',
      message,
    ],
    checkoutDir
  )
  return runOrThrow('git', ['rev-parse', 'HEAD'], checkoutDir).trim()
}
