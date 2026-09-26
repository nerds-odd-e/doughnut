import * as childProcess from 'node:child_process'
import { execFileSync } from 'node:child_process'
import type { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { runGit } from './notebookClone.testHelpers.js'
import { configureTestGitIdentity } from './notebookGit.testHelpers.js'
import {
  bundleMain,
  cloneAsBoundCheckout,
  type rejectionPost,
  stubFetchForSubmission,
} from './notebookPublish.testHelpers.js'

/** Requires the calling test file to `vi.mock('node:child_process')` with a `vi.fn` spawnSync. */
export const realSpawnSync = (
  await vi.importActual<typeof import('node:child_process')>(
    'node:child_process'
  )
).spawnSync

export const LFS_ATTRIBUTES = `* filter=lfs diff=lfs merge=lfs -text
*.md !filter !diff !merge text
.gitattributes !filter !diff !merge text
.keep !filter !diff !merge text
**/.keep !filter !diff !merge text
`

export const OID_A =
  'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
export const OID_B =
  'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'

const VERSION = 'https://git-lfs.github.com/spec/v1'

export function formatLfsPointer(sha256Hex: string, size: number): Buffer {
  return Buffer.from(
    `version ${VERSION}\noid sha256:${sha256Hex}\nsize ${size}\n`,
    'ascii'
  )
}

export function hashObject(
  realSpawnSync: typeof spawnSync,
  dir: string,
  bytes: Buffer
): string {
  const result = realSpawnSync(
    'git',
    ['-C', dir, 'hash-object', '-w', '--stdin'],
    { input: bytes, encoding: 'utf8' }
  )
  if (result.status !== 0) {
    throw new Error(
      `hash-object failed: ${result.stderr ?? result.stdout ?? result.status}`
    )
  }
  return (result.stdout as string).trim()
}

export function commitPointerAttachment(
  realSpawnSync: typeof spawnSync,
  dir: string,
  relativePath: string,
  oid: string,
  size: number,
  message: string
): string {
  const blob = hashObject(realSpawnSync, dir, formatLfsPointer(oid, size))
  runGit(
    ['update-index', '--add', '--cacheinfo', `100644,${blob},${relativePath}`],
    dir
  )
  runGit(['commit', '--quiet', '-m', message], dir)
  return runGit(['rev-parse', 'main'], dir)
}

export function buildLfsSourceRepo(workDir: string): string {
  const dir = join(workDir, 'lfs-source')
  fs.mkdirSync(dir)
  runGit(['init', '--quiet', '-b', 'main'], dir)
  configureTestGitIdentity(dir)
  fs.writeFileSync(join(dir, '.gitattributes'), LFS_ATTRIBUTES)
  fs.writeFileSync(join(dir, 'note.md'), '# lfs notebook\n')
  runGit(['add', '.gitattributes', 'note.md'], dir)
  runGit(['commit', '--quiet', '-m', 'lfs attributes'], dir)
  return dir
}

export function installLfsPushIntercept(
  realSpawnSync: typeof spawnSync,
  options?: {
    failPush?: boolean
    onPush?: () => void
  }
): { pushCalls: string[][]; gitCalls: string[][] } {
  const pushCalls: string[][] = []
  const gitCalls: string[][] = []
  vi.mocked(childProcess.spawnSync).mockImplementation(((
    command: string,
    args?: readonly string[],
    spawnOptions?: Parameters<typeof spawnSync>[2]
  ) => {
    const argv = [...(args ?? [])]
    if (command === 'git') gitCalls.push(argv)
    const lfsIdx = argv.indexOf('lfs')
    if (lfsIdx >= 0 && argv[lfsIdx + 1] === 'push') {
      options?.onPush?.()
      pushCalls.push(argv)
      if (options?.failPush) {
        return {
          status: 1,
          stdout: '',
          stderr: 'upload denied',
          error: undefined,
        }
      }
      return { status: 0, stdout: '', stderr: '', error: undefined }
    }
    if (lfsIdx >= 0 && argv[lfsIdx + 1] === 'version') {
      return {
        status: 0,
        stdout: 'git-lfs/3.7.1\n',
        stderr: '',
        error: undefined,
      }
    }
    if (lfsIdx >= 0 && argv[lfsIdx + 1] === 'install') {
      return { status: 0, stdout: '', stderr: '', error: undefined }
    }
    return realSpawnSync(command, args as string[], spawnOptions)
  }) as typeof spawnSync)
  return { pushCalls, gitCalls }
}

/** An LFS object id distinct for each `n`. */
export function oid(n: number): string {
  return n.toString(16).padStart(64, '0')
}

/**
 * Appends `commits` commits to main with one `git fast-import`, each changing
 * `attachmentsPerCommit` pointer attachments (`fileN.bin`) to new objects.
 */
export function commitAttachmentHistory(
  dir: string,
  commits: number,
  attachmentsPerCommit: number
): void {
  let next = 1
  let stream = ''
  for (let c = 0; c < commits; c += 1) {
    const message = `attachments ${c}`
    stream += `commit refs/heads/main\ncommitter T <t@example.com> 0 +0000\ndata ${message.length}\n${message}\n`
    if (c === 0) stream += 'from refs/heads/main^0\n'
    for (let f = 0; f < attachmentsPerCommit; f += 1) {
      const pointer = formatLfsPointer(oid(next), next).toString('ascii')
      next += 1
      stream += `M 100644 inline file${f}.bin\ndata ${pointer.length}\n${pointer}\n`
    }
  }
  execFileSync('git', ['fast-import', '--quiet'], { cwd: dir, input: stream })
}

const ACCEPTED_HEAD = 'deadbeefcafef00ddeadbeefcafef00ddeadbeef'

export function prepareLfsPublishCheckout(
  workDir: string,
  checkoutName: string,
  postResponse:
    | { status: number; ok: boolean; text: () => Promise<string> }
    | ReturnType<typeof rejectionPost>,
  buildAcceptedHistory?: (sourceRepoDir: string) => void
): {
  dir: string
  fetchMock: ReturnType<typeof stubFetchForSubmission>
} {
  const sourceRepoDir = buildLfsSourceRepo(workDir)
  buildAcceptedHistory?.(sourceRepoDir)
  const bundleFile = join(workDir, 'accepted.bundle')
  bundleMain(sourceRepoDir, bundleFile)
  const fetchMock = stubFetchForSubmission(bundleFile, postResponse)
  const dir = cloneAsBoundCheckout(
    workDir,
    sourceRepoDir,
    getApiConfig().apiBaseUrl,
    checkoutName
  )
  return { dir, fetchMock }
}

export function stubSuccessfulAcceptedHead() {
  return {
    status: 200,
    ok: true,
    text: () => Promise.resolve(ACCEPTED_HEAD),
  }
}

export function stubOrderedLfsThenBundleFetch(
  bundleFile: string,
  callOrder: string[]
) {
  const fetchMock = vi.fn(
    (
      _url: unknown,
      init?: { method?: string }
    ): Promise<
      | { ok: boolean; arrayBuffer: () => Promise<ArrayBuffer> }
      | { status: number; ok: boolean; text: () => Promise<string> }
    > => {
      if (init?.method === 'POST') {
        callOrder.push('bundle-post')
        return Promise.resolve(stubSuccessfulAcceptedHead())
      }
      const bundleBytes = fs.readFileSync(bundleFile)
      return Promise.resolve({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(
            bundleBytes.buffer.slice(
              bundleBytes.byteOffset,
              bundleBytes.byteOffset + bundleBytes.byteLength
            )
          ),
      })
    }
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
