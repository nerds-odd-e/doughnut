import {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
  inputBoxTopBorder,
} from './outputAssertions'
import { recallSession } from './recallSession'
import { removeToken } from './removeToken'
import { installation, interactive, accessToken } from './execution'
import { backend } from './backend'
import { setup } from './setup'

/**
 * CLI page objects. Domain ordering:
 * - Output sections (non-interactive, history, current guidance)
 * - Recall session assertions
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
