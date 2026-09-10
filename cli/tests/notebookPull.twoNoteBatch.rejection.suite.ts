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
  structuralChangeRefusal,
} from './notebookPull.testHelpers.js'
import {
  ACCEPTED_THIRD_NOTE,
  NOT_EXISTING_NOTE_CONTENT_EDIT,
  prepareTwoNoteBatchDivergence,
  THREE_NOTE_LOCAL_CHANGES,
  TWO_NOTE_UNSUPPORTED_ACCEPTED,
} from './notebookPull.twoNoteBatch.testHelpers.js'

export function describeNotebookPullTwoNoteBatchRejection(): void {
  describe('notebook pull (unsupported two-note batch shapes)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-two-note-batch-rejection-test-'
    )

    test.each([
      {
        shape: 'overlap-root',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [
                {
                  path: 'note.md',
                  content:
                    '---\ntype: Note\n---\n# Alpha\n\nAccepted overlapping root.\n',
                },
              ],
            ],
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'overlap-nested',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [
                {
                  path: 'Nested/Cell.md',
                  content:
                    '---\ntype: Note\n---\n# Nested\n\nAccepted overlapping nested.\n',
                },
              ],
            ],
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'extra-accepted-edge',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [
                {
                  path: 'gamma.md',
                  content: '---\ntype: Note\n---\n# Gamma\n\nFirst accepted.\n',
                },
              ],
              [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
            ],
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'net-equivalent-interval',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
              [
                {
                  path: 'gamma.md',
                  content: '---\ntype: Note\n---\n# Gamma\n\nIntermediate.\n',
                },
              ],
              [{ path: 'gamma.md', content: ACCEPTED_THIRD_NOTE }],
            ],
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'structural-edge',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              fs.chmodSync(join(source, 'gamma.md'), 0o755)
              runGit(['add', 'gamma.md'], source)
              runGit(['commit', '--quiet', '-m', 'accepted mode'], source)
            },
          }),
        message: structuralChangeRefusal('gamma.md'),
      },
      {
        shape: 'three-local-edits',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            localChanges: THREE_NOTE_LOCAL_CHANGES,
          }),
        message: NOT_EXISTING_NOTE_CONTENT_EDIT,
      },
      {
        shape: 'accepted-multi-path-save',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [
                { path: 'gamma.md', content: ACCEPTED_THIRD_NOTE },
                {
                  path: 'note.md',
                  content: '---\ntype: Note\n---\n# Alpha\n\nAlso accepted.\n',
                },
              ],
            ],
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'accepted-addition',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [
                {
                  path: 'added.md',
                  content: '---\ntype: Note\n---\n# Added\n\nNew note.\n',
                },
              ],
            ],
          }),
        message: structuralChangeRefusal('added.md'),
      },
      {
        shape: 'creation-then-save',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            acceptedChangeSets: [
              [
                {
                  path: 'added.md',
                  content: '---\ntype: Note\n---\n# Added\n\nNew note.\n',
                },
              ],
              [
                {
                  path: 'added.md',
                  content: '---\ntype: Note\n---\n# Added\n\nSaved.\n',
                },
              ],
            ],
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
    ] as const)(
      'refuses unsupported $shape without changing the checkout',
      async ({ shape, prepare, message }) => {
        const setup = prepare()
        serveAcceptedBundle(ctx, setup.source, `two-note-reject-${shape}`)
        const before = checkoutState(setup.directory)
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(
          run(['notebook', 'pull', setup.directory])
        ).rejects.toThrow(ProcessExitForTest)

        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${message}`)
        expect(checkoutState(setup.directory)).toEqual(before)
        expect(runGit(['rev-parse', setup.acceptedHead], setup.source)).toBe(
          setup.acceptedHead
        )
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
