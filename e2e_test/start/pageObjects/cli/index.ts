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

/** CLI page objects. Grouped by domain: output sections → recall → access-token removal → execution → backend. */
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
}
