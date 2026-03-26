import {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
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
 * - Output assertions (`outputAssertions`: non-interactive, chat history, current guidance, recall /stop)
 * - Access-token removal assertions
 * - Execution (installation, interactive, access-token)
 * - Backend (bundle, install script)
 * - Setup (config dir, interactive session lifecycle)
 */
export const cli = {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
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
