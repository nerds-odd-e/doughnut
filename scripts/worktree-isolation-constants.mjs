/**
 * Shared worktree isolation constants (Cypress expose keys, barrier env, timeouts).
 * Keep this module free of node:* imports — Cypress esbuild may bundle it for the browser.
 */

/** Cypress.expose flag set when this checkout runs under worktree browser isolation. */
export const WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY =
  'WORKTREE_BROWSER_ISOLATION'

export const WORKTREE_RESET_ISOLATION_BARRIER_DIR =
  'WORKTREE_RESET_ISOLATION_BARRIER_DIR'
export const WORKTREE_RESET_ISOLATION_ROLE = 'WORKTREE_RESET_ISOLATION_ROLE'
export const WORKTREE_RESET_ISOLATION_BARRIER_AT =
  'WORKTREE_RESET_ISOLATION_BARRIER_AT'
export const WORKTREE_RESET_ISOLATION_PEER_ROLE = 'peer'
export const WORKTREE_RESET_ISOLATION_RESETTER_ROLE = 'resetter'
export const WORKTREE_RESET_ISOLATION_BARRIER_AT_FIXTURE = 'fixture'
export const WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK = 'openai-mock'
export const WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS = 180_000
export const OPENAI_MOCK_ISOLATION_SUGGESTION =
  'OPENAI_MOCK_ISOLATION_SUGGESTION'
export const OPENAI_MOCK_ISOLATION_REQUEST_MARKER =
  'OPENAI_MOCK_ISOLATION_REQUEST_MARKER'
