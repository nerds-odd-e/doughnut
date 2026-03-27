import React from 'react'
import { saveUserInputHistory } from '../userInputHistoryFile.js'
import { getConfigDir } from '../configDir.js'
import { maskInteractiveInputLineForStorage } from '../inputHistoryMask.js'
import {
  appendUserInputHistoryLine,
  clearLiveCommandLine,
} from '../interactiveCommandInput.js'
import { getInteractiveFetchWaitLine } from '../interactiveFetchWait.js'
import {
  commandTurnBufferAppendError,
  commandTurnBufferAppendLog,
  commandTurnBufferAppendUserNotice,
  pastMessagesAppendCliAssistantBlock,
  pastMessagesFlushCommandTurnIfNonEmpty,
} from '../shell/pastMessagesModel.js'
import {
  applyShellSessionPatch,
  type ShellSessionState,
  type TokenSelectionState,
} from '../shell/shellSessionState.js'
import type { CliAssistantMessageTone, OutputAdapter } from '../types.js'
import {
  ShellSessionRoot,
  type ShellSessionInkHandlers,
} from './ShellSessionRoot.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'
import {
  useAccessTokenListStage,
  type AccessTokenListStageNavigation,
} from './accessTokenListStage.js'
import type { AppStage } from './interactiveAppStage.js'
import type { InteractiveAppTerminalContract } from './interactiveAppTerminalContract.js'
import {
  useInteractiveShellStage,
  type InteractiveShellSharedContext,
} from './interactiveShellStage.js'

export type { InteractiveAppTerminalContract } from './interactiveAppTerminalContract.js'

type InteractiveAppProps = {
  initialSession: ShellSessionState
  deps: InteractiveShellDeps
  latestSessionRef: React.MutableRefObject<ShellSessionState>
  terminalContract: InteractiveAppTerminalContract
  ttyOutputRef: React.MutableRefObject<OutputAdapter | null>
  exitSession: () => void
}

