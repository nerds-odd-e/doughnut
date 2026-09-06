import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_UNAVAILABLE =
  'Receiving a differing accepted notebook head is not available yet.'
const RECEIVE_ANCESTRY_ERROR =
  'Local main cannot receive the accepted history because it contains unpublished or unrelated commits. ' +
  'Publish or reconcile those commits, then try again.'

/**
 * Rejects local history that cannot be fast-forwarded to the accepted head, while recognizing the
 * no-op case. A differing accepted descendant remains unavailable until fast-forward is supported.
 */
export async function receiveAcceptedNotebookHead(
  directory: string,
  notebookId: number
): Promise<string> {
  return withDownloadedAcceptedNotebookHistory(
    notebookId,
    (acceptedRepoDir, acceptedHead) => {
      const localHead = runSystemGitOrThrow(
        ['-C', directory, 'rev-parse', 'main'],
        (detail, status) =>
          `failed to read local main${detail ? `: ${detail}` : ` (exit code ${status})`}`
      ).trim()

      if (localHead === acceptedHead) return acceptedHead

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
      const localCommitOutsideAcceptedHistory = runSystemGitOrThrow(
        [
          '-C',
          acceptedRepoDir,
          'rev-list',
          '--max-count=1',
          localHead,
          '--not',
          acceptedHead,
        ],
        (detail, status) =>
          `failed to inspect local main's ancestry${detail ? `: ${detail}` : ` (exit code ${status})`}`
      ).trim()

      if (localCommitOutsideAcceptedHistory)
        throw new Error(RECEIVE_ANCESTRY_ERROR)
      throw new Error(RECEIVE_UNAVAILABLE)
    }
  )
}
