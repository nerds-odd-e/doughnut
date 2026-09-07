import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import { dirname, join } from 'node:path'
import { afterEach, beforeEach, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import {
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  bundleGetResponse,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'

export const GIT_BUNDLE_GET = [
  `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
  { headers: { Authorization: 'Bearer fake-bearer' } },
] as const

export function installNotebookPullAcceptedHistoryTest(workDirPrefix: string) {
  const base = installNotebookCliRunFixture(workDirPrefix)
  let fetchMock: ReturnType<typeof vi.fn>
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    logSpy.mockRestore()
  })

  return {
    ...base,
    getFetchMock: () => fetchMock,
    getLogSpy: () => logSpy,
  }
}

export function checkoutState(directory: string) {
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    branch: runGit(['rev-parse', '--abbrev-ref', 'HEAD'], directory),
    refs: runGit(
      ['for-each-ref', '--format=%(refname) %(objectname)'],
      directory
    ),
    indexTree: runGit(['write-tree'], directory),
    status: runGit(['status', '--porcelain=v1'], directory),
    staged: runGit(['diff', '--cached'], directory),
    unstaged: runGit(['diff'], directory),
    notebookId: runGit(
      ['config', '--local', '--get', 'donut.notebook-id'],
      directory
    ),
    apiOrigin: runGit(
      ['config', '--local', '--get', 'donut.api-origin'],
      directory
    ),
  }
}

/** Captures a conflicted rebase without `write-tree`, which fails on an unmerged index. */
export function unmergedOperationObservation(directory: string) {
  const rebaseMergePath = runGit(
    ['rev-parse', '--git-path', 'rebase-merge'],
    directory
  )
  return {
    head: runGit(['rev-parse', 'HEAD'], directory),
    main: runGit(['rev-parse', 'main'], directory),
    unmerged: runGit(['ls-files', '-u'], directory),
    note: fs.readFileSync(join(directory, 'note.md'), 'utf8'),
    rebaseMerge: fs.existsSync(join(directory, rebaseMergePath)),
  }
}

/** Two same-line edits, then `git rebase` left paused with unmerged stages and rebase-merge. */
export function startPausedSameLineRebase(directory: string): void {
  fs.writeFileSync(join(directory, 'note.md'), '# local same-line edit\n')
  runGit(['add', 'note.md'], directory)
  runGit(['commit', '--quiet', '-m', 'local same-line edit'], directory)
  runGit(['checkout', '--quiet', '-b', 'accepted', 'HEAD~1'], directory)
  fs.writeFileSync(join(directory, 'note.md'), '# accepted same-line edit\n')
  runGit(['add', 'note.md'], directory)
  runGit(['commit', '--quiet', '-m', 'accepted same-line edit'], directory)
  runGit(['checkout', '--quiet', 'main'], directory)
  const rebase = spawnSync('git', ['rebase', 'accepted'], {
    cwd: directory,
    encoding: 'utf8',
  })
  if (rebase.status === 0) {
    throw new Error('expected git rebase to pause with unmerged stages')
  }
}

export function commitPortableFile(
  directory: string,
  relativePath: string,
  bytes: string,
  message: string
): void {
  const absolutePath = join(directory, relativePath)
  fs.mkdirSync(dirname(absolutePath), { recursive: true })
  fs.writeFileSync(absolutePath, bytes)
  runGit(['add', relativePath], directory)
  runGit(['commit', '--quiet', '-m', message], directory)
}

export function serveAcceptedBundle(
  ctx: ReturnType<typeof installNotebookPullAcceptedHistoryTest>,
  source: string,
  name: string
): void {
  const bundleFile = join(ctx.getWorkDir(), `accepted-${name}.bundle`)
  bundleMain(source, bundleFile)
  ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))
}

export function cloneWithLocalNoteEdit(
  workDir: string,
  baseBytes: string,
  localBytes: string,
  relativePath = 'note.md'
): {
  directory: string
  source: string
  localTip: string
} {
  const source = buildSourceRepo(workDir)
  commitPortableFile(source, relativePath, baseBytes, 'portable shared base')
  const directory = cloneAsBoundCheckout(
    workDir,
    source,
    getApiConfig().apiBaseUrl,
    'checkout'
  )
  commitPortableFile(
    directory,
    relativePath,
    localBytes,
    'unpublished note edit'
  )
  return {
    directory,
    source,
    localTip: runGit(['rev-parse', 'HEAD'], directory),
  }
}