export function InteractiveApp({
  initialSession,
  deps,
  latestSessionRef,
  terminalContract,
  ttyOutputRef,
  exitSession,
}: InteractiveAppProps): React.ReactElement {
  const [session, setSession] = React.useReducer(
    (_state: ShellSessionState, next: ShellSessionState) => next,
    initialSession
  )

  const [appStage, setAppStage] = React.useState<AppStage>({ kind: 'shell' })

  const applyAccessTokenListNavigation = React.useCallback(
    (nav: AccessTokenListStageNavigation) => {
      if (nav.kind === 'shell') setAppStage({ kind: 'shell' })
      else setAppStage({ kind: 'accessTokenList', picker: nav.picker })
    },
    []
  )

  const latestTokenPickerRef = React.useRef<TokenSelectionState | null>(null)
  const ttyOutputHolder = React.useRef<OutputAdapter | null>(null)

  const patch = React.useCallback(
    (reducerPatch: (s: ShellSessionState) => ShellSessionState) => {
      const next = applyShellSessionPatch(
        latestSessionRef.current,
        reducerPatch
      )
      latestSessionRef.current = next
      setSession(next)
    },
    []
  )

  React.useLayoutEffect(() => {
    latestTokenPickerRef.current =
      appStage.kind === 'accessTokenList' ? appStage.picker : null
  }, [appStage])

  React.useLayoutEffect(() => {
    latestSessionRef.current = session
    terminalContract.onShellSessionLayoutEffect(session, deps)
  }, [session, deps, terminalContract])

  const writeCurrentPromptLine = (msg: string) =>
    terminalContract.writeCurrentPromptLine(msg)

  const doBeginCurrentPrompt = () => terminalContract.beginCurrentPrompt()

  function rememberCommittedLine(raw: string): void {
    if (!deps.shouldRecordCommittedLineInUserInputHistory()) return
    const nextHistory = appendUserInputHistoryLine(
      latestSessionRef.current.commandInput.userInputHistoryLines,
      maskInteractiveInputLineForStorage(raw)
    )
    patch((s) => ({
      ...s,
      commandInput: {
        ...s.commandInput,
        userInputHistoryLines: nextHistory,
      },
    }))
    saveUserInputHistory(getConfigDir(), nextHistory)
  }

  function commitHistoryOutput(
    lines: readonly string[],
    tone: CliAssistantMessageTone = 'plain'
  ): void {
    patch((s) => ({
      ...s,
      pastMessages: pastMessagesAppendCliAssistantBlock(
        s.pastMessages,
        lines,
        tone
      ),
    }))
  }

  function flushCommandTurnToPastMessagesBeforeFetchWait(): void {
    if (latestSessionRef.current.commandTurn.lines.length === 0) return
    const flushed = pastMessagesFlushCommandTurnIfNonEmpty(
      latestSessionRef.current.pastMessages,
      latestSessionRef.current.commandTurn
    )
    patch((s) => ({
      ...s,
      pastMessages: flushed.pastMessages,
      commandTurn: flushed.turn,
    }))
  }

  function resetLiveLineDraftAndSlashSuggestions(): void {
    patch((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
      highlightIndex: 0,
      suggestionsDismissed: false,
    }))
  }

  if (!ttyOutputHolder.current) {
    ttyOutputHolder.current = {
      log: (msg) => {
        patch((s) => ({
          ...s,
          commandTurn: commandTurnBufferAppendLog(s.commandTurn, msg),
        }))
      },
      logError: (err) => {
        patch((s) => ({
          ...s,
          commandTurn: commandTurnBufferAppendError(s.commandTurn, err),
        }))
      },
      logUserNotice: (msg) => {
        patch((s) => ({
          ...s,
          commandTurn: commandTurnBufferAppendUserNotice(s.commandTurn, msg),
        }))
      },
      writeCurrentPrompt: writeCurrentPromptLine,
      beginCurrentPrompt: doBeginCurrentPrompt,
      onInteractiveFetchWaitChanged: () => {
        const activeWaitPrompt = getInteractiveFetchWaitLine()
        if (activeWaitPrompt) {
          flushCommandTurnToPastMessagesBeforeFetchWait()
        } else {
          resetLiveLineDraftAndSlashSuggestions()
        }
        patch((s) => ({ ...s, ttyContractEpoch: s.ttyContractEpoch + 1 }))
      },
    }
  }
  const ttyOutput = ttyOutputHolder.current
  ttyOutputRef.current = ttyOutput

  const tokenStage = useAccessTokenListStage({
    patch,
    latestSessionRef,
    latestTokenPickerRef,
    ttyOutput,
    commitHistoryOutput,
    rememberCommittedLine,
  })

  const shellShared: InteractiveShellSharedContext = {
    deps,
    patch,
    latestSessionRef,
    terminalContract,
    ttyOutputRef,
    ttyOutput,
    exitSession,
    hasActiveTokenPicker: () => appStage.kind === 'accessTokenList',
    commitHistoryOutput,
    rememberCommittedLine,
    applyAccessTokenListNavigation,
  }

  const { handlers: shellHandlers } = useInteractiveShellStage(shellShared, {
    tryHandleTokenListSlashSubmit: tokenStage.tryHandleTokenListSlashSubmit,
  })

  const handlers: ShellSessionInkHandlers = {
    ...shellHandlers,
    onTokenPickerGuidanceKey: async (input, key) => {
      const nav = await tokenStage.handleTokenPickerKey(input, key)
      if (nav) applyAccessTokenListNavigation(nav)
    },
  }

  const tokenSelection =
    appStage.kind === 'accessTokenList' ? appStage.picker : null

  return (
    <ShellSessionRoot
      session={session}
      tokenSelection={tokenSelection}
      deps={deps}
      handlers={handlers}
    />
  )
}
