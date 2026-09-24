import { loadAuthenticatedFetchContext } from '../../backendApi/donutBackendClient.js'
import {
  checkoutUsesLfs,
  configureLocalLfsEndpointAndAuth,
  ensurePlaceholderOrigin,
  requireGitLfs,
  smudgeSkippedGitOptions,
} from './notebookLfsLocal.js'
import { runSystemGitOrThrow } from './systemGit.js'

/**
 * When the checkout enables Git LFS, refreshes the authenticated notebook LFS endpoint in
 * local Git config (never authored content) from the CLI's current login, then fills in only
 * the current checkout's files via `git lfs fetch` and `git lfs checkout`. Returns whether
 * the fill-in ran. A placeholder remote satisfies Git LFS's remote argument while transfers
 * use `lfs.url`. Failures tell the owner to rerun `rerunCommand`.
 */
export function fillInCurrentLfsFilesIfNeeded(
  checkoutDir: string,
  notebookId: number,
  rerunCommand: 'clone' | 'pull'
): boolean {
  if (!checkoutUsesLfs(checkoutDir)) {
    return false
  }
  const rerun = `rerun "donut notebook ${rerunCommand}"`
  requireGitLfs(
    (detail, status) =>
      `Git LFS is required to receive this notebook's attachments${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Install Git LFS, then ${rerun}.`
  )
  const noPrompt = {
    env: { ...smudgeSkippedGitOptions().env, GIT_TERMINAL_PROMPT: '0' },
  }
  const { apiBaseUrl, token } = loadAuthenticatedFetchContext()
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
  const incomplete =
    (verb: string) => (detail: string | undefined, status: number | null) =>
      `Notebook attachments are incomplete: failed to ${verb} current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then ${rerun}.`
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'lfs', 'fetch', 'origin'],
    incomplete('download'),
    noPrompt
  )
  runSystemGitOrThrow(
    ['-C', checkoutDir, 'lfs', 'checkout'],
    incomplete('materialize'),
    noPrompt
  )
  return true
}
