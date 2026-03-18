import {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
} from './outputAssertions'
import { recallSession } from './recallSession'
import { removeToken } from './removeToken'
import {
  installation,
  nonInteractive,
  interactive,
  accessToken,
  gmail,
} from './execution'
import { backend } from './backend'
import { setup } from './setup'

/**
 * CLI page objects. Domain ordering:
 * - Output sections (non-interactive, history, current guidance)
 * - Recall session assertions
 * - Access-token removal assertions
 * - Execution (installation, non-interactive, interactive, access-token, gmail)
 * - Backend (bundle, install script)
 * - Setup (config dir, interactive session lifecycle)
 */
export const cli = {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
  recallSession,
  removeToken,
  installation,
  nonInteractive,
  interactive,
  accessToken,
  gmail,
  backend,
  setup,
}
