import { downloadAcceptedNotebookHead } from './notebookAcceptedHistory.js'
import { runSystemGitOrThrow } from './systemGit.js'

const ANCESTRY_ERROR =
  'local main cannot be published: only a contiguous single-parent commit range on top of the ' +
  "notebook's currently accepted history can be published. " +
  'Run "donut notebook pull" to base your local commits on the accepted history, then try again.'

/**
 * Confirms `directory`'s local `main` either matches the notebook's currently accepted history
 * exactly, or is a contiguous single-parent commit range ahead of it. Downloads the accepted
 * bundle into command-owned temporary storage, without fetching into or resetting any ref in the
 * user's own `directory`. Throws an actionable error for any other shape (stale/behind, merge
 * commit tip, unrelated history, or a non-linear unpublished range). Returns the accepted head SHA
 * so the caller can submit it as the publish request's expected head without re-downloading.
 */
export async function assertLocalMainFollowsAcceptedHistory(
  directory: string,
  notebookId: number
): Promise<string> {
  const acceptedHead = await downloadAcceptedNotebookHead(notebookId)
  const localHead = runSystemGitOrThrow(
    ['-C', directory, 'rev-parse', 'main'],
    (detail, status) =>
      `failed to read local main${detail ? `: ${detail}` : ` (exit code ${status})`}`
  ).trim()

  if (localHead === acceptedHead) return acceptedHead

  // Walk only objects present in the checkout: acceptedHead may be absent when
  // local main is stale or unrelated.
  let current = localHead
  while (current !== acceptedHead) {
    const parents = runSystemGitOrThrow(
      ['-C', directory, 'log', '-1', '--format=%P', current],
      (detail, status) =>
        `failed to inspect local main's history${detail ? `: ${detail}` : ` (exit code ${status})`}`
    )
      .trim()
      .split(/\s+/)
      .filter((sha) => sha !== '')

    if (parents.length !== 1) {
      throw new Error(ANCESTRY_ERROR)
    }
    current = parents[0]!
  }

  return acceptedHead
}
