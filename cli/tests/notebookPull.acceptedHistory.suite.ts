import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest } from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  buildSourceRepo,
  bundleGetResponse,
  bundleMain,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
} from './notebookPull.testHelpers.js'
import { describeNotebookPullFastForward } from './notebookPull.fastForward.suite.js'
import { describeNotebookPullHistorySafety } from './notebookPull.historySafety.suite.js'

export function describeNotebookPullAcceptedHistory(): void {
  describe('notebook pull (downloaded accepted history)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-history-test-'
    )

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
      ctx.getFetchMock().mockResolvedValue(bundleGetResponse(bundleFile))
      const before = checkoutState(directory)
      const noteBefore = fs.readFileSync(join(directory, 'note.md'), 'utf8')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await run(['notebook', 'pull', directory])

      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Notebook unchanged. Accepted head: ${before.head}`
      )
      expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
      expect(ctx.getFetchMock()).toHaveBeenCalledWith(
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
      ctx.getFetchMock().mockResolvedValue({ ok: false, status: 403 })
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
      ctx.getFetchMock().mockResolvedValue({
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
  })

  describeNotebookPullFastForward()
  describeNotebookPullHistorySafety()
}
