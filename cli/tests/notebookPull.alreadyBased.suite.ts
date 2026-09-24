import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  GIT_BUNDLE_GET,
  installNotebookPullAcceptedHistoryTest,
  alreadyBasedReport,
  rebasedReport,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  LATER_OTHER_NOTE,
  LOCAL_NOTE,
  prepareEligibleDivergence,
} from './notebookPull.rebase.testHelpers.js'
import {
  ACCEPTED_THIRD_NOTE,
  LOCAL_NESTED_NOTE,
  LOCAL_ROOT_NOTE,
  prepareTwoNoteBatchDivergence,
  THREE_NOTE_LOCAL_CHANGES,
} from './notebookPull.twoNoteBatch.testHelpers.js'

export function describeNotebookPullAlreadyBased(): void {
  describe('notebook pull (already-based unpublished commit)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-already-based-test-'
    )

    test('reports two local-ahead commits as already based without rewriting them', async () => {
      const setup = prepareEligibleDivergence(ctx.getWorkDir(), {
        remoteEdits: 0,
        localEdits: 2,
      })
      serveAcceptedBundle(ctx, setup.source, 'local-ahead')
      const before = checkoutState(setup.directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await run(['notebook', 'pull', setup.directory])

      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        alreadyBasedReport(setup.directory, setup.localTip, setup.acceptedHead)
      )
      expect(ctx.getFetchMock().mock.calls).toEqual([GIT_BUNDLE_GET])
      expect(checkoutState(setup.directory)).toEqual(before)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_NOTE
      )
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
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        noteAfterRebase
      )
      expect(fs.readFileSync(join(setup.directory, 'other.md'), 'utf8')).toBe(
        otherAfterRebase
      )
      expect(ctx.getLogSpy().mock.calls.at(-1)).toEqual([
        alreadyBasedReport(setup.directory, rebasedHead, setup.acceptedHead),
      ])
      expect(ctx.getFetchMock().mock.calls).toEqual([
        GIT_BUNDLE_GET,
        GIT_BUNDLE_GET,
      ])
    })

    test('accepts an already-based content batch spanning more than two paths without rewriting it', async () => {
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
        localChanges: THREE_NOTE_LOCAL_CHANGES,
        acceptedChangeSets: [],
      })
      serveAcceptedBundle(ctx, setup.source, 'already-based-three-note-batch')
      const before = checkoutState(setup.directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await run(['notebook', 'pull', setup.directory])

      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        alreadyBasedReport(setup.directory, setup.localTip, setup.acceptedHead)
      )
      expect(checkoutState(setup.directory)).toEqual(before)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_ROOT_NOTE
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe(LOCAL_NESTED_NOTE)
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        ACCEPTED_THIRD_NOTE
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
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
      expect(
        runGit(
          ['rev-list', '--count', 'HEAD', '--not', laterAccepted],
          setup.directory
        )
      ).toBe('1')
      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        laterAccepted
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_NOTE
      )
      expect(fs.readFileSync(join(setup.directory, 'other.md'), 'utf8')).toBe(
        LATER_OTHER_NOTE
      )
      expect(ctx.getLogSpy().mock.calls.at(-1)).toEqual([
        rebasedReport(setup.directory, laterLocalHead, laterAccepted),
      ])
    })
  })
}
