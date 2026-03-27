import type { Dispatch, MutableRefObject, SetStateAction } from 'react'
import type { Key } from 'ink'
import {
  getDefaultTokenLabel,
  listAccessTokens,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
} from '../commands/accessToken.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userVisibleOutcomeFromCommandError,
} from '../fetchAbort.js'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  runInteractiveFetchWait,
} from '../interactiveFetchWait.js'
import { maskInteractiveInputLineForStorage } from '../inputHistoryMask.js'
import { clearLiveCommandLine } from '../interactiveCommandInput.js'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListKeyEventFromInk,
} from '../interactions/selectListInteraction.js'
import { isCommittedInteractiveInput } from '../renderer.js'
import {
  pastMessagesAppendCliAssistantBlock,
  pastMessagesCommitUserLine,
} from '../shell/pastMessagesModel.js'
import type {
  ShellSessionState,
  TokenSelectionState,
} from '../shell/shellSessionState.js'
import { TOKEN_LIST_COMMANDS } from '../shell/tokenListCommands.js'
import type { CliAssistantMessageTone, OutputAdapter } from '../types.js'
import type { AppStage } from './interactiveAppStage.js'

export type AccessTokenListStageContext = {
  patch: (reducerPatch: (s: ShellSessionState) => ShellSessionState) => void
  latestSessionRef: MutableRefObject<ShellSessionState>
  latestTokenPickerRef: MutableRefObject<TokenSelectionState | null>
  setAppStage: Dispatch<SetStateAction<AppStage>>
  ttyOutput: OutputAdapter
  commitHistoryOutput: (
    lines: readonly string[],
    tone?: CliAssistantMessageTone
  ) => void
  rememberCommittedLine: (raw: string) => void
}

export function useAccessTokenListStage(ctx: AccessTokenListStageContext): {
  onTokenPickerGuidanceKey: (input: string, key: Key) => Promise<void>
  tryHandleTokenListSlashSubmit: (
    trimmedInput: string,
    inputLine: string
  ) => boolean
} {
  const {
    patch,
    latestSessionRef,
    latestTokenPickerRef,
    setAppStage,
    ttyOutput,
    commitHistoryOutput,
    rememberCommittedLine,
  } = ctx

  function commitTokenListResult(
    message: string,
    tone: CliAssistantMessageTone = 'plain'
  ): void {
    setAppStage({ kind: 'shell' })
    patch((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
      pastMessages: pastMessagesAppendCliAssistantBlock(
        s.pastMessages,
        [message],
        tone
      ),
    }))
  }

  async function onTokenPickerGuidanceKey(
    input: string,
    key: Key
  ): Promise<void> {
    const activeTokenSelection = latestTokenPickerRef.current
    if (!activeTokenSelection) return

    const ev = selectListKeyEventFromInk(
      input,
      key,
      latestSessionRef.current.commandInput.lineDraft
    )
    const listDispatch = dispatchSelectListKey(
      ev,
      activeTokenSelection.highlightIndex,
      { kind: 'highlight-only' },
      'abort-list'
    )
    switch (listDispatch.result) {
      case 'abort-highlight-only-list':
        commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
        break
      case 'move-highlight':
        setAppStage((prev) => {
          if (prev.kind !== 'accessTokenList') return prev
          return {
            ...prev,
            picker: {
              ...prev.picker,
              highlightIndex: cycleListSelectionIndex(
                prev.picker.highlightIndex,
                listDispatch.delta,
                prev.picker.items.length
              ),
            },
          }
        })
        break
      case 'submit-highlight-index': {
        const selectedLabel =
          activeTokenSelection.items[listDispatch.index]!.label
        const action = activeTokenSelection.action
        let message = ''
        let tone: CliAssistantMessageTone = 'plain'
        if (action === 'set-default') {
          setDefaultTokenLabel(selectedLabel)
          message = `Default token set to: ${selectedLabel}`
        } else if (action === 'remove') {
          removeAccessToken(selectedLabel)
          message = `Token "${selectedLabel}" removed.`
        } else {
          try {
            await runInteractiveFetchWait(
              ttyOutput,
              INTERACTIVE_FETCH_WAIT_LINES.removeAccessTokenCompletely,
              (signal) => removeAccessTokenCompletely(selectedLabel, signal)
            )
            message = `Token "${selectedLabel}" removed locally and from server.`
          } catch (err) {
            const o = userVisibleOutcomeFromCommandError(err)
            message = o.text
            tone = o.tone
          }
        }
        commitTokenListResult(message, tone)
        break
      }
      default:
        commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
        break
    }
  }

  function tryHandleTokenListSlashSubmit(
    trimmedInput: string,
    inputLine: string
  ): boolean {
    const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
    if (!tokenSelect) return false

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
      return true
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
    const defaultLabel = getDefaultTokenLabel()
    setAppStage({
      kind: 'accessTokenList',
      picker: {
        items: tokens,
        command: trimmedInput,
        action: tokenSelect.action,
        highlightIndex: Math.max(
          0,
          tokens.findIndex((token) => token.label === defaultLabel)
        ),
      },
    })
    return true
  }

  return { onTokenPickerGuidanceKey, tryHandleTokenListSlashSubmit }
}
