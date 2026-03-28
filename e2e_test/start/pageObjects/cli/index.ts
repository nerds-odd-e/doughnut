import { nonInteractiveOutput } from './outputAssertions'
import { installation } from './execution'
import { backend } from './backend'
import { interactiveCli } from './interactiveCli'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive install runs)
 * - `interactiveCli()`: current interactive transcript (`@cliInteractivePtyOutput`); PTY handle in plugin `interactiveCliPtySession`
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  interactiveCli,
  installation,
  backend,
}
