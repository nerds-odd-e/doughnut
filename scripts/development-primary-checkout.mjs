import { isLinkedGitWorktree } from './browser-worktree-isolation.mjs'

/** Refuse a Development command (e.g. `pnpm dev`) in a Git linked worktree. */
export function refuseDevelopmentInLinkedWorktree(
  checkoutRoot,
  command,
  refusal
) {
  if (isLinkedGitWorktree(checkoutRoot)) {
    throw new Error(
      `Development (\`${command}\`) is only supported in the primary checkout. ` +
        `This checkout uses linked worktree isolation; ${refusal}. Resources were left unchanged.`
    )
  }
}
