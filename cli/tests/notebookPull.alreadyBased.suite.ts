import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  buildSourceRepo,
  cloneAsBoundCheckout,
} from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
} from './notebookPull.testHelpers.js'
import { serveAcceptedBundle } from './notebookPull.historySafety.testHelpers.js'
import {
  LATER_OTHER_NOTE,
  LOCAL_NOTE,
  prepareEligibleDivergence,
} from './notebookPull.rebase.testHelpers.js'

export function describeNotebookPullAlreadyBased(): void {
  describe('notebook pull (already-based unpublished commit)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-already-based-test-'
    )

    test('reports an eligible local-ahead commit as already based without rewriting it', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], source)

      fs.writeFileSync(join(directory, 'note.md'), LOCAL_NOTE)
      runGit(['add', 'note.md'], directory)
      runGit(['commit', '--quiet', '-m', 'unpublished local edit'], directory)

      serveAcceptedBundle(ctx, source, 'local-ahead')
      const localTip = runGit(['rev-parse', 'main'], directory)
      const before = checkoutState(directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await run(['notebook', 'pull', directory])

      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Unpublished local commit is already based on the accepted history. Local head: ${localTip}. Accepted head: ${acceptedHead}. Inspect the result, then run "donut notebook publish ${directory}".`
      )
      expect(ctx.getFetchMock()).toHaveBeenCalledOnce()
      expect(ctx.getFetchMock()).toHaveBeenCalledWith(
        `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
        { headers: { Authorization: 'Bearer fake-bearer' } }
      )
      expect(checkoutState(directory)).toEqual(before)
      expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
        LOCAL_NOTE
      )
      expect(() =>
        runGit(['cat-file', '-e', `${localTip}^{commit}`], directory)
      ).not.toThrow()
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('repeat pull leaves the rebased unpublished commit unchanged', async () => {
      const setup = prepareEligibleDivergence(ctx.getWorkDir(), {
        remoteEdits: 1,
      })
      serveAcceptedBundle(ctx, setup.source, 'rebase-repeat')

      await run(['notebook', 'pull', setup.directory])
      const rebasedHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      const noteAfterRebase = fs.readFileSync(
        join(setup.directory, 'note.md'),
        'utf8'
      )
      const otherAfterRebase = fs.readFileSync(
        join(setup.directory, 'other.md'),
        'utf8'
      )

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(rebasedHead)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        noteAfterRebase
      )
      expect(fs.readFileSync(join(setup.directory, 'other.md'), 'utf8')).toBe(
        otherAfterRebase
      )
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Unpublished local commit is already based on the accepted history. Local head: ${rebasedHead}. Accepted head: ${setup.acceptedHead}. Inspect the result, then run "donut notebook publish ${setup.directory}".`
      )
      expect(ctx.getFetchMock().mock.calls).toEqual([
        [
          `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
          { headers: { Authorization: 'Bearer fake-bearer' } },
        ],
        [
          `${getApiConfig().apiBaseUrl}/api/notebooks/42/git-bundle`,
          { headers: { Authorization: 'Bearer fake-bearer' } },
        ],
      ])
    })

    test('later eligible other-note advance rebases the same local patch once more', async () => {
      const setup = prepareEligibleDivergence(ctx.getWorkDir(), {
        remoteEdits: 1,
      })
      serveAcceptedBundle(ctx, setup.source, 'rebase-later-first')
      await run(['notebook', 'pull', setup.directory])

      fs.writeFileSync(join(setup.source, 'other.md'), LATER_OTHER_NOTE)
      runGit(['add', 'other.md'], setup.source)
      runGit(
        ['commit', '--quiet', '-m', 'later accepted other-note edit'],
        setup.source
      )
      const laterAccepted = runGit(['rev-parse', 'main'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'rebase-later-second')

      await run(['notebook', 'pull', setup.directory])

      const laterLocalHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        laterAccepted
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_NOTE
      )
      expect(fs.readFileSync(join(setup.directory, 'other.md'), 'utf8')).toBe(
        LATER_OTHER_NOTE
      )
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased the unpublished local commit onto the accepted history. Local head: ${laterLocalHead}. Accepted head: ${laterAccepted}. Inspect the result, then run "donut notebook publish ${setup.directory}".`
      )
    })
  })
}
