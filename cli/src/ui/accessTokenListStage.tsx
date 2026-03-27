import {
  useCallback,
  useLayoutEffect,
  useRef,
  useState,
  type MutableRefObject,
} from 'react'
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

/** Parent maps this to interactive app stage; this module does not call setState. */
export type AccessTokenListStageNavigation =
  | { kind: 'shell' }
  | { kind: 'picker'; picker: TokenSelectionState }

export type TokenListSlashSubmitResult =
  | { handled: false }
  | { handled: true; navigation?: AccessTokenListStageNavigation }

export type AccessTokenListStageContext = {
  patch: (reducerPatch: (s: ShellSessionState) => ShellSessionState) => void
  latestSessionRef: MutableRefObject<ShellSessionState>
  ttyOutput: OutputAdapter
  commitHistoryOutput: (
    lines: readonly string[],
    tone?: CliAssistantMessageTone
  ) => void
  rememberCommittedLine: (raw: string) => void
}

export function useAccessTokenListStage(ctx: AccessTokenListStageContext): {
  tokenSelection: TokenSelectionState | null
  handleTokenPickerKey: (
    input: string,
    key: Key
  ) => Promise<AccessTokenListStageNavigation | null>
  tryHandleTokenListSlashSubmit: (
    trimmedInput: string,
    inputLine: string
  ) => TokenListSlashSubmitResult
  applyAccessTokenListNavigation: (nav: AccessTokenListStageNavigation) => void
  hasActiveTokenPicker: () => boolean
  onTokenPickerGuidanceKey: (input: string, key: Key) => Promise<void>
} {
  const {
    patch,
    latestSessionRef,
    ttyOutput,
    commitHistoryOutput,
    rememberCommittedLine,
  } = ctx

  const [tokenSelection, setTokenSelection] =
    useState<TokenSelectionState | null>(null)
  const latestTokenPickerRef = useRef<TokenSelectionState | null>(null)

  useLayoutEffect(() => {
    latestTokenPickerRef.current = tokenSelection
  }, [tokenSelection])

  const applyAccessTokenListNavigation = useCallback(
    (nav: AccessTokenListStageNavigation) => {
      if (nav.kind === 'shell') setTokenSelection(null)
      else setTokenSelection(nav.picker)
    },
    []
  )

  const hasActiveTokenPicker = useCallback(
    () => tokenSelection !== null,
    [tokenSelection]
  )

  function patchSessionAfterTokenListClose(
    message: string,
    tone: CliAssistantMessageTone = 'plain'
  ): void {
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

  async function handleTokenPickerKey(
    input: string,
    key: Key
  ): Promise<AccessTokenListStageNavigation | null> {
    const activeTokenSelection = latestTokenPickerRef.current
    if (!activeTokenSelection) return null

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
        patchSessionAfterTokenListClose(
          CLI_USER_ABORTED_WAIT_MESSAGE,
          'userNotice'
        )
        return { kind: 'shell' }
      case 'move-highlight':
        return {
          kind: 'picker',
          picker: {
            ...activeTokenSelection,
            highlightIndex: cycleListSelectionIndex(
              activeTokenSelection.highlightIndex,
              listDispatch.delta,
              activeTokenSelection.items.length
            ),
          },
        }
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
        patchSessionAfterTokenListClose(message, tone)
        return { kind: 'shell' }
      }
      default:
        patchSessionAfterTokenListClose(
          CLI_USER_ABORTED_WAIT_MESSAGE,
          'userNotice'
        )
        return { kind: 'shell' }
    }
  }

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
    const defaultLabel = getDefaultTokenLabel()
    return {
      handled: true,
      navigation: {
        kind: 'picker',
        picker: {
          items: tokens,
          command: trimmedInput,
          action: tokenSelect.action,
          highlightIndex: Math.max(
            0,
            tokens.findIndex((token) => token.label === defaultLabel)
          ),
        },
      },
    }
  }

  async function onTokenPickerGuidanceKey(
    input: string,
    key: Key
  ): Promise<void> {
    const nav = await handleTokenPickerKey(input, key)
    if (nav) applyAccessTokenListNavigation(nav)
  }

  return {
    tokenSelection,
    handleTokenPickerKey,
    tryHandleTokenListSlashSubmit,
    applyAccessTokenListNavigation,
    hasActiveTokenPicker,
    onTokenPickerGuidanceKey,
  }
}
