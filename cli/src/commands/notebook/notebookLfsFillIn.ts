import {
  prepareAuthenticatedLfsCheckout,
  smudgeSkippedGitOptions,
} from './notebookLfsLocal.js'
import { runSystemGitOrThrow } from './systemGit.js'

/**
 * Prepares authenticated LFS transfers, then fills in only the current checkout's files via
 * `git lfs fetch` and `git lfs checkout`. Failures end with the caller's `nextStep` (e.g. `rerun "donut notebook pull"`).
 */
export function fillInCurrentLfsFiles(
  checkoutDir: string,
  notebookId: number,
  nextStep: string
): void {
  prepareAuthenticatedLfsCheckout(checkoutDir, notebookId, 'receive', nextStep)
  const noPrompt = {
    env: { ...smudgeSkippedGitOptions().env, GIT_TERMINAL_PROMPT: '0' },
  }
  const incomplete =
    (verb: string) => (detail: string | undefined, status: number | null) =>
      `Notebook attachments are incomplete: failed to ${verb} current notebook attachments via Git LFS${
        detail ? `: ${detail}` : ` (exit code ${status})`
      }. Fix authorization or connectivity, then ${nextStep}.`
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
}
