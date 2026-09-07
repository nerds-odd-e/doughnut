import * as path from 'node:path'
import { withDownloadedAcceptedNotebookHistory } from './notebookAcceptedHistory.js'
import { assertLocalMainIsReadyToReceive } from './notebookCheckoutReadiness.js'
import { inspectUnpublishedLocalHistory } from './notebookLocalCandidate.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_CHECKOUT_CHANGED =
  'Local main changed while the accepted history was downloading. Try again from the unchanged clean main.'

interface AcceptedNotebookReceiveResult {
  kind: 'unchanged' | 'already-based' | 'rebased' | 'fast-forward'
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

function unpublishedCommitAuthor(directory: string): {
  name: string
  email: string
} {
  const text = runSystemGitOrThrow(
    ['-C', directory, 'log', '-1', '--format=%an%n%ae'],
    (detail, status) =>
      `failed to read the unpublished local commit identity${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trimEnd()
  const [name = '', email = ''] = text.split('\n')
  return { name, email }
}

function rebaseUnpublishedCommit(
  directory: string,
  acceptedHead: string,
  localParent: string
): void {
  const { name, email } = unpublishedCommitAuthor(directory)
  runSystemGitOrThrow(
    [
      '-C',
      directory,
      '-c',
      `user.name=${name}`,
      '-c',
      `user.email=${email}`,
      'rebase',
      '--onto',
      acceptedHead,
      localParent,
    ],
    (detail, status) =>
      `failed to rebase the unpublished local commit onto the accepted head${detail ? `: ${detail}` : ` (exit code ${status})`}`,
    {
      env: {
        ...process.env,
        GIT_EDITOR: 'true',
        GIT_SEQUENCE_EDITOR: 'true',
      },
    }
  )
}

/**
 * Downloads accepted history and advances an unchanged, clean local main: equal heads stay
 * unchanged, an eligible already-based unpublished commit stays unpublished, eligible
 * content-only divergence rebases, and ancestor checkouts fast-forward. Imported objects do
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
        rebaseUnpublishedCommit(
          directory,
          acceptedHead,
          localHistory.localParent
        )
        return {
          kind: 'rebased',
          acceptedHead,
          localHead: readHead(directory),
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
