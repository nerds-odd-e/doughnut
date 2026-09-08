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
const SHARED_FRONTMATTER_BASE = '---\ntype: Note\n---\n'
const SAME_BODY_WITH_FINAL_LF =
  '---\ntype: Note\n---\n# Note\n\nSame authored body.\n'
const SAME_BODY_WITHOUT_FINAL_LF =
  '---\ntype: Note\n---\n# Note\n\nSame authored body.'

function expectAbsorbedPullOutcome(
  directory: string,
  acceptedHead: string,
  localTip: string,
  acceptedBytes: string,
  logSpy: { mock: { calls: unknown[][] } },
  fetchMock: { mock: { calls: unknown[] } },
  expectedFetchCalls: unknown[]
): void {
  expect(runGit(['rev-parse', 'HEAD'], directory)).toBe(acceptedHead)
  expect(runGit(['rev-parse', '--abbrev-ref', 'HEAD'], directory)).toBe('main')
  expect(runGit(['status', '--porcelain=v1'], directory)).toBe('')
  expect(fs.readFileSync(join(directory, 'note.md'), 'utf8')).toBe(
    acceptedBytes
  )
  expect(runGit(['rev-parse', 'ORIG_HEAD'], directory)).toBe(localTip)
  expect(() =>
    runGit(['cat-file', '-e', `${localTip}^{commit}`], directory)
  ).not.toThrow()
  expect(logSpy).toHaveBeenCalledWith(
    `Rebased onto the accepted history. No unpublished change remains. Accepted head: ${acceptedHead}.`
  )
  expect(fetchMock.mock.calls).toEqual(expectedFetchCalls)
}

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

      expectAbsorbedPullOutcome(
        setup.directory,
        acceptedHead,
        setup.localTip,
        LOCAL_BODY_NOTE,
        ctx.getLogSpy(),
        ctx.getFetchMock(),
        [GIT_BUNDLE_GET]
      )

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

    test('absorbs when the only difference is an optional final LF', async () => {
      const setup = cloneWithLocalNoteEdit(
        ctx.getWorkDir(),
        SHARED_FRONTMATTER_BASE,
        SAME_BODY_WITH_FINAL_LF
      )
      commitPortableFile(
        setup.source,
        'note.md',
        SAME_BODY_WITHOUT_FINAL_LF,
        'accepted same body without final LF'
      )
      const acceptedHead = runGit(['rev-parse', 'main'], setup.source)
      serveAcceptedBundle(ctx, setup.source, 'absorbed-final-lf')

      await run(['notebook', 'pull', setup.directory])

      expectAbsorbedPullOutcome(
        setup.directory,
        acceptedHead,
        setup.localTip,
        SAME_BODY_WITHOUT_FINAL_LF,
        ctx.getLogSpy(),
        ctx.getFetchMock(),
        [GIT_BUNDLE_GET]
      )

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
