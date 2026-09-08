import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  GIT_BUNDLE_GET,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  ACCEPTED_THIRD_NOTE,
  LOCAL_NESTED_NOTE,
  LOCAL_ROOT_NOTE,
  prepareTwoNoteBatchDivergence,
} from './notebookPull.twoNoteBatch.testHelpers.js'

export function describeNotebookPullTwoNoteBatch(): void {
  describe('notebook pull (two-note batch over one disjoint save)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-two-note-batch-test-'
    )

    test('rebases one root-and-nested two-note batch over one accepted third-note save', async () => {
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir())
      serveAcceptedBundle(ctx, setup.source, 'two-note-canonical')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()
      const originalAuthor = runGit(
        ['log', '-1', '--format=%an <%ae>', setup.localTip],
        setup.directory
      )
      const originalMessage = runGit(
        ['log', '-1', '--format=%s', setup.localTip],
        setup.directory
      )

      await run(['notebook', 'pull', setup.directory])

      const localHead = runGit(['rev-parse', 'HEAD'], setup.directory)
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(runGit(['rev-parse', setup.acceptedHead], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_ROOT_NOTE
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe(LOCAL_NESTED_NOTE)
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        ACCEPTED_THIRD_NOTE
      )
      expect(runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)).toBe(
        originalAuthor
      )
      expect(runGit(['log', '-1', '--format=%s'], setup.directory)).toBe(
        originalMessage
      )
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['rev-parse', 'ORIG_HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(
        runGit(
          ['rev-list', '--count', 'HEAD', '--not', setup.acceptedHead],
          setup.directory
        )
      ).toBe('1')
      expect(ctx.getFetchMock().mock.calls).toEqual([GIT_BUNDLE_GET])
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased onto the accepted history. Unpublished local commit: ${localHead}. Accepted head: ${setup.acceptedHead}. Inspect the result, then run "donut notebook publish ${setup.directory}".`
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })
  })
}
