import {
  checkoutUsesLfs,
  runAuthenticatedLfsTransfer,
} from './notebookLfsLocal.js'

/**
 * When the checkout enables Git LFS, fills in only the current checkout's files via an
 * authenticated `git lfs pull`. Returns whether the fill-in ran. Failures end with the caller's
 * `nextStep` (e.g. `rerun "donut notebook pull"`).
 */
export function fillInCurrentLfsFilesIfNeeded(
  checkoutDir: string,
  notebookId: number,
  nextStep: string
): boolean {
  if (!checkoutUsesLfs(checkoutDir)) {
    return false
  }
  runAuthenticatedLfsTransfer(
    checkoutDir,
    notebookId,
    'receive',
    nextStep,
    ['pull', 'origin'],
    (detail, status) =>
      `Notebook attachments are incomplete: failed to download current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then ${nextStep}.`
  )
  return true
}
