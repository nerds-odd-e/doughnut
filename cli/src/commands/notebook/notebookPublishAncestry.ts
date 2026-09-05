import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { downloadNotebookGitBundle } from './notebookAcquisition.js'
import { runSystemGitOrThrow } from './systemGit.js'

const ANCESTRY_ERROR =
  'local main cannot be published: only a single direct commit on top of the ' +
  "notebook's currently accepted history can be published. Rebase or recreate " +
  'your change as one commit directly on the accepted head, then try again.'

/**
 * Confirms `directory`'s local `main` either matches the notebook's currently accepted history
 * exactly, or is exactly one direct (single-parent) commit ahead of it. Downloads the accepted
 * bundle (via the same {@link downloadNotebookGitBundle} used by `notebook clone`) into a
 * command-owned temporary directory and inspects it inside a temporary bare Git repository —
 * never fetches into or resets any ref in the user's own `directory`. Cleans up all temporary
 * files on both success and failure. Throws an actionable error for any other shape (stale/behind,
 * merge commit tip, unrelated history, or several commits ahead). Returns the accepted head SHA
 * so the caller can submit it as the publish request's expected head without re-downloading.
 */
export async function assertLocalMainFollowsAcceptedHistory(
  directory: string,
  notebookId: number
): Promise<string> {
  const tempDir = fs.mkdtempSync(
    path.join(os.tmpdir(), 'donut-notebook-publish-ancestry-')
  )
  try {
    const bundleFile = path.join(tempDir, 'accepted.bundle')
    await downloadNotebookGitBundle(notebookId, bundleFile)

    const acceptedRepoDir = path.join(tempDir, 'accepted.git')
    runSystemGitOrThrow(
      ['init', '--quiet', '--bare', acceptedRepoDir],
      (detail, status) =>
        `failed to prepare a temporary repository to check publish ancestry${detail ? `: ${detail}` : ` (exit code ${status})`}`
    )
    runSystemGitOrThrow(
      [
        '-C',
        acceptedRepoDir,
        'fetch',
        '--quiet',
        bundleFile,
        'refs/heads/main:refs/heads/main',
      ],
      (detail, status) =>
        `failed to read the notebook's accepted history${detail ? `: ${detail}` : ` (exit code ${status})`}`
    )

    const acceptedHead = runSystemGitOrThrow(
      ['-C', acceptedRepoDir, 'rev-parse', 'main'],
      (detail, status) =>
        `failed to read the notebook's accepted history${detail ? `: ${detail}` : ` (exit code ${status})`}`
    ).trim()

    const localHead = runSystemGitOrThrow(
      ['-C', directory, 'rev-parse', 'main'],
      (detail, status) =>
        `failed to read local main${detail ? `: ${detail}` : ` (exit code ${status})`}`
    ).trim()

    if (localHead === acceptedHead) return acceptedHead

    const parents = runSystemGitOrThrow(
      ['-C', directory, 'log', '-1', '--format=%P', localHead],
      (detail, status) =>
        `failed to inspect local main's history${detail ? `: ${detail}` : ` (exit code ${status})`}`
    )
      .trim()
      .split(/\s+/)
      .filter((sha) => sha !== '')

    if (parents.length === 1 && parents[0] === acceptedHead) return acceptedHead

    throw new Error(ANCESTRY_ERROR)
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}
