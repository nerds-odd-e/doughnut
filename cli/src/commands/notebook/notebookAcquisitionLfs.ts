import {
  checkoutUsesLfs,
  configureLocalLfsEndpointAndAuth,
  ensurePlaceholderOrigin,
  requireGitLfs,
} from './notebookLfsLocal.js'
import { runSystemGitOrThrow } from './systemGit.js'

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
  requireGitLfs(
    (detail, status) =>
      `Git LFS is required to clone this notebook's attachments${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Install Git LFS, then rerun "donut notebook clone".`
  )
  const noPrompt = {
    env: { ...process.env, GIT_TERMINAL_PROMPT: '0', GIT_LFS_SKIP_SMUDGE: '1' },
  }
  ensurePlaceholderOrigin(checkoutDir, apiBaseUrl)
  configureLocalLfsEndpointAndAuth(checkoutDir, notebookId, apiBaseUrl, token)
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
