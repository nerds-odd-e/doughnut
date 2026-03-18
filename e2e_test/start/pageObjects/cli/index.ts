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
  backend,
} from './execution'

export const cli = {
  // Output sections (non-interactive, history output/input, current guidance)
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
  // Recall session state
  recallSession,
  // Access token removal
  removeToken,
  // Execution (install, run, interactive, access-token, gmail)
  installation,
  nonInteractive,
  interactive,
  accessToken,
  gmail,
  backend,
}
