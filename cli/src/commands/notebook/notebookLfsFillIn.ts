import {
  checkoutHasLfsFiles,
  checkoutUsesLfs,
  prepareAuthenticatedLfsCheckout,
  runGitLfsOrThrow,
} from './notebookLfsLocal.js'

/**
 * When the checkout enables Git LFS, prepares it for authenticated transfers and fills in the
 * current checkout's LFS files via `git lfs pull`; a checkout without LFS files skips the
 * download. Failures end with the caller's `nextStep` (e.g. `rerun "donut notebook pull"`).
 */
export function fillInCurrentLfsFilesIfNeeded(
  checkoutDir: string,
  notebookId: number,
  nextStep: string
): void {
  if (!checkoutUsesLfs(checkoutDir)) {
    return
  }
  prepareAuthenticatedLfsCheckout(checkoutDir, notebookId, 'receive', nextStep)
  if (!checkoutHasLfsFiles(checkoutDir)) {
    return
  }
  runGitLfsOrThrow(
    checkoutDir,
    'receive',
    nextStep,
    ['pull', 'origin'],
    (detail, status) =>
      `Notebook attachments are incomplete: failed to download current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then ${nextStep}.`
  )
}
