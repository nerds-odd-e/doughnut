import * as fs from 'node:fs'
import * as path from 'node:path'
import { runSystemGitOrThrow } from './systemGit.js'

function stripTrailingSlash(apiBaseUrl: string): string {
  return apiBaseUrl.replace(/\/$/, '')
}

function notebookLfsEndpoint(apiBaseUrl: string, notebookId: number): string {
  return `${stripTrailingSlash(apiBaseUrl)}/api/notebooks/${notebookId}/lfs`
}

function placeholderNotebookRemoteUrl(apiBaseUrl: string): string {
  return `${stripTrailingSlash(apiBaseUrl)}/donut-notebook.git`
}

/**
 * Options for worktree-changing Git operations: LFS pointers stay pointers until the
 * authenticated fill-in downloads the current files once.
 */
export function smudgeSkippedGitOptions(): { env: NodeJS.ProcessEnv } {
  return { env: { ...process.env, GIT_LFS_SKIP_SMUDGE: '1' } }
}

/** True when the checkout's `.gitattributes` enables the standard LFS filter. */
export function checkoutUsesLfs(checkoutDir: string): boolean {
  const attributesPath = path.join(checkoutDir, '.gitattributes')
  if (!fs.existsSync(attributesPath)) {
    return false
  }
  return fs.readFileSync(attributesPath, 'utf8').includes('filter=lfs')
}

export function requireGitLfs(
  describeFailure: (detail: string | undefined, status: number | null) => string
): void {
  runSystemGitOrThrow(['lfs', 'version'], describeFailure)
}

/** Adds `origin` when missing so `git lfs push` / `fetch` has a remote name. */
export function ensurePlaceholderOrigin(
  checkoutDir: string,
  apiBaseUrl: string
): void {
  const existing = runSystemGitOrThrow(
    ['-C', checkoutDir, 'remote'],
    (detail, status) =>
      `failed to list remotes${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
  if (
    existing
      .split('\n')
      .map((line) => line.trim())
      .includes('origin')
  ) {
    return
  }
  runSystemGitOrThrow(
    [
      '-C',
      checkoutDir,
      'remote',
      'add',
      'origin',
      placeholderNotebookRemoteUrl(apiBaseUrl),
    ],
    (detail, status) =>
      `failed to add placeholder origin for Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`
  )
}

export function configureLocalLfsEndpointAndAuth(
  checkoutDir: string,
  notebookId: number,
  apiBaseUrl: string,
  token: string
): void {
  const lfsUrl = notebookLfsEndpoint(apiBaseUrl, notebookId)
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'config', '--local', 'lfs.url', lfsUrl],
    (detail, status) =>
      `failed to record local Git LFS endpoint${detail ? `: ${detail}` : ` (exit code ${status})`}`
  )
  runSystemGitOrThrow(
    [
      '-C',
      checkoutDir,
      'config',
      '--local',
      'http.extraHeader',
      `Authorization: Bearer ${token}`,
    ],
    (detail, status) =>
      `failed to record local Git LFS authorization${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`
  )
}
