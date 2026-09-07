import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { assertLocalMainIsReadyToReceive } from './notebookCheckoutReadiness.js'
import { unpublishedLocalHistoryRejection } from './notebookLocalCandidate.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_CHECKOUT_CHANGED =
  'Local main changed while the accepted history was downloading. Try again from the unchanged clean main.'

interface AcceptedNotebookReceiveResult {
  acceptedHead: string
  changed: boolean
}

function readHead(directory: string): string {
  return runSystemGitOrThrow(
    ['-C', directory, 'rev-parse', 'HEAD'],
    (detail, status) =>
      `failed to read local HEAD${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trim()
}

function assertCheckoutStillReady(
  directory: string,
  capturedHead: string
): void {
  assertLocalMainIsReadyToReceive(directory)
  if (readHead(directory) !== capturedHead) {
    throw new Error(RECEIVE_CHECKOUT_CHANGED)
  }
}

/**
 * Downloads accepted history and advances an unchanged, clean local main with Git's non-forced
 * fast-forward operation. Imported objects do not install a remote or a persistent remote ref.
 */
export async function receiveAcceptedNotebookHead(
  directory: string,
  notebookId: number
): Promise<AcceptedNotebookReceiveResult> {
  const capturedHead = readHead(directory)

  return withDownloadedAcceptedNotebookHistory(
    notebookId,
    (acceptedRepoDir, acceptedHead) => {
      assertCheckoutStillReady(directory, capturedHead)

      if (capturedHead === acceptedHead) {
        return { acceptedHead, changed: false }
      }

      runSystemGitOrThrow(
        [
          '-C',
          acceptedRepoDir,
          'fetch',
          '--quiet',
          '--no-tags',
          path.resolve(directory),
          'refs/heads/main:refs/heads/local-main',
        ],
        (detail, status) =>
          `failed to import local main for ancestry inspection${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )
      const localHistoryRejection = unpublishedLocalHistoryRejection(
        acceptedRepoDir,
        capturedHead,
        acceptedHead
      )
      if (localHistoryRejection) throw new Error(localHistoryRejection)

      runSystemGitOrThrow(
        [
          '-C',
          directory,
          'fetch',
          '--quiet',
          '--no-tags',
          '--no-write-fetch-head',
          acceptedRepoDir,
          acceptedHead,
        ],
        (detail, status) =>
          `failed to import accepted notebook history${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      assertCheckoutStillReady(directory, capturedHead)
      runSystemGitOrThrow(
        ['-C', directory, 'merge', '--quiet', '--ff-only', acceptedHead],
        (detail, status) =>
          `failed to fast-forward local main to the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      return { acceptedHead, changed: true }
    }
  )
}
