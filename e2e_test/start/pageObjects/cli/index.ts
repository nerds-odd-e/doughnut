import {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  currentGuidance,
  inputBoxTopBorder,
} from './outputAssertions'
import { installation, interactive, accessToken } from './execution'
import { backend } from './backend'
import { setup } from './setup'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive, past messages, current guidance)
 * - Execution (installation, interactive, access-token)
 * - Backend (bundle, install script)
 * - Setup (config dir, interactive session lifecycle)
 */
export const cli = {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  currentGuidance,
  inputBoxTopBorder,
  installation,
  interactive,
  accessToken,
  backend,
  setup,
}
