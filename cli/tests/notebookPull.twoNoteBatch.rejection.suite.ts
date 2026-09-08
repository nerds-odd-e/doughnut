import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  commitPortableFile,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import {
  ACCEPTED_THIRD_NOTE,
  NOT_EXISTING_NOTE_CONTENT_EDIT,
  prepareTwoNoteBatchDivergence,
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
            localPaths: 'overlap-root',
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'overlap-nested',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            localPaths: 'overlap-nested',
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'extra-accepted-edge',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              commitPortableFile(
                source,
                'gamma.md',
                '---\ntype: Note\n---\n# Gamma\n\nFirst accepted.\n',
                'accepted first save'
              )
              commitPortableFile(
                source,
                'gamma.md',
                ACCEPTED_THIRD_NOTE,
                'accepted second save'
              )
            },
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'net-equivalent-interval',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              commitPortableFile(
                source,
                'gamma.md',
                ACCEPTED_THIRD_NOTE,
                'accepted save'
              )
              commitPortableFile(
                source,
                'gamma.md',
                '---\ntype: Note\n---\n# Gamma\n\nIntermediate.\n',
                'accepted intermediate'
              )
              commitPortableFile(
                source,
                'gamma.md',
                ACCEPTED_THIRD_NOTE,
                'accepted restore to same bytes'
              )
            },
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
        message:
          'Local main cannot receive the accepted history because accepted history includes a structural change at "gamma.md". Divergent structural history is not supported yet.',
      },
      {
        shape: 'three-local-edits',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            localPaths: 'three-notes',
          }),
        message: NOT_EXISTING_NOTE_CONTENT_EDIT,
      },
      {
        shape: 'accepted-multi-path-save',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              fs.writeFileSync(join(source, 'gamma.md'), ACCEPTED_THIRD_NOTE)
              fs.writeFileSync(
                join(source, 'note.md'),
                '---\ntype: Note\n---\n# Alpha\n\nAlso accepted.\n'
              )
              runGit(['add', 'gamma.md', 'note.md'], source)
              runGit(
                ['commit', '--quiet', '-m', 'accepted multi-path save'],
                source
              )
            },
          }),
        message: TWO_NOTE_UNSUPPORTED_ACCEPTED,
      },
      {
        shape: 'accepted-addition',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              commitPortableFile(
                source,
                'added.md',
                '---\ntype: Note\n---\n# Added\n\nNew note.\n',
                'accepted addition'
              )
            },
          }),
        message:
          'Local main cannot receive the accepted history because accepted history includes a structural change at "added.md". Divergent structural history is not supported yet.',
      },
      {
        shape: 'creation-then-save',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              commitPortableFile(
                source,
                'added.md',
                '---\ntype: Note\n---\n# Added\n\nNew note.\n',
                'accepted addition'
              )
              commitPortableFile(
                source,
                'added.md',
                '---\ntype: Note\n---\n# Added\n\nSaved.\n',
                'accepted creation follow-on save'
              )
            },
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
