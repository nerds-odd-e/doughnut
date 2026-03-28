import {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
} from './outputAssertions'
import { installation, interactive } from './execution'
import { backend } from './backend'
import { setup } from './setup'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive, past messages)
 * - Execution (installation, interactive)
 * - Backend (bundle, install script)
 * - Setup (config dir, interactive session lifecycle)
 */
export const cli = {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  installation,
  interactive,
  backend,
  setup,
}
