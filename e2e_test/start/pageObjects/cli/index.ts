import { nonInteractiveOutput } from './outputAssertions'
import { installation } from './execution'
import { backend } from './backend'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive install runs)
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  installation,
  backend,
}
