import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { assertLocalMainIsReadyToReceive } from './notebookCheckoutReadiness.js'
import { inspectUnpublishedLocalHistory } from './notebookLocalCandidate.js'
import { smudgeSkippedGitOptions } from './notebookLfsLocal.js'
import { rebaseUnpublishedCommits } from './notebookPullRebase.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_CHECKOUT_CHANGED =
  'Local main changed while the accepted history was downloading. Try again from the unchanged clean main.'

type AcceptedNotebookReceiveResult =
  | {
      kind:
        | 'unchanged'
        | 'already-based'
        | 'rebased'
        | 'absorbed'
        | 'fast-forward'
      acceptedHead: string
      localHead: string
    }
  /** A real conflict left the rebase paused; `conflictGuidance` tells the owner how to recover. */
  | { kind: 'paused'; conflictGuidance: string }

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
 * Downloads accepted history and advances an unchanged, clean local main: equal heads stay
 * unchanged, already-based linear unpublished commits stay unpublished, other linear
 * unpublished commits rebase over the accepted history with Git deciding what replays (an
 * empty remaining patch leaves local main at the accepted head, a real conflict pauses),
 * and ancestor checkouts fast-forward. Imported objects do not install a remote or a
 * persistent remote ref.
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
          kind: 'unchanged',
          acceptedHead,
          localHead: capturedHead,
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
          kind: 'already-based',
          acceptedHead,
          localHead: capturedHead,
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
        const conflictGuidance = rebaseUnpublishedCommits(
          directory,
          acceptedHead,
          localHistory.mergeBase
        )
        if (conflictGuidance !== undefined) {
          return { kind: 'paused', conflictGuidance }
        }
        const localHead = readHead(directory)
        return {
          kind: localHead === acceptedHead ? 'absorbed' : 'rebased',
          acceptedHead,
          localHead,
        }
      }

      runSystemGitOrThrow(
        ['-C', directory, 'merge', '--quiet', '--ff-only', acceptedHead],
        (detail, status) =>
          `failed to fast-forward local main to the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`,
        smudgeSkippedGitOptions()
      )

      return {
        kind: 'fast-forward',
        acceptedHead,
        localHead: acceptedHead,
      }
    }
  )
}
