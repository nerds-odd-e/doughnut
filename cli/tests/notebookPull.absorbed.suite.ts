import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { runGit } from './notebookClone.testHelpers.js'
import {
  cloneWithLocalNoteEdit,
  commitPortableFile,
  GIT_BUNDLE_GET,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'

const ORIGINAL_BODY_NOTE = '---\ntype: Note\n---\n# Note\n\nOriginal body.\n'
const LOCAL_BODY_NOTE = '---\ntype: Note\n---\n# Note\n\nLocal body.\n'

export function describeNotebookPullAbsorbed(): void {
  describe('notebook pull (absorbed same-note patch)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-absorbed-test-'
    )

    test('reports no unpublished change when accepted history already contains the local patch', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        ORIGINAL_BODY_NOTE,
        LOCAL_BODY_NOTE
      )
      commitPortableFile(
        setup.source,
        'note.md',
        LOCAL_BODY_NOTE,
        'accepted independently contains the local patch'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'absorbed-patch')

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(acceptedHead)
      expect(
        runGit(['rev-parse', '--abbrev-ref', 'HEAD'], setup.directory)
      ).toBe('main')
      expect(runGit(['status', '--porcelain=v1'], setup.directory)).toBe('')
      expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
        LOCAL_BODY_NOTE
      )
      expect(runGit(['rev-parse', 'ORIG_HEAD'], setup.directory)).toBe(
        setup.localTip
      )
      expect(() =>
        runGit(
          ['cat-file', '-e', `${setup.localTip}^{commit}`],
          setup.directory
        )
      ).not.toThrow()
      expect(ctx.getLogSpy()).toHaveBeenCalledWith(
        `Rebased onto the accepted history. No unpublished change remains. Accepted head: ${acceptedHead}.`
      )
      expect(ctx.getFetchMock().mock.calls).toEqual([GIT_BUNDLE_GET])

      await run(['notebook', 'pull', setup.directory])

      expect(runGit(['rev-parse', 'HEAD'], setup.directory)).toBe(acceptedHead)
      expect(ctx.getLogSpy().mock.calls.at(-1)).toEqual([
        `Notebook unchanged. Accepted head: ${acceptedHead}`,
      ])
      expect(ctx.getFetchMock().mock.calls).toEqual([
        GIT_BUNDLE_GET,
        GIT_BUNDLE_GET,
      ])
    })
  })
}
