import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import {
  loadAuthenticatedFetchContext,
  withBackendClient,
} from '../../backendApi/donutBackendClient.js'
import { exceptionText } from '../../exceptionText.js'
import { errnoCode } from '../../errnoCode.js'
import { runSystemGitOrThrow } from './systemGit.js'

/**
 * Authenticated GET of the notebook's accepted Git bundle, written to `destinationFile`.
 * Uses {@link loadAuthenticatedFetchContext} for the same token/base-URL resolution as
 * {@link import('../../backendApi/donutBackendClient.js').attachNotebookBookFile}, but a
 * simpler error shape since there's no multipart response body to parse.
 */
export async function downloadNotebookGitBundle(
  notebookId: number,
  destinationFile: string
): Promise<{ apiBaseUrl: string }> {
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
  return { apiBaseUrl }
}

/** Runs the system `git` executable to produce a clean checkout from a local bundle file. */
function cloneBundleWithSystemGit(bundleFile: string, targetDir: string): void {
  runSystemGitOrThrow(
    ['clone', '--quiet', bundleFile, targetDir],
    (detail, status) =>
      `git clone of the notebook bundle failed${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
}

/**
 * Records a local-only Git config binding (never a tracked file) marking which Donut notebook
 * and API origin this checkout came from, so a future publish command can find its way back.
 */
function recordLocalNotebookBinding(
  checkoutDir: string,
  notebookId: number,
  apiBaseUrl: string
): void {
  const bindings: [string, string][] = [
    ['donut.notebook-id', String(notebookId)],
    ['donut.api-origin', apiBaseUrl],
  ]
  for (const [key, value] of bindings) {
    runSystemGitOrThrow(
      ['-C', checkoutDir, 'config', '--local', key, value],
      (detail, status) =>
        `failed to record local Git config ${key}${detail ? `: ${detail}` : ` (exit code ${status})`}`
    )
  }
}

/**
 * Removes the `origin` remote that `git clone <bundle-file> <target>` points at the (deleted)
 * temporary local bundle file, so a finished checkout has no dangling remote.
 */
function removeOriginRemote(checkoutDir: string): void {
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'remote', 'remove', 'origin'],
    (detail, status) =>
      `failed to remove origin remote${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
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
    if (errnoCode(e) === 'EXDEV') {
      fs.cpSync(stagedCheckoutDir, destinationPath, { recursive: true })
      fs.rmSync(stagedCheckoutDir, { recursive: true, force: true })
      return
    }
    throw new Error(
      `failed to move checkout into destination: ${exceptionText(e)}`
    )
  }
}

/**
 * Downloads the notebook's accepted Git bundle and produces a clean local checkout at
 * `destinationPath`, with a local-only (untracked) Git config binding
 * ({@link recordLocalNotebookBinding}) recording the source notebook id and API origin, and
 * with the bundle-pointing `origin` remote removed ({@link removeOriginRemote}). Both happen
 * while the checkout is still in command-owned temporary staging, before the final atomic move;
 * `destinationPath` is only ever touched by that move, and only once staging fully succeeds.
 * Staging is always removed afterward, success or failure.
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
    const { apiBaseUrl } = await downloadNotebookGitBundle(
      notebookId,
      bundleFile
    )

    const checkoutDir = path.join(stagingDir, 'checkout')
    cloneBundleWithSystemGit(bundleFile, checkoutDir)

    recordLocalNotebookBinding(checkoutDir, notebookId, apiBaseUrl)
    removeOriginRemote(checkoutDir)
    moveCheckoutIntoDestination(checkoutDir, destinationPath)
  } finally {
    fs.rmSync(stagingDir, { recursive: true, force: true })
  }
}
