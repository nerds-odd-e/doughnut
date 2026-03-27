import {
  createContext,
  useCallback,
  useContext,
  useLayoutEffect,
  useRef,
  useState,
  type MutableRefObject,
  type ReactElement,
  type ReactNode,
} from 'react'
import type { Key } from 'ink'
import {
  getDefaultTokenLabel,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
} from '../commands/accessToken.js'
import type { AccessTokenEntry } from '../commands/accessToken.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userVisibleOutcomeFromCommandError,
} from '../fetchAbort.js'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  runInteractiveFetchWait,
} from '../interactiveFetchWait.js'
import { clearLiveCommandLine } from '../interactiveCommandInput.js'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListKeyEventFromInk,
} from '../interactions/selectListInteraction.js'
import { pastMessagesAppendCliAssistantBlock } from '../shell/pastMessagesModel.js'
import type {
  ShellSessionState,
  TokenSelectionState,
} from '../shell/shellSessionState.js'
import type {
  AccessTokenPickerAction,
  CliAssistantMessageTone,
  OutputAdapter,
} from '../types.js'

type TokenPickerNavigation =
  | { kind: 'shell' }
  | { kind: 'picker'; picker: TokenSelectionState }

export type TokenPickerSlashOpenPayload = {
  items: AccessTokenEntry[]
  command: string
  action: AccessTokenPickerAction
}

type TokenPickerContextValue = {
  tokenSelection: TokenSelectionState | null
  openPickerFromSlashCommand: (payload: TokenPickerSlashOpenPayload) => void
  onTokenPickerGuidanceKey: (input: string, key: Key) => Promise<void>
}

const TokenPickerContext = createContext<TokenPickerContextValue | null>(null)

export function useTokenPicker(): TokenPickerContextValue {
  const ctx = useContext(TokenPickerContext)
  if (!ctx) {
    throw new Error('useTokenPicker must be used within TokenPickerProvider')
  }
  return ctx
}

type TokenPickerProviderProps = {
  patch: (reducerPatch: (s: ShellSessionState) => ShellSessionState) => void
  latestSessionRef: MutableRefObject<ShellSessionState>
  ttyOutput: OutputAdapter
  children: ReactNode
}

export function TokenPickerProvider({
  patch,
  latestSessionRef,
  ttyOutput,
  children,
}: TokenPickerProviderProps): ReactElement {
  const [tokenSelection, setTokenSelection] =
    useState<TokenSelectionState | null>(null)
  const latestTokenPickerRef = useRef<TokenSelectionState | null>(null)

  useLayoutEffect(() => {
    latestTokenPickerRef.current = tokenSelection
  }, [tokenSelection])

  const applyAccessTokenListNavigation = useCallback(
    (nav: TokenPickerNavigation) => {
      if (nav.kind === 'shell') setTokenSelection(null)
      else setTokenSelection(nav.picker)
    },
    []
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
  ): Promise<TokenPickerNavigation | null> {
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

  const openPickerFromSlashCommand = useCallback(
    (payload: TokenPickerSlashOpenPayload) => {
      const defaultLabel = getDefaultTokenLabel()
      setTokenSelection({
        items: payload.items,
        command: payload.command,
        action: payload.action,
        highlightIndex: Math.max(
          0,
          payload.items.findIndex((token) => token.label === defaultLabel)
        ),
      })
    },
    []
  )

  async function onTokenPickerGuidanceKey(
    input: string,
    key: Key
  ): Promise<void> {
    const nav = await handleTokenPickerKey(input, key)
    if (nav) applyAccessTokenListNavigation(nav)
  }

  return (
    <TokenPickerContext.Provider
      value={{
        tokenSelection,
        openPickerFromSlashCommand,
        onTokenPickerGuidanceKey,
      }}
    >
      {children}
    </TokenPickerContext.Provider>
  )
}
