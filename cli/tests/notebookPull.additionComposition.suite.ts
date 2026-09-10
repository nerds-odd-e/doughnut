import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
  STRUCTURAL_CHANGE_REFUSAL,
  type FileChange,
} from './notebookPull.testHelpers.js'
import {
  LOCAL_NESTED_NOTE,
  LOCAL_ROOT_NOTE,
  prepareTwoNoteBatchDivergence,
} from './notebookPull.twoNoteBatch.testHelpers.js'

const ADDED_V1 = '---\ntype: Note\n---\n# Added\n\nFirst version.\n'
const ADDED_V2 = '---\ntype: Note\n---\n# Added\n\nSecond version.\n'
const ADDED_V3 = '---\ntype: Note\n---\n# Added\n\nThird version.\n'
const NESTED_ADDED = '---\ntype: Note\n---\n# Beta\n\nNested addition.\n'
const GAMMA_SAVED = '---\ntype: Note\n---\n# Gamma\n\nAccepted save.\n'
const EXTRA_ADDED = '---\ntype: Note\n---\n# Extra\n\nRoot addition.\n'

const EXPECTED_FILES: Record<string, string> = {
  'added.md': ADDED_V3,
  'Nested/Beta.md': NESTED_ADDED,
  'gamma.md': GAMMA_SAVED,
  'extra.md': EXTRA_ADDED,
}

export function describeNotebookPullAdditionComposition(): void {
  describe('notebook pull (composed accepted additions and saves over a local batch)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-addition-composition-test-'
    )

    test.each([
      {
        shape: 'grouped-additions-then-saves',
        acceptedChangeSets: [
          [
            { path: 'added.md', content: ADDED_V1 },
            { path: 'Nested/Beta.md', content: NESTED_ADDED },
          ],
          [{ path: 'added.md', content: ADDED_V2 }],
          [
            { path: 'gamma.md', content: GAMMA_SAVED },
            { path: 'extra.md', content: EXTRA_ADDED },
          ],
          [{ path: 'added.md', content: ADDED_V3 }],
        ] satisfies FileChange[][],
      },
      {
        shape: 'interleaved-saves-then-additions',
        acceptedChangeSets: [
          [{ path: 'gamma.md', content: GAMMA_SAVED }],
          [{ path: 'added.md', content: ADDED_V1 }],
          [
            { path: 'Nested/Beta.md', content: NESTED_ADDED },
            { path: 'added.md', content: ADDED_V2 },
          ],
          [
            { path: 'extra.md', content: EXTRA_ADDED },
            { path: 'added.md', content: ADDED_V3 },
          ],
        ] satisfies FileChange[][],
      },
    ])(
      'retains a two-path local batch as one unpublished child over $shape',
      async ({ acceptedChangeSets, shape }) => {
        const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
          acceptedChangeSets,
        })
        serveAcceptedBundle(ctx, setup.source, `addition-composition-${shape}`)
        const originalAuthor = runGit(
          ['log', '-1', '--format=%an <%ae>', setup.localTip],
          setup.directory
        )
        const originalMessage = runGit(
          ['log', '-1', '--format=%s', setup.localTip],
          setup.directory
        )

        await run(['notebook', 'pull', setup.directory])

        expect(runGit(['rev-parse', 'HEAD^'], setup.directory)).toBe(
          setup.acceptedHead
        )
        expect(
          runGit(
            ['rev-list', '--count', 'HEAD', '--not', setup.acceptedHead],
            setup.directory
          )
        ).toBe('1')
        expect(fs.readFileSync(join(setup.directory, 'note.md'), 'utf8')).toBe(
          LOCAL_ROOT_NOTE
        )
        expect(
          fs.readFileSync(join(setup.directory, 'Nested/Cell.md'), 'utf8')
        ).toBe(LOCAL_NESTED_NOTE)
        for (const [path, content] of Object.entries(EXPECTED_FILES)) {
          expect(fs.readFileSync(join(setup.directory, path), 'utf8')).toBe(
            content
          )
        }
        expect(
          runGit(['log', '-1', '--format=%an <%ae>'], setup.directory)
        ).toBe(originalAuthor)
        expect(runGit(['log', '-1', '--format=%s'], setup.directory)).toBe(
          originalMessage
        )
        expect(() =>
          runGit(
            ['cat-file', '-e', `${setup.localTip}^{commit}`],
            setup.directory
          )
        ).not.toThrow()
      }
    )

    test('refuses the whole interval when an otherwise-eligible sequence ends in an unsupported addition, leaving local work and checkout intact', async () => {
      const setup = prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
        acceptedChangeSets: [
          [{ path: 'added.md', content: ADDED_V1 }],
          [{ path: 'added.md', content: ADDED_V2 }],
          [{ path: 'gamma.md', content: GAMMA_SAVED }],
          // Unsupported: a folder never represented anywhere in the preceding tree.
          [{ path: 'Unrepresented/Late.md', content: EXTRA_ADDED }],
        ],
      })
      serveAcceptedBundle(
        ctx,
        setup.source,
        'addition-composition-late-unsupported'
      )
      const before = checkoutState(setup.directory)
      const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

      await expect(run(['notebook', 'pull', setup.directory])).rejects.toThrow(
        ProcessExitForTest
      )

      expect(ctx.getErrorSpy().mock.calls[0]?.[0]).toMatch(
        STRUCTURAL_CHANGE_REFUSAL
      )
      expect(checkoutState(setup.directory)).toEqual(before)
      expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
    })
  })
}
