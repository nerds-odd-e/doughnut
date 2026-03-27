import { listAccessTokens } from '../commands/accessToken.js'
import { maskInteractiveInputLineForStorage } from '../inputHistoryMask.js'
import { isCommittedInteractiveInput } from '../renderer.js'
import { pastMessagesCommitUserLine } from '../shell/pastMessagesModel.js'
import type { ShellSessionState } from '../shell/shellSessionState.js'
import { TOKEN_LIST_COMMANDS } from '../shell/tokenListCommands.js'
import type { CliAssistantMessageTone } from '../types.js'
import { useTokenPicker } from './tokenPicker.js'

export type TokenListSlashSubmitResult = { handled: false } | { handled: true }

export type AccessTokenListStageContext = {
  patch: (reducerPatch: (s: ShellSessionState) => ShellSessionState) => void
  commitHistoryOutput: (
    lines: readonly string[],
    tone?: CliAssistantMessageTone
  ) => void
  rememberCommittedLine: (raw: string) => void
}

export function useAccessTokenListStage(ctx: AccessTokenListStageContext): {
  tryHandleTokenListSlashSubmit: (
    trimmedInput: string,
    inputLine: string
  ) => TokenListSlashSubmitResult
} {
  const { patch, commitHistoryOutput, rememberCommittedLine } = ctx

  const { openPickerFromSlashCommand } = useTokenPicker()

  function tryHandleTokenListSlashSubmit(
    trimmedInput: string,
    inputLine: string
  ): TokenListSlashSubmitResult {
    const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
    if (!tokenSelect) return { handled: false }

    const tokens = listAccessTokens()
    if (tokens.length === 0) {
      patch((s) => ({
        ...s,
        pastMessages: pastMessagesCommitUserLine(
          s.pastMessages,
          maskInteractiveInputLineForStorage(trimmedInput)
        ),
      }))
      rememberCommittedLine(trimmedInput)
      commitHistoryOutput(['No access tokens stored.'])
      return { handled: true }
    }
    if (isCommittedInteractiveInput(inputLine)) {
      patch((s) => ({
        ...s,
        pastMessages: pastMessagesCommitUserLine(
          s.pastMessages,
          maskInteractiveInputLineForStorage(inputLine)
        ),
      }))
    }
    rememberCommittedLine(trimmedInput)
    openPickerFromSlashCommand({
      items: tokens,
      command: trimmedInput,
      action: tokenSelect.action,
    })
    return { handled: true }
  }

  return {
    tryHandleTokenListSlashSubmit,
  }
}
