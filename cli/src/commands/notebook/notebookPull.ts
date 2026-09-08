import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { assertLocalMainIsReadyToReceive } from './notebookCheckoutReadiness.js'
import { buildExactSubtreeMoveReplayCommit } from './notebookExactSubtreeMoveReplay.js'
import { inspectUnpublishedLocalHistory } from './notebookLocalCandidate.js'
import { rebaseUnpublishedCommit } from './notebookPullRebase.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_CHECKOUT_CHANGED =
  'Local main changed while the accepted history was downloading. Try again from the unchanged clean main.'

interface AcceptedNotebookReceiveResult {
  kind: 'unchanged' | 'already-based' | 'rebased' | 'absorbed' | 'fast-forward'
  acceptedHead: string
  localHead: string
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
 * Downloads accepted history and advances an unchanged, clean local main: equal heads stay
 * unchanged, an eligible already-based unpublished commit stays unpublished, eligible
 * content-only, one ordinary-note addition, that addition plus one content save of the same
 * note, or one two-note content edit over exactly one disjoint third-note content save
 * divergence rebases (an empty remaining patch leaves local main at the accepted head), one
 * local descendant content edit across one accepted exact same-name subtree relocation
 * replays onto the mapped path, and ancestor checkouts fast-forward. Imported objects do
 * not install a remote or a persistent remote ref.
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

      const installCommit =
        localHistory.kind === 'exact-subtree-move-replay'
          ? buildExactSubtreeMoveReplayCommit(
              acceptedRepoDir,
              acceptedHead,
              capturedHead,
              localHistory.localPath,
              localHistory.mapping
            )
          : acceptedHead

      runSystemGitOrThrow(
        [
          '-C',
          directory,
          'fetch',
          '--quiet',
          '--no-tags',
          '--no-write-fetch-head',
          acceptedRepoDir,
          installCommit,
        ],
        (detail, status) =>
          `failed to import accepted notebook history${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      assertCheckoutStillReady(directory, capturedHead)
      if (localHistory.kind === 'exact-subtree-move-replay') {
        runSystemGitOrThrow(
          ['-C', directory, 'reset', '--hard', '--quiet', installCommit],
          (detail, status) =>
            `failed to install the replayed unpublished local commit onto the accepted folder move${detail ? `: ${detail}` : ` (exit code ${status})`}`
        )
        return {
          kind: 'rebased',
          acceptedHead,
          localHead: readHead(directory),
        }
      }
      if (localHistory.kind === 'rebase') {
        rebaseUnpublishedCommit(
          directory,
          acceptedHead,
          localHistory.localParent
        )
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
          `failed to fast-forward local main to the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`
      )

      return {
        kind: 'fast-forward',
        acceptedHead,
        localHead: acceptedHead,
      }
    }
  )
}
