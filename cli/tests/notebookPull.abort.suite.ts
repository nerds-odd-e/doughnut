import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  BODY_ACCEPTED,
  BODY_BASE,
  BODY_LOCAL,
  SPACED_NOTE_PATH,
  abortPausedRebase,
  prepareConflictingSameNote,
} from './notebookPull.conflict.testHelpers.js'
import {
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

function rebaseMergeExists(directory: string): boolean {
  return fs.existsSync(
    join(
      directory,
      runGit(['rev-parse', '--git-path', 'rebase-merge'], directory)
    )
  )
}

export function describeNotebookPullAbort(): void {
  describe('notebook pull (abort same-note conflict)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-abort-test-'
    )

    test('ordinary abort after CLI return restores original unpublished work', async () => {
      const setup = prepareConflictingSameNote(
        ctx.getWorkDir(),
        SPACED_NOTE_PATH,
        BODY_BASE,
        BODY_LOCAL,
        BODY_ACCEPTED
      )
      const originalTree = runGit(
        ['rev-parse', `${setup.localTip}^{tree}`],
        setup.directory
      )
      serveAcceptedBundle(ctx, setup.source, 'abort-original-work')
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      expect(rebaseMergeExists(setup.directory)).toBe(true)
      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(runGit(['rev-parse', 'main'], setup.directory)).toBe(
        setup.localTip
      )

      abortPausedRebase(setup.directory)

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(runGit(['rev-parse', 'main'], setup.directory)).toBe(
        setup.localTip
      )
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(runGit(['write-tree'], setup.directory)).toBe(originalTree)
      expect(
        fs.readFileSync(join(setup.directory, SPACED_NOTE_PATH), 'utf8')
      ).toBe(BODY_LOCAL)
      expect(rebaseMergeExists(setup.directory)).toBe(false)
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.acceptedHead}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(
        ctx
          .getFetchMock()
          .mock.calls.some(
            (call) =>
              (call[1] as { method?: string } | undefined)?.method === 'POST'
          )
      ).toBe(false)
    })
  })
}
