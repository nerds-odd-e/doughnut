import * as fs from 'node:fs'
import * as path from 'node:path'
import { runSystemGitOrThrow } from './systemGit.js'

function stripTrailingSlash(apiBaseUrl: string): string {
  return apiBaseUrl.replace(/\/$/, '')
}

function notebookLfsEndpoint(apiBaseUrl: string, notebookId: number): string {
  return `${stripTrailingSlash(apiBaseUrl)}/api/notebooks/${notebookId}/lfs`
}

/** True when the checkout's `.gitattributes` enables the standard LFS filter. */
function checkoutUsesLfs(checkoutDir: string): boolean {
  const attributesPath = path.join(checkoutDir, '.gitattributes')
  if (!fs.existsSync(attributesPath)) {
    return false
  }
  return fs.readFileSync(attributesPath, 'utf8').includes('filter=lfs')
}

function requireGitLfs(): void {
  runSystemGitOrThrow(
    ['lfs', 'version'],
    (detail, status) =>
      `Git LFS is required to clone this notebook's attachments${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Install Git LFS, then rerun "donut notebook clone".`
  )
}

/**
 * When the tip enables Git LFS, configures the authenticated notebook LFS endpoint in
 * local Git config (never authored content), then hydrates only the selected current
 * checkout via `git lfs fetch` and `git lfs checkout`. Returns whether hydration ran.
 * The bundle-pointing `origin` must already be gone; a placeholder remote satisfies
 * Git LFS's remote argument while transfers use `lfs.url`.
 */
export function configureAndHydrateCurrentLfsCheckoutIfNeeded(
  checkoutDir: string,
  notebookId: number,
  apiBaseUrl: string,
  token: string
): boolean {
  if (!checkoutUsesLfs(checkoutDir)) {
    return false
  }
  requireGitLfs()
  const lfsUrl = notebookLfsEndpoint(apiBaseUrl, notebookId)
  const noPrompt = {
    env: { ...process.env, GIT_TERMINAL_PROMPT: '0', GIT_LFS_SKIP_SMUDGE: '1' },
  }
  runSystemGitOrThrow(
    [
      '-C',
      checkoutDir,
      'remote',
      'add',
      'origin',
      `${stripTrailingSlash(apiBaseUrl)}/donut-notebook.git`,
    ],
    (detail, status) =>
      `failed to add placeholder origin for Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`
  )
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
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'lfs', 'install', '--local'],
    (detail, status) =>
      `failed to configure Git LFS in the checkout${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }`,
    noPrompt
  )
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'lfs', 'fetch', 'origin'],
    (detail, status) =>
      `failed to download current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then rerun "donut notebook clone".`,
    noPrompt
  )
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'lfs', 'checkout'],
    (detail, status) =>
      `failed to materialize current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then rerun "donut notebook clone".`,
    noPrompt
  )
  return true
}
