import { describe, expect, test } from 'vitest'
import { run } from '../src/run.js'
import { ProcessExitForTest } from './notebookClone.testHelpers.js'
import { buildSourceRepo } from './notebookPublish.testHelpers.js'
import { acceptedHistoryStagingDirsUnderTmp } from './notebookAcceptedHistory.testHelpers.js'
import {
  checkoutState,
  installNotebookPullAcceptedHistoryTest,
  serveAcceptedBundle,
} from './notebookPull.testHelpers.js'
import { prepareUnsupportedLocalHistory } from './notebookPull.localCandidate.testHelpers.js'

const NOT_EXISTING_NOTE_CONTENT_EDIT =
  'Local main cannot receive the accepted history because the unpublished commit is not one existing-note content edit. Recreate it as one unpublished commit that edits one existing ordinary Markdown note at an unchanged path, then try again.'

export function describeNotebookPullLocalCandidate(): void {
  describe('notebook pull (unsupported local candidate)', () => {
    const ctx = installNotebookPullAcceptedHistoryTest(
      'donut-cli-pull-local-candidate-test-'
    )

    test.each([
      {
        shape: 'unrelated',
        message:
          'Local main cannot receive the accepted history because it does not share Git history with the accepted notebook. Clone the notebook with "donut notebook clone", then try again.',
      },
      {
        shape: 'multiple-commits',
        message:
          'Local main cannot receive the accepted history because it contains more than one unpublished commit. Reduce local work to one unpublished commit that edits one existing ordinary Markdown note, then try again.',
      },
      {
        shape: 'merge',
        message:
          'Local main cannot receive the accepted history because the unpublished commit is a merge. Recreate the change as one ordinary commit that edits one existing ordinary Markdown note, then try again.',
      },
      {
        shape: 'add',
        message: NOT_EXISTING_NOTE_CONTENT_EDIT,
      },
      {
        shape: 'rename',
        message: NOT_EXISTING_NOTE_CONTENT_EDIT,
      },
      {
        shape: 'readme',
        message: NOT_EXISTING_NOTE_CONTENT_EDIT,
      },
      {
        shape: 'mode',
        message: NOT_EXISTING_NOTE_CONTENT_EDIT,
      },
    ] as const)(
      'explains unsupported $shape local history without changing the checkout',
      async ({ shape, message }) => {
        const source = buildSourceRepo(ctx.getWorkDir())
        const directory = prepareUnsupportedLocalHistory(
          ctx.getWorkDir(),
          source,
          shape
        )
        serveAcceptedBundle(ctx, source, shape)
        const before = checkoutState(directory)
        const stagingBefore = acceptedHistoryStagingDirsUnderTmp()

        await expect(run(['notebook', 'pull', directory])).rejects.toThrow(
          ProcessExitForTest
        )

        expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${message}`)
        expect(checkoutState(directory)).toEqual(before)
        expect(acceptedHistoryStagingDirsUnderTmp()).toEqual(stagingBefore)
      }
    )
  })
}
