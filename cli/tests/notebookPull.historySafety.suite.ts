import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { getApiConfig } from 'donut-api'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
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

const RECEIVE_GATE_ERROR =
  'Local main cannot receive the accepted history because it contains unpublished or unrelated commits. Publish or reconcile those commits, then try again.'

export function describeNotebookPullHistorySafety(): void {
  describe('notebook pull (local history safety)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-history-safety-test-'
    )

    test('refuses local-ahead history without changing the checkout or losing its tip', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )

      fs.writeFileSync(join(directory, 'note.md'), '# unpublished local edit\n')
      runGit(['add', 'note.md'], directory)
      runGit(['commit', '--quiet', '-m', 'unpublished local edit'], directory)

      serveAcceptedBundle(ctx, source, 'local-ahead')
      const localTip = runGit(['rev-parse', 'main'], directory)
      const noteBefore = fs.readFileSync(join(directory, 'note.md'), 'utf8')
      const before = checkoutState(directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${RECEIVE_GATE_ERROR}`
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
      expect(() =>
        runGit(['cat-file', '-e', `${localTip}^{commit}`], directory)
      ).not.toThrow()
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })

    test('refuses eligible other-note divergence with the receive-gate error and an unchanged checkout', async () => {
      const source = buildSourceRepo(ctx.getWorkDir())
      fs.writeFileSync(
        join(source, 'other.md'),
        '---\ntype: Note\n---\n# Other\n\nAccepted body.\n'
      )
      runGit(['add', 'other.md'], source)
      runGit(['commit', '--quiet', '-m', 'add other note'], source)
      const directory = cloneAsBoundCheckout(
        ctx.getWorkDir(),
        source,
        getApiConfig().apiBaseUrl,
        'checkout'
      )
      fs.writeFileSync(
        join(directory, 'note.md'),
        '---\ntype: Note\n---\n# Local edit\n\nUnpublished body.\n'
      )
      runGit(['add', 'note.md'], directory)
      runGit(['commit', '--quiet', '-m', 'unpublished note edit'], directory)
      fs.writeFileSync(
        join(source, 'other.md'),
        '---\ntype: Note\n---\n# Other\n\nAccepted edit.\n'
      )
      runGit(['add', 'other.md'], source)
      runGit(['commit', '--quiet', '-m', 'accepted other-note edit'], source)
      serveAcceptedBundle(ctx, source, 'eligible-divergence')
      const before = checkoutState(directory)

      await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        `donut: ${RECEIVE_GATE_ERROR}`
      )
      expect(checkoutState(directory)).toEqual(before)
    })
  })
}
