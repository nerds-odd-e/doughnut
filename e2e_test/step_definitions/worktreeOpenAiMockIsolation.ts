/// <reference types="cypress" />
// @ts-check

import {
  WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY,
  WORKTREE_RESET_ISOLATION_BARRIER_AT,
  WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK,
  WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
} from '../../scripts/worktree-isolation-constants.mjs'
import {
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
  loadBrowserOpenAiMockEndpointOverrideFromTask,
  resolveOpenAiMockEndpointContext,
} from '../start/mock_services/openAiMockEndpointContext'

export type OpenAiMockIsolationProof = {
  suggestion: string
  requestMarker: string
  foreignRequestMarker?: string
}

export function fetchOpenAiMockIsolationProof() {
  return cy.task<OpenAiMockIsolationProof | null>(
    'openAiMockIsolationProofParams'
  )
}

export function worktreeResetIsolationTask(name: string) {
  cy.task(name, null, { timeout: WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS })
}

export function signalPeerSeededForWorktreeResetIsolation() {
  worktreeResetIsolationTask('worktreeResetIsolationAfterSeed')
}

export function openAiMockIsolationBarrierActive() {
  return (
    Cypress.expose(WORKTREE_RESET_ISOLATION_BARRIER_AT) ===
    WORKTREE_RESET_ISOLATION_BARRIER_AT_OPENAI_MOCK
  )
}

export function worktreeBrowserIsolationActive() {
  return Boolean(Cypress.expose(WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY))
}

function isolatedOpenAiMockActive() {
  return Boolean(Cypress.expose(ISOLATED_OPEN_AI_MOCK_ENV_KEY))
}

/** Load private mock endpoint (post before:run) and verify ownership before mutation. */
export function ensurePrivateOpenAiMockReady() {
  if (!worktreeBrowserIsolationActive()) {
    return
  }
  return loadBrowserOpenAiMockEndpointOverrideFromTask().then((endpoint) => {
    if (endpoint || isolatedOpenAiMockActive()) {
      cy.task(VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK)
      cy.log(
        `OpenAI mock ready at ${JSON.stringify(resolveOpenAiMockEndpointContext())}`
      )
    }
  })
}
