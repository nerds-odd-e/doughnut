import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test, vi } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import {
  ProcessExitForTest,
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  startPausedSameLineRebase,
  unmergedOperationObservation,
} from './notebookPull.testHelpers.js'
import {
  localGitObservation,
  stubFetchWithAcceptedBundleFrom,
} from './notebookPublish.testHelpers.js'

export function describeNotebookPublishReadiness(): void {
  describe('notebook publish (CLI routing, local readiness checks)', () => {
    const ctx = installNotebookCliRunFixture(
      'donut-cli-publish-readiness-test-'
    )

    test('detached HEAD is rejected with an actionable readiness error', async () => {
      const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
      const sha = runGit(['rev-parse', 'HEAD'], dir)
      runGit(['checkout', '--quiet', sha], dir)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('detached HEAD')
      )
      expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    })

    test('HEAD on a branch other than main is rejected with an actionable readiness error', async () => {
      const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
      runGit(['checkout', '--quiet', '-b', 'feature'], dir)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining('feature')
      )
      expect(ctx.getExitSpy()).toHaveBeenCalledWith(1)
    })

    test.each(['staged', 'unstaged', 'untracked'] as const)(
      '%s work warns, publishes committed main, and preserves local Git state',
      async (kind) => {
        const dir = initBoundCheckout(
          ctx.getWorkDir(),
          getApiConfig().apiBaseUrl
        )
        if (kind === 'untracked') {
          fs.writeFileSync(join(dir, 'untracked.md'), '# new note\n')
        } else {
          fs.writeFileSync(
            join(dir, 'note.md'),
            `# hello notebook (${kind} edit)\n`
          )
          if (kind === 'staged') runGit(['add', 'note.md'], dir)
        }
        const before = localGitObservation(dir)
        const fetchMock = stubFetchWithAcceptedBundleFrom(dir, ctx.getWorkDir())

        await run(['notebook', 'publish', dir])

        expect(ctx.getErrorSpy()).toHaveBeenCalledOnce()
        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
          `donut: warning: ${dir} has uncommitted changes (1 file not clean, including untracked files) — publishing committed main; local changes are not included.`
        )
        expect(fetchMock).toHaveBeenCalledWith(
          expect.anything(),
          expect.objectContaining({ method: 'POST' })
        )
        expect(localGitObservation(dir)).toEqual(before)
      }
    )

    test('an unfinished rebase is rejected without submitting or changing the unmerged checkout', async () => {
      const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
      startPausedSameLineRebase(dir)
      const before = unmergedOperationObservation(dir)
      expect(before.unmerged).not.toBe('')
      expect(before.rebaseMerge).toBe(true)
      const fetchMock = vi.fn()
      vi.stubGlobal('fetch', fetchMock)

      await expect(run(['notebook', 'publish', dir])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        expect.stringContaining(
          'Finish or abort the active Git operation before publishing'
        )
      )
      expect(unmergedOperationObservation(dir)).toEqual(before)
      expect(fetchMock).not.toHaveBeenCalled()
    })

    test('a clean checkout on main reaches submission', async () => {
      const dir = initBoundCheckout(ctx.getWorkDir(), getApiConfig().apiBaseUrl)
      const fetchMock = stubFetchWithAcceptedBundleFrom(dir, ctx.getWorkDir())

      await run(['notebook', 'publish', dir])
      expect(fetchMock).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ method: 'POST' })
      )
    })
  })
}
