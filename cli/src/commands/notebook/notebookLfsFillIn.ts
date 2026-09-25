import {
  checkoutHasLfsFiles,
  prepareAuthenticatedLfsCheckout,
  runGitLfsOrThrow,
} from './notebookLfsLocal.js'

/**
 * Prepares the checkout for authenticated Git LFS transfers and fills in the current
 * checkout's LFS files via `git lfs pull`; a checkout without LFS files skips the download. Failures end with the caller's `nextStep` (e.g. `rerun "donut notebook pull"`).
 */
export function fillInCurrentLfsFiles(
  checkoutDir: string,
  notebookId: number,
  nextStep: string
): void {
  prepareAuthenticatedLfsCheckout(checkoutDir, notebookId, 'receive', nextStep)
  if (!checkoutHasLfsFiles(checkoutDir)) {
    return
  }
  runGitLfsOrThrow(
    checkoutDir,
    'receive',
    nextStep,
    ['pull'],
    (detail, status) =>
      `Notebook attachments are incomplete: failed to download current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then ${nextStep}.`
  )
}
