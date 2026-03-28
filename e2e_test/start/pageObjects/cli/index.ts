import {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  inputBoxTopBorder,
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
  inputBoxTopBorder,
  installation,
  interactive,
  backend,
  setup,
}
