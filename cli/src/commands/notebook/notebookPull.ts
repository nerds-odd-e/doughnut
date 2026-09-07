import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { assertLocalMainIsReadyToReceive } from './notebookCheckoutReadiness.js'
import { inspectUnpublishedLocalHistory } from './notebookLocalCandidate.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_CHECKOUT_CHANGED =
  'Local main changed while the accepted history was downloading. Try again from the unchanged clean main.'

interface AcceptedNotebookReceiveResult {
  acceptedHead: string
  localHead: string
  changed: boolean
  rebased: boolean
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
        return {
          acceptedHead,
          localHead: capturedHead,
          changed: false,
          rebased: false,
        }
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
      const localHistory = inspectUnpublishedLocalHistory(
        acceptedRepoDir,
        capturedHead,
        acceptedHead
      )
      if (localHistory.kind === 'reject') throw new Error(localHistory.message)
      if (localHistory.kind === 'already-based') {
        return {
          acceptedHead,
          localHead: capturedHead,
          changed: false,
          rebased: false,
        }
      }

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
      if (localHistory.kind === 'rebase') {
        runSystemGitOrThrow(
          [
            '-C',
            directory,
            'rebase',
            '--onto',
            acceptedHead,
            localHistory.localParent,
          ],
          (detail, status) =>
            `failed to rebase the unpublished local commit onto the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`
        )
        return {
          acceptedHead,
          localHead: readHead(directory),
          changed: true,
          rebased: true,
        }
      }

      runSystemGitOrThrow(
        ['-C', directory, 'merge', '--quiet', '--ff-only', acceptedHead],
        (detail, status) =>
          `failed to fast-forward local main to the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      return {
        acceptedHead,
        localHead: acceptedHead,
        changed: true,
        rebased: false,
      }
    }
  )
}
