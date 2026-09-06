import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  buildSourceRepo,
  bundleGetResponse,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import { checkoutState } from './notebookPull.testHelpers.js'

export function describeNotebookPullAcceptedHistory(): void {
  describe('notebook pull (downloaded accepted history)', () => {
    const ctx = installNotebookCliRunFixture('donut-cli-pull-history-test-')
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

    test('an already received accepted head is reported unchanged after an authenticated download', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const bundleFile = join(ctx.getWorkDir(), 'accepted.bundle')
      bundleMain(source, bundleFile)
      fetchMock.mockResolvedValue(bundleGetResponse(bundleFile))
      const before = checkoutState(directory)
      const noteBefore = fs.readFileSync(join(directory, 'note.md'), 'utf8')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await run(['notebook', 'pull', directory])

      expect(logSpy).toHaveBeenCalledWith(
        `Notebook unchanged. Accepted head: ${before.head}`
      )
      expect(fetchMock).toHaveBeenCalledOnce()
      expect(fetchMock).toHaveBeenCalledWith(
        `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
        { headers: { Authorization: 'Bearer fake-bearer' } }
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
        noteBefore
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('download permission denial fails before changing the checkout and cleans temporary storage', async () => {
      const directory = initBoundCheckout(
        ctx.getWorkDir(),
        getApiConfig().apiBaseUrl
      )
      fetchMock.mockResolvedValue({ ok: false, status: 403 })
      const before = checkoutState(directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('does not have permission')
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('a malformed accepted bundle fails before changing the checkout and cleans temporary storage', async () => {
      const directory = initBoundCheckout(
        ctx.getWorkDir(),
        getApiConfig().apiBaseUrl
      )
      fetchMock.mockResolvedValue({
        ok: true,
        arrayBuffer: () =>
          Promise.resolve(new TextEncoder().encode('not a git bundle').buffer),
      })
      const before = checkoutState(directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining(
          "failed to read the notebook's accepted history"
        )
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('a differing valid accepted head remains unavailable without changing the checkout', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      fs.writeFileSync(join(source, 'note.md'), '# accepted remote edit\n')
      runGit(['add', 'note.md'], source)
      runGit(['commit', '--quiet', '-m', 'accepted remote edit'], source)
      const bundleFile = join(ctx.getWorkDir(), 'accepted-ahead.bundle')
      bundleMain(source, bundleFile)
      fetchMock.mockResolvedValue(bundleGetResponse(bundleFile))
      const before = checkoutState(directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        'donut: Receiving a differing accepted notebook head is not available yet.'
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toContain(
        'hello notebook'
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test.each(['local-ahead', 'divergent', 'unrelated'] as const)(
      'refuses %s local history without changing the checkout or losing its tip',
      async (historyShape) => {
        const source = buildSourceRepo(ctx.getWorkDir())
        const directory =
          historyShape === 'unrelated'
            ? initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
            : cloneAsBoundCheckout(
                ctx.getWorkDir(),
                source,
                getApiConfig().apiBaseUrl,
                'checkout'
              )

        if (historyShape !== 'unrelated') {
          fs.writeFileSync(
            join(directory, 'note.md'),
            '# unpublished local edit\n'
          )
          runGit(['add', 'note.md'], directory)
          runGit(
            ['commit', '--quiet', '-m', 'unpublished local edit'],
            directory
          )
        }
        if (historyShape === 'divergent') {
          fs.writeFileSync(join(source, 'note.md'), '# accepted remote edit\n')
          runGit(['add', 'note.md'], source)
          runGit(['commit', '--quiet', '-m', 'accepted remote edit'], source)
        }

        const bundleFile = join(
          ctx.getWorkDir(),
          `accepted-${historyShape}.bundle`
        )
        bundleMain(source, bundleFile)
        fetchMock.mockResolvedValue(bundleGetResponse(bundleFile))
        const localTip = runGit(['rev-parse', 'main'], directory)
        const noteBefore = fs.readFileSync(join(directory, 'note.md'), 'utf8')
        const before = checkoutState(directory)
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
          'donut: Local main cannot receive the accepted history because it contains unpublished or unrelated commits. Publish or reconcile those commits, then try again.'
        )
        expect(fetchMock).toHaveBeenCalledOnce()
        expect(fetchMock).toHaveBeenCalledWith(
          `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
          { headers: { Authorization: 'Bearer fake-bearer' } }
        )
        expect(checkoutState(directory)).toEqual(before)
        expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
          noteBefore
        )
        expect(() =>
          runGit(['cat-file', '-e', `${localTip}^{commit}`], directory)
        ).not.toThrow()
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
