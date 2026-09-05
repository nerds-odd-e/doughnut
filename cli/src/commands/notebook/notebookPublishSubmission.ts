import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { loadAuthenticatedFetchContext } from '../../backendApi/donutBackendClient.js'
import { runSystemGitOrThrow } from './systemGit.js'

const PUBLISH_DENIED_MESSAGE =
  "you don't have permission to publish this notebook — publishing requires the notebook owner's credentials."

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
 * `expectedHead` as the request parameter. Uses the same authenticated
 * token/API-origin resolution as the bundle download ({@link loadAuthenticatedFetchContext}).
 * Interprets the response:
 * - 401/403 → throws the explicit authorization-denial message.
 * - 200 → returns the accepted head from the response body.
 * - any other status → throws the server's `ApiError.message`, or an HTTP-status fallback when
 *   the response does not contain one.
 * Never mutates any local ref or file; the built bundle is a command-owned temp file, removed on
 * both success and failure.
 */
export async function submitNotebookGitProposal(
  directory: string,
  notebookId: number,
  expectedHead: string
): Promise<string> {
  const { token, apiBaseUrl } = loadAuthenticatedFetchContext()

  const tempDir = fs.mkdtempSync(
    path.join(os.tmpdir(), 'donut-notebook-publish-submission-')
  )
  try {
    const bundleFile = path.join(tempDir, 'proposal.bundle')
    buildLocalMainBundle(directory, bundleFile)
    const bundleBytes = fs.readFileSync(bundleFile)

    const url = `${apiBaseUrl}/api/notebooks/${notebookId}/git-bundle?expectedHead=${encodeURIComponent(expectedHead)}`
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/x-git-bundle',
      },
      body: bundleBytes,
    })

    if (res.status === 401 || res.status === 403) {
      throw new Error(PUBLISH_DENIED_MESSAGE)
    }

    if (res.ok) {
      return (await res.text()).trim()
    }

    const responseBodyText = await res.text()
    let responseBody: unknown
    try {
      responseBody = JSON.parse(responseBodyText) as unknown
    } catch {
      responseBody = undefined
    }
    if (
      typeof responseBody === 'object' &&
      responseBody !== null &&
      'message' in responseBody &&
      typeof responseBody.message === 'string' &&
      responseBody.message.trim() !== ''
    ) {
      throw new Error(responseBody.message)
    }
    throw new Error(`notebook publication rejected (HTTP ${res.status})`)
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true })
  }
}
