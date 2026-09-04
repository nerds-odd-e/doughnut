import { spawnSync } from 'node:child_process'
import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  loadAuthenticatedFetchContext,
  withBackendClient,
} from '../../backendApi/donutBackendClient.js'

function exceptionText(e: unknown): string {
  return e instanceof Error ? e.message : String(e)
}

/**
 * Authenticated GET of the notebook's accepted Git bundle, written to `destinationFile`.
 * Uses {@link loadAuthenticatedFetchContext} for the same token/base-URL resolution as
 * {@link import('../../backendApi/donutBackendClient.js').attachNotebookBookFile}, but a
 * simpler error shape since there's no multipart response body to parse.
 */
async function downloadNotebookGitBundle(
  notebookId: number,
  destinationFile: string
): Promise<void> {
  const { token, apiBaseUrl } = loadAuthenticatedFetchContext()

  const buffer = await withBackendClient(token, async () => {
    const res = await fetch(
      `${apiBaseUrl}/api/notebooks/${notebookId}/git-bundle`,
      { headers: { Authorization: `Bearer ${token}` } }
    )
    if (!res.ok) {
      throw { status: res.status }
    }
    return res.arrayBuffer()
  })

  fs.writeFileSync(destinationFile, Buffer.from(buffer))
}

/** Runs the system `git` executable to produce a clean checkout from a local bundle file. */
function cloneBundleWithSystemGit(bundleFile: string, targetDir: string): void {
  const result = spawnSync('git', ['clone', '--quiet', bundleFile, targetDir], {
    encoding: 'utf8',
  })
  if (result.error) {
    throw new Error(
      `git is required but could not be run: ${exceptionText(result.error)}`
    )
  }
  if (result.status !== 0) {
    const detail = result.stderr?.trim()
    throw new Error(
      `git clone of the notebook bundle failed${detail ? `: ${detail}` : ` (exit code ${result.status})`}`
    )
  }
}

/** Atomically installs a staged checkout at `destinationPath`, which must not already exist. */
function moveCheckoutIntoDestination(
  stagedCheckoutDir: string,
  destinationPath: string
): void {
  if (fs.existsSync(destinationPath)) {
    throw new Error(`destination already exists: ${destinationPath}`)
  }
  try {
    fs.renameSync(stagedCheckoutDir, destinationPath)
  } catch (e) {
    throw new Error(
      `failed to move checkout into destination: ${exceptionText(e)}`
    )
  }
}

/**
 * Downloads the notebook's accepted Git bundle and produces a clean local checkout at
 * `destinationPath`. All download/clone work happens in command-owned temporary staging;
 * `destinationPath` is only ever touched by the final atomic move, and only once staging
 * fully succeeds. Staging is always removed afterward, success or failure.
 */
export async function acquireNotebookGitCheckout(
  notebookId: number,
  destinationPath: string
): Promise<void> {
  if (fs.existsSync(destinationPath)) {
    throw new Error(`destination already exists: ${destinationPath}`)
  }

  const stagingDir = fs.mkdtempSync(
    path.join(os.tmpdir(), 'donut-notebook-clone-')
  )
  try {
    const bundleFile = path.join(stagingDir, 'notebook.bundle')
    await downloadNotebookGitBundle(notebookId, bundleFile)

    const checkoutDir = path.join(stagingDir, 'checkout')
    cloneBundleWithSystemGit(bundleFile, checkoutDir)

    moveCheckoutIntoDestination(checkoutDir, destinationPath)
  } finally {
    fs.rmSync(stagingDir, { recursive: true, force: true })
  }
}
