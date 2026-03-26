import {
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
  currentGuidance,
  recallSession,
  inputBoxTopBorder,
} from './outputAssertions'
import { removeToken } from './removeToken'
import { installation, interactive, accessToken } from './execution'
import { backend } from './backend'
import { setup } from './setup'

/**
 * CLI page objects. Domain ordering:
 * - Output assertions (`outputAssertions`: non-interactive, past messages, current guidance, recall /stop)
 * - Access-token removal assertions
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
  recallSession,
  removeToken,
  installation,
  interactive,
  accessToken,
  backend,
  setup,
}
