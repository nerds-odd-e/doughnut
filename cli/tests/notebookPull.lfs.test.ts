import * as childProcess from 'node:child_process'
import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { continuePausedRebaseWithChosenBytes } from './notebookPull.conflict.testHelpers.js'
import {
  OID_A,
  OID_B,
  buildLfsSourceRepo,
  commitPointerAttachment,
  realSpawnSync,
} from './notebookPublish.lfs.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import {
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  filled,
  interceptGitLfs,
  lfsCheckout,
} from './notebookPull.lfs.testHelpers.js'

vi.mock('node:child_process', async () => {
  const actual =
    await vi.importActual<typeof import('node:child_process')>(
      'node:child_process'
    )
  return { ...actual, spawnSync: vi.fn(actual.spawnSync) }
})

const notebookLfsUrl = () => `${getApiConfig().apiBaseUrl}/api/notebooks/42/lfs`

describe('notebook pull (LFS checkout fill-in)', () => {
  const ctx = installNotebookPullAcceptedHistoryTest('donut-cli-pull-lfs-test-')

  beforeEach(() => {
    vi.stubEnv('GIT_CONFIG_GLOBAL', '/dev/null')
    vi.stubEnv('GIT_CONFIG_NOSYSTEM', '1')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.mocked(childProcess.spawnSync).mockReset()
  })

  test.each([
    {
      outcome: 'unchanged',
      web: [],
      local: false,
      report: 'Notebook unchanged',
    },
    { outcome: 'already based', web: [], local: true, report: 'already based' },
    {
      outcome: 'fast-forward',
      web: ['a.bin'],
      local: false,
      report: 'Received accepted',
    },
    {
      outcome: 'rebased',
      web: ['other.md'],
      local: true,
      report: 'Rebased onto',
    },
    {
      outcome: 'conflict pause',
      web: ['note.md'],
      local: true,
      report: 'Git paused a rebase with a conflict in "note.md"',
    },
  ])(
    'fills in current files after $outcome with the refreshed login',
    async ({ web, local, report }) => {
      const { source, directory } = lfsCheckout(ctx.getWorkDir())
      if (web.includes('a.bin')) {
        commitPointerAttachment(realSpawnSync, source, 'a.bin', OID_B, 4, 'web')
      }
      for (const note of web.filter((path) => path.endsWith('.md'))) {
        commitPortableFile(source, note, '# web\n', 'web note')
      }
      if (local) commitPortableFile(directory, 'note.md', '# local\n', 'local')
      serveAcceptedBundle(ctx, source, 'lfs')
      const lfs = interceptGitLfs()
      const paused = web.includes('note.md')

      if (paused) {
        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )
      } else {
        await run(['notebook', 'pull', directory])
      }

      const acceptedHead = runGit(['rev-parse', 'main'], source)
      expect(
        runGit(['merge-base', '--is-ancestor', acceptedHead, 'HEAD'], directory)
      ).toBe('')
      expect(fs.readFileSync(join(directory, 'a.bin'), 'utf8')).toBe(
        filled(web.includes('a.bin') ? OID_B : OID_A)
      )
      expect(lfs.lfsSteps()).toEqual(['install', 'pull'])
      expect(runGit(['remote'], directory)).toBe('')
      expect(lfs.worktreeOpsSkipSmudge().every((v) => v === '1')).toBe(true)
      expect(lfs.worktreeOpsSkipSmudge().length > 0).toBe(web.length > 0)
      expect(runGit(['config', 'lfs.url'], directory)).toBe(notebookLfsUrl())
      expect(runGit(['config', 'http.extraHeader'], directory)).toBe(
        'Authorization: Bearer fake-bearer'
      )
      const reportSpy = paused ? ctx.getErrorSpy() : ctx.getLogSpy()
      expect(reportSpy).toHaveBeenCalledWith(expect.stringContaining(report))
      if (paused) {
        expect(runGit(['ls-files', '-u'], directory)).toContain('note.md')
        expect(reportSpy.mock.invocationCallOrder[0]).toBeGreaterThan(
          lfs.lfsPullOrder() as number
        )
      }
    }
  )

  test('a configured checkout only refreshes a rotated login before filling in', async () => {
    const { source, directory } = lfsCheckout(ctx.getWorkDir())
    for (const [key, value] of [
      ['lfs.url', notebookLfsUrl()],
      ['filter.lfs.process', 'git-lfs filter-process'],
    ]) {
      runGit(['config', key, value], directory)
    }
    serveAcceptedBundle(ctx, source, 'lfs')
    const lfs = interceptGitLfs()

    await run(['notebook', 'pull', directory])

    expect(lfs.lfsSteps()).toEqual(['pull'])
    expect(runGit(['config', 'http.extraHeader'], directory)).toBe(
      'Authorization: Bearer fake-bearer'
    )
    expect(fs.readFileSync(join(directory, 'a.bin'), 'utf8')).toBe(
      filled(OID_A)
    )
  })

  test.each([
    {
      checkout: 'markdown',
      buildSource: buildLfsSourceRepo,
      lfsSteps: ['install'],
    },
    { checkout: 'legacy', buildSource: buildSourceRepo, lfsSteps: [] },
  ] as const)(
    'a $checkout checkout pulls without a Git LFS fill-in',
    async ({ checkout, buildSource, lfsSteps }) => {
      const source = buildSource(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      commitPortableFile(source, 'physics/.keep', '', 'web folder')
      serveAcceptedBundle(ctx, source, checkout)
      const lfs = interceptGitLfs()

      await run(['notebook', 'pull', directory])

      expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(
        runGit(['rev-parse', 'main'], source)
      )
      expect(lfs.lfsSteps()).toEqual(lfsSteps)
    }
  )

  test('a failed download reports incomplete attachments; the rerun fills them in, then reports unchanged', async () => {
    const { source, directory } = lfsCheckout(ctx.getWorkDir())
    commitPointerAttachment(realSpawnSync, source, 'a.bin', OID_B, 4, 'web')
    serveAcceptedBundle(ctx, source, 'lfs')
    interceptGitLfs({ failPull: true })

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringMatching(
        /^donut: Notebook attachments are incomplete: .*rerun "donut notebook pull"\.$/s
      )
    )
    expect(ctx.getLogSpy()).not.toHaveBeenCalled()
    const acceptedHead = runGit(['rev-parse', 'main'], source)
    expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)

    interceptGitLfs()
    await run(['notebook', 'pull', directory])

    expect(fs.readFileSync(join(directory, 'a.bin'), 'utf8')).toBe(
      filled(OID_B)
    )
    expect(ctx.getLogSpy()).toHaveBeenCalledWith(
      `Notebook unchanged. Accepted head: ${acceptedHead}`
    )
  })

  test('a failed download during a conflict pause names finishing the rebase before the rerun, which then fills in', async () => {
    const { source, directory } = lfsCheckout(ctx.getWorkDir())
    commitPortableFile(source, 'note.md', '# web\n', 'web note')
    commitPortableFile(directory, 'note.md', '# local\n', 'local')
    serveAcceptedBundle(ctx, source, 'lfs')
    interceptGitLfs({ failPull: true })

    await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
      ProcessExitForTest
    )

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
      expect.stringMatching(
        /^donut: Git paused a rebase with a conflict in "note\.md"\..*git add -- 'note\.md'.*git rebase --continue.*git rebase --abort.*\nNotebook attachments are incomplete: .*finish \("git rebase --continue"\) or abort \("git rebase --abort"\) the rebase, then rerun "donut notebook pull"\.$/s
      )
    )

    continuePausedRebaseWithChosenBytes(directory, {
      path: 'note.md',
      content: '# chosen\n',
    })
    interceptGitLfs()
    await run(['notebook', 'pull', directory])

    expect(fs.readFileSync(join(directory, 'a.bin'), 'utf8')).toBe(
      filled(OID_A)
    )
    expect(ctx.getLogSpy()).toHaveBeenCalledWith(
      expect.stringContaining(
        'Unpublished local work is already based on the accepted history.'
      )
    )
  })
})
