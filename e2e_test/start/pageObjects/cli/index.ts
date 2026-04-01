import { nonInteractiveOutput } from './outputAssertions'
import { installation } from './execution'
import { backend } from './backend'
import { interactiveCli } from './interactiveCli'
import { removeToken } from './removeToken'
import { ttyAssertTerminal } from './ttyAssertTerminal'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive install runs)
 * - `ttyAssertTerminal()`: PTY `cy.task` wrappers + transcript assertions (Phase 1.5)
 * - `interactiveCli()`: domain helpers on top of the same PTY session
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  ttyAssertTerminal,
  interactiveCli,
  removeToken,
  installation,
  backend,
}
