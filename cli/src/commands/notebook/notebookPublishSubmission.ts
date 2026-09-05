import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { loadAuthenticatedFetchContext } from '../../backendApi/donutBackendClient.js'
import { runSystemGitOrThrow } from './systemGit.js'

export const PUBLISH_DENIED_MESSAGE =
  "you don't have permission to publish this notebook — publishing requires the notebook owner's credentials."

export interface NotebookGitProposalSubmission {
  /** `true` when the endpoint accepted the proposal (only reachable via a test stub today). */
  accepted: boolean
  /** The accepted head reported back by the endpoint, when `accepted` is `true`. */
  acceptedHead?: string
  /** `true` when the endpoint denied the request as unauthorized (401/403). */
  denied?: boolean
}

/** Builds a full `main` bundle (the complete reachable history) from `directory` via system Git. */
function buildLocalMainBundle(directory: string, bundleFile: string): void {
  runSystemGitOrThrow(
    ['-C', directory, 'bundle', 'create', bundleFile, 'main'],
    (detail, status) =>
      `failed to build the local publish bundle${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
}

/**
 * Builds the full local `main` bundle via system Git and POSTs it, as raw
 * `application/x-git-bundle` bytes, to the notebook's owner-authorized Git-bundle endpoint, with
 * `acceptedHead` as the `expectedHead` request parameter. Uses the same authenticated
 * token/API-origin resolution as the bundle download ({@link loadAuthenticatedFetchContext}).
 * Interprets the response:
 * - 401/403 → returns `{ accepted: false, denied: true }` (authorization denial).
 * - 200 → returns `{ accepted: true, acceptedHead }`, reading the response body's text as the
 *   accepted head (only reachable via a test stub today; production always throws first).
 * - any other status → returns `{ accepted: false }`, letting the caller fall through to its own
 *   generic "not available yet" handling.
 * Never mutates any local ref or file; the built bundle is a command-owned temp file, removed on
 * both success and failure.
 */
export async function submitNotebookGitProposal(
  directory: string,
  notebookId: number,
  acceptedHead: string
): Promise<NotebookGitProposalSubmission> {
  const { token, apiBaseUrl } = loadAuthenticatedFetchContext()

  const tempDir = fs.mkdtempSync(
    path.join(os.tmpdir(), 'donut-notebook-publish-submission-')
  )
  try {
    const bundleFile = path.join(tempDir, 'proposal.bundle')
    buildLocalMainBundle(directory, bundleFile)
    const bundleBytes = fs.readFileSync(bundleFile)

    const url = `${apiBaseUrl}/api/notebooks/${notebookId}/git-bundle?expectedHead=${encodeURIComponent(acceptedHead)}`
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/x-git-bundle',
      },
      body: bundleBytes,
    })

    if (res.status === 401 || res.status === 403) {
      return { accepted: false, denied: true }
    }

    if (res.ok) {
      const body = (await res.text()).trim()
      return { accepted: true, acceptedHead: body }
    }

    return { accepted: false }
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}
