import {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
} from './outputAssertions'
import {
  installation,
  nonInteractive,
  interactive,
  accessToken,
  gmail,
  backend,
} from './execution'
import { recallSession } from './recallSession'
import { removeToken } from './removeToken'

export const cli = {
  nonInteractiveOutput,
  historyOutput,
  historyInput,
  currentGuidance,
  installation,
  nonInteractive,
  interactive,
  accessToken,
  gmail,
  backend,
  recallSession,
  removeToken,
}
