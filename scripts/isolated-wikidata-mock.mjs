/**
 * Wikidata adapter over the generic runner-owned Mountebank lifecycle
 * (`isolated-mountebank-mock.mjs`). Wikidata supplies its service label, its
 * canonical serving-port exclusion (5002), and its Cypress `expose`/task
 * keys; the owned process, management listener, port allocation,
 * verification and stop/kill behavior come from the generic adapter. The
 * adapter consumes an invocation-owned endpoint instead of Wikidata's
 * hardcoded 5002/default 2525. Does not call start_mb.sh (shared-port
 * adoption / swallowed failure).
 */
import { spawn } from 'node:child_process'
import { allocatePrivateWikidataMockPorts } from './isolated-mountebank-mock-ports.mjs'
import {
  observeOwnedMockChild,
  startOwnedMountebankMock,
} from './isolated-mountebank-mock.mjs'

export { SHARED_WIKIDATA_SERVING_PORT } from './isolated-mountebank-mock-ports.mjs'
export {
  GET_ISOLATED_WIKIDATA_MOCK_ENDPOINT_TASK,
  ISOLATED_WIKIDATA_MOCK_ENV_KEY,
  VERIFY_ISOLATED_WIKIDATA_MOCK_OWNERSHIP_TASK,
  WIKIDATA_MOCK_ENDPOINT_ENV_KEY,
} from './wikidata-mock-endpoint-expose-keys.mjs'
export { assertOwnedMockListener } from './isolated-mountebank-mock-ownership.mjs'

export const WIKIDATA_SERVICE_LABEL = 'Wikidata'

/**
 * Wikidata-flavored failure observer for a private-mock child. Delegates to
 * the generic observer with the Wikidata service label so failure messages
 * name the service.
 */
export function observePrivateWikidataMockChild(child) {
  return observeOwnedMockChild(child, { serviceLabel: WIKIDATA_SERVICE_LABEL })
}

/**
 * @returns {Promise<{
 *   endpoint: { managementUrl: string, servingPort: number },
 *   child: import('node:child_process').ChildProcess,
 *   verifyOwnership: () => Promise<true>,
 *   stop: () => Promise<void>,
 *   killSync: () => void,
 *   getFailure: () => Error | null,
 * }>}
 */
export async function startPrivateWikidataMock({
  checkoutRoot,
  allocation,
  spawnFn = spawn,
  allocatePortsFn = allocatePrivateWikidataMockPorts,
} = {}) {
  return startOwnedMountebankMock({
    checkoutRoot,
    allocation,
    spawnFn,
    allocatePortsFn,
    serviceLabel: WIKIDATA_SERVICE_LABEL,
  })
}
