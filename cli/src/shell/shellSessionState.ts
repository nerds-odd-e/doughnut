import type { AccessTokenEntry } from '../accessToken.js'
import type { AccessTokenPickerAction } from '../types.js'
import {
  emptyInteractiveCommandInput,
  type InteractiveCommandInput,
} from '../interactiveCommandInput.js'
import { loadCliCommandHistory } from '../cliCommandHistoryFile.js'
import { getConfigDir } from '../configDir.js'
import {
  emptyCommandTurnBuffer,
  type CommandTurnBuffer,
} from './scrollbackModel.js'
import type { ChatHistory } from '../types.js'

export type TokenSelectionState = {
  items: AccessTokenEntry[]
  command: string
  action: AccessTokenPickerAction
  highlightIndex: number
}

export type ShellSessionState = {
  chatHistory: ChatHistory
  commandInput: InteractiveCommandInput
  highlightIndex: number
  suggestionsDismissed: boolean
  tokenSelection: TokenSelectionState | null
  numberedChoiceHighlightIndex: number
  commandTurn: CommandTurnBuffer
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
    chatHistory: [],
    commandInput: {
      ...emptyInteractiveCommandInput(),
      committedCommands: loadCliCommandHistory(getConfigDir()),
    },
    highlightIndex: 0,
    suggestionsDismissed: false,
    tokenSelection: null,
    numberedChoiceHighlightIndex: 0,
    commandTurn: emptyCommandTurnBuffer(),
  }
}
