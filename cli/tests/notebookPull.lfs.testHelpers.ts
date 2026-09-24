import * as childProcess from 'node:child_process'
import type { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import {
  OID_A,
  buildLfsSourceRepo,
  commitPointerAttachment,
  formatLfsPointer,
  realSpawnSync,
} from './notebookPublish.lfs.testHelpers.js'
import { cloneAsBoundCheckout } from './notebookPublish.testHelpers.js'

type SpawnOptions = Parameters<typeof spawnSync>[2]

/** Real Git for history; Git LFS itself is stubbed, and `lfs checkout` marks the current pointer filled. */
export function interceptGitLfs(options: { failFetch?: boolean } = {}) {
  const calls: { argv: string[]; env?: NodeJS.ProcessEnv }[] = []
  vi.mocked(childProcess.spawnSync).mockImplementation(((
    command: string,
    args?: readonly string[],
    spawnOptions?: SpawnOptions
  ) => {
    const argv = [...(args ?? [])]
    calls.push({ argv, env: spawnOptions?.env })
    const lfs = argv.indexOf('lfs')
    if (lfs < 0) return realSpawnSync(command, argv, spawnOptions)
    const ok = { status: 0, stdout: '', stderr: '', error: undefined }
    if (argv[lfs + 1] === 'fetch' && options.failFetch) {
      return { ...ok, status: 2, stderr: 'batch: Authentication required' }
    }
    if (argv[lfs + 1] === 'checkout') {
      const file = join(argv[1] as string, 'a.bin')
      fs.writeFileSync(file, `filled\n${fs.readFileSync(file, 'utf8')}`)
    }
    return ok
  }) as typeof spawnSync)
  return {
    lfsSteps: () =>
      calls
        .filter((c) => c.argv.includes('lfs'))
        .map((c) => c.argv[c.argv.indexOf('lfs') + 1]),
    lfsCheckoutOrder: () =>
      vi.mocked(childProcess.spawnSync).mock.invocationCallOrder[
        calls.findIndex(
          (c) => c.argv.includes('lfs') && c.argv.includes('checkout')
        )
      ],
    worktreeOpsSkipSmudge: () =>
      calls
        .filter(
          (c) =>
            !c.argv.includes('lfs') &&
            c.argv.some((a) => ['merge', 'rebase', 'reset'].includes(a))
        )
        .map((c) => c.env?.GIT_LFS_SKIP_SMUDGE),
  }
}

export function lfsCheckout(workDir: string) {
  const source = buildLfsSourceRepo(workDir)
  commitPointerAttachment(realSpawnSync, source, 'a.bin', OID_A, 4, 'image')
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  runGit(['config', 'lfs.url', 'https://stale.example.com/lfs'], directory)
  runGit(
    ['config', 'http.extraHeader', 'Authorization: Bearer stale'],
    directory
  )
  return { source, directory }
}

export const filled = (oid: string) => `filled\n${formatLfsPointer(oid, 4)}`
