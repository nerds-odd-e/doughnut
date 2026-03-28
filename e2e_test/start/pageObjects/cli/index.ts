import {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
} from './outputAssertions'
import { installation } from './execution'
import { backend } from './backend'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive, past messages)
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  installation,
  backend,
}
