import { downloadAcceptedNotebookHead } from './notebookAcceptedHistory.js'
import { runSystemGitOrThrow } from './systemGit.js'

const RECEIVE_UNAVAILABLE =
  'Receiving a differing accepted notebook head is not available yet.'

/**
 * Recognizes the no-op case only; a differing valid head remains unavailable until the receive
 * command can prove ancestry and perform a safe fast-forward.
 */
export async function receiveAcceptedNotebookHead(
  directory: string,
  notebookId: number
): Promise<string> {
  const acceptedHead = await downloadAcceptedNotebookHead(notebookId)
  const localHead = runSystemGitOrThrow(
    ['-C', directory, 'rev-parse', 'main'],
    (detail, status) =>
      `failed to read local main${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trim()

  if (localHead !== acceptedHead) throw new Error(RECEIVE_UNAVAILABLE)
  return acceptedHead
}
