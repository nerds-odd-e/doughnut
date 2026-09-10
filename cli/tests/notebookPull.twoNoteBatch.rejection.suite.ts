import * as fs from 'node:fs'
import { join } from 'node:path'
import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION } from '../src/commands/notebook/notebookLocalCandidate.js'
import { ProcessExitForTest, runGit } from './notebookClone.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
  structuralChangeRefusal,
} from './notebookPull.testHelpers.js'
import { prepareTwoNoteBatchDivergence } from './notebookPull.twoNoteBatch.testHelpers.js'

const LOCAL_NON_LINEAR_ACCEPTED =
  'Local main cannot receive the accepted history because the accepted history since the local parent is not one contiguous chain of saves. ' +
  LOCAL_WORK_PRESERVED_BEFORE_PUBLICATION

export function describeNotebookPullTwoNoteBatchRejection(): void {
  describe('notebook pull (unsupported two-note batch shapes)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-two-note-batch-rejection-test-'
    )

    test.each([
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
        // Two accepted branches (each an ordinary content edit) diverge from the local parent
        // and are merged back into accepted main: a genuine merge commit, not a contiguous
        // single-parent chain, even though every individual edge is content-only.
        shape: 'non-linear-accepted-merge',
        prepare: () =>
          prepareTwoNoteBatchDivergence(ctx.getWorkDir(), {
            remote: (source) => {
              runGit(['checkout', '--quiet', '-b', 'accepted-branch'], source)
              fs.writeFileSync(
                join(source, 'gamma.md'),
                '---\ntype: Note\n---\n# Gamma\n\nBranch edit.\n'
              )
              runGit(['add', 'gamma.md'], source)
              runGit(['commit', '--quiet', '-m', 'branch save'], source)
              runGit(['checkout', '--quiet', 'main'], source)
              fs.writeFileSync(
                join(source, 'Nested/Cell.md'),
                '---\ntype: Note\n---\n# Nested\n\nMain edit.\n'
              )
              runGit(['add', 'Nested/Cell.md'], source)
              runGit(['commit', '--quiet', '-m', 'main save'], source)
              runGit(
                [
                  'merge',
                  '--quiet',
                  '--no-ff',
                  '-m',
                  'merge accepted branch',
                  'accepted-branch',
                ],
                source
              )
            },
          }),
        message: LOCAL_NON_LINEAR_ACCEPTED,
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
