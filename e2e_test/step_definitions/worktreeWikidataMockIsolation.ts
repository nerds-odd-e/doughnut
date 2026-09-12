/// <reference types="cypress" />
// @ts-check

import { WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY } from '../../scripts/worktree-isolation-constants.mjs'
import {
  ISOLATED_WIKIDATA_MOCK_ENV_KEY,
  VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK,
  loadBrowserWikidataMockEndpointOverrideFromTask,
  resolveWikidataMockEndpointContext,
} from '../start/mock_services/wikidataMockEndpointContext'

export function worktreeBrowserIsolationActive() {
  return Boolean(Cypress.expose(WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY))
}

function isolatedWikidataMockActive() {
  return Boolean(Cypress.expose(ISOLATED_WIKIDATA_MOCK_ENV_KEY))
}

/**
 * Load the private Wikidata mock endpoint (posted by the runner before:run)
 * and verify ownership before the `@usingMockedWikidataService` hook installs
 * stubs on it. No-op outside an isolated worktree (primary/CI keep shared
 * 2525/5002 defaults). No service is started here — the runner owns startup.
 */
export function ensurePrivateWikidataMockReady() {
  if (!worktreeBrowserIsolationActive()) {
    return
  }
  return loadBrowserWikidataMockEndpointOverrideFromTask().then((endpoint) => {
    if (endpoint || isolatedWikidataMockActive()) {
      cy.task(VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK)
      cy.log(
        `Wikidata mock ready at ${JSON.stringify(resolveWikidataMockEndpointContext())}`
      )
    }
  })
}
