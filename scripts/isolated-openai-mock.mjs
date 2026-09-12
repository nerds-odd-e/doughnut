/**
 * OpenAI adapter over the generic runner-owned Mountebank lifecycle
 * (`isolated-mountebank-mock.mjs`). OpenAI supplies its service label, its
 * canonical serving-port exclusion, and its Cypress `expose`/task keys; the
 * owned process, management listener, port allocation, verification and
 * stop/kill behavior come from the generic adapter with unchanged observable
 * behavior. Does not call start_mb.sh (shared-port adoption / swallowed
 * failure).
 */
import { spawn } from 'node:child_process'
import { allocatePrivateOpenAiMockPorts } from './isolated-mountebank-mock-ports.mjs'
import {
  observeOwnedMockChild,
  startOwnedMountebankMock,
} from './isolated-mountebank-mock.mjs'

export {
  SHARED_MOUNTEBANK_MANAGEMENT_PORT,
  SHARED_OPEN_AI_SERVING_PORT,
} from './isolated-mountebank-mock-ports.mjs'
export {
  GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK,
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
} from './open-ai-mock-endpoint-expose-keys.mjs'
export { assertOwnedMockListener } from './isolated-mountebank-mock-ownership.mjs'

export const OPEN_AI_SERVICE_LABEL = 'OpenAI'

/**
 * OpenAI-flavored failure observer for a private-mock child. Delegates to the
 * generic observer with the OpenAI service label so failure messages match
 * the original observable behavior.
 */
export function observePrivateMockChild(child) {
  return observeOwnedMockChild(child, { serviceLabel: OPEN_AI_SERVICE_LABEL })
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
export async function startPrivateOpenAiMock({
  checkoutRoot,
  allocation,
  spawnFn = spawn,
  allocatePortsFn = allocatePrivateOpenAiMockPorts,
} = {}) {
  return startOwnedMountebankMock({
    checkoutRoot,
    allocation,
    spawnFn,
    allocatePortsFn,
    serviceLabel: OPEN_AI_SERVICE_LABEL,
  })
}
