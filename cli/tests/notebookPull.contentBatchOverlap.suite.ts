import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import {
  abortPausedRebase,
  continuePausedRebaseWithChosenBytes,
} from './notebookPull.conflict.testHelpers.js'
import {
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  LOCAL_COMPANION_NOTE,
  LOCAL_NESTED_NOTE,
  LOCAL_ROOT_NOTE,
  prepareTwoFileOverlap,
} from './notebookPull.contentBatch.testHelpers.js'

export function describeNotebookPullContentBatchOverlap(): void {
  describe('notebook pull (content batch overlapping accepted edits)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-content-batch-overlap-test-'
    )

    test('pauses on a two-file overlap and continues with an untouched companion edit intact', async () => {
      const setup = prepareTwoFileOverlap(ctx.getWorkDir())
      serveAcceptedBundle(ctx, setup.source, 'batch-two-file-overlap')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      expect(runGit(['ls-files', '-u'], setup.directory)).toContain('note.md')
      expect(runGit(['ls-files', '-u'], setup.directory)).toContain(
        'Nested/Cell.md'
      )
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        LOCAL_COMPANION_NOTE
      )

      continuePausedRebaseWithChosenBytes(setup.directory, [
        {
          path: 'note.md',
          content: '---\ntype: Note\n---\n# Alpha\n\nChosen root.\n',
        },
        {
          path: 'Nested/Cell.md',
          content: '---\ntype: Note\n---\n# Nested\n\nChosen nested.\n',
        },
      ])

      expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
        setup.acceptedHead
      )
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        '---\ntype: Note\n---\n# Alpha\n\nChosen root.\n'
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe('---\ntype: Note\n---\n# Nested\n\nChosen nested.\n')
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        LOCAL_COMPANION_NOTE
      )
    })

    test('aborting a two-file overlap pause restores the entire original batch', async () => {
      const setup = prepareTwoFileOverlap(ctx.getWorkDir())
      const originalTree = runGit(
        ['rev-parse', `${setup.localTip}^{tree}`],
        setup.directory
      )
      serveAcceptedBundle(ctx, setup.source, 'batch-two-file-overlap-abort')

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )
      abortPausedRebase(setup.directory)

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(runGit(['write-tree'], setup.directory)).toBe(originalTree)
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_ROOT_NOTE
      )
      expect(
        fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
      ).toBe(LOCAL_NESTED_NOTE)
      expect(fs.readFileSync(join(setup.directory, 'gamma.md'), 'utf8')).toBe(
        LOCAL_COMPANION_NOTE
      )
    })
  })
}
