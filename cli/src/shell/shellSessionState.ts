import type { AccessTokenEntry } from '../commands/accessToken.js'
import type { AccessTokenPickerAction } from '../types.js'
import {
  emptyInteractiveCommandInput,
  singleLineCommandDraft,
  type InteractiveCommandInput,
} from '../interactiveCommandInput.js'
import { loadUserInputHistory } from '../userInputHistoryFile.js'
import { getConfigDir } from '../configDir.js'
import {
  emptyCommandTurnBuffer,
  type CommandTurnBuffer,
} from './pastMessagesModel.js'
import type { PastMessages } from '../types.js'

export type TokenSelectionState = {
  items: AccessTokenEntry[]
  command: string
  action: AccessTokenPickerAction
  highlightIndex: number
}

export type ShellSessionState = {
  pastMessages: PastMessages
  commandInput: InteractiveCommandInput
  highlightIndex: number
  suggestionsDismissed: boolean
  commandTurn: CommandTurnBuffer
  /** Bumps when module-level fetch-wait (or similar) changes without other session fields; re-runs TTY layout effect. */
  ttyContractEpoch: number
}

export type ShellSessionPatch = (s: ShellSessionState) => ShellSessionState

export function applyShellSessionPatch(
  state: ShellSessionState,
  patch: ShellSessionPatch
): ShellSessionState {
  return patch(state)
}

export function createInitialShellSessionState(): ShellSessionState {
  return {
    pastMessages: [],
    commandInput: {
      ...emptyInteractiveCommandInput(),
      userInputHistoryLines: loadUserInputHistory(getConfigDir())
        .map((s) => singleLineCommandDraft(s).trim())
        .filter((s) => s.length > 0),
    },
    highlightIndex: 0,
    suggestionsDismissed: false,
    commandTurn: emptyCommandTurnBuffer(),
    ttyContractEpoch: 0,
  }
}
