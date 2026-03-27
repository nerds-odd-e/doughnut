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
} from '../shell/shellSessionState.js'
import type { CliAssistantMessageTone, OutputAdapter } from '../types.js'
import {
  ShellSessionRoot,
  type ShellSessionInkHandlers,
} from './ShellSessionRoot.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'
import { useAccessTokenListStage } from './accessTokenListStage.js'
import type { InteractiveAppTerminalContract } from './interactiveAppTerminalContract.js'
import {
  useInteractiveShellStage,
  type InteractiveShellSharedContext,
} from './interactiveShellStage.js'
import { TokenPickerProvider, useTokenPicker } from './tokenPicker.js'

export type { InteractiveAppTerminalContract } from './interactiveAppTerminalContract.js'

type InteractiveAppProps = {
  initialSession: ShellSessionState
  deps: InteractiveShellDeps
  latestSessionRef: React.MutableRefObject<ShellSessionState>
  terminalContract: InteractiveAppTerminalContract
  ttyOutputRef: React.MutableRefObject<OutputAdapter | null>
  exitSession: () => void
}

type InteractiveAppShellProps = {
  session: ShellSessionState
  deps: InteractiveShellDeps
  latestSessionRef: React.MutableRefObject<ShellSessionState>
  terminalContract: InteractiveAppTerminalContract
  ttyOutputRef: React.MutableRefObject<OutputAdapter | null>
  ttyOutput: OutputAdapter
  exitSession: () => void
  patch: (reducerPatch: (s: ShellSessionState) => ShellSessionState) => void
  commitHistoryOutput: (
    lines: readonly string[],
    tone?: CliAssistantMessageTone
  ) => void
  rememberCommittedLine: (raw: string) => void
}

function InteractiveAppShell({
  session,
  deps,
  latestSessionRef,
  terminalContract,
  ttyOutputRef,
  ttyOutput,
  exitSession,
  patch,
  commitHistoryOutput,
  rememberCommittedLine,
}: InteractiveAppShellProps): React.ReactElement {
  const tokenStage = useAccessTokenListStage({
    patch,
    commitHistoryOutput,
    rememberCommittedLine,
  })

  const { onTokenPickerGuidanceKey } = useTokenPicker()

  const shellShared: InteractiveShellSharedContext = {
    deps,
    patch,
    latestSessionRef,
    terminalContract,
    ttyOutputRef,
    ttyOutput,
    exitSession,
    commitHistoryOutput,
    rememberCommittedLine,
  }

  const { handlers: shellHandlers } = useInteractiveShellStage(shellShared, {
    tryHandleTokenListSlashSubmit: tokenStage.tryHandleTokenListSlashSubmit,
  })

  const handlers: ShellSessionInkHandlers = {
    ...shellHandlers,
    onTokenPickerGuidanceKey,
  }

  return <ShellSessionRoot session={session} deps={deps} handlers={handlers} />
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

  return (
    <TokenPickerProvider
      patch={patch}
      latestSessionRef={latestSessionRef}
      ttyOutput={ttyOutput}
    >
      <InteractiveAppShell
        session={session}
        deps={deps}
        latestSessionRef={latestSessionRef}
        terminalContract={terminalContract}
        ttyOutputRef={ttyOutputRef}
        ttyOutput={ttyOutput}
        exitSession={exitSession}
        patch={patch}
        commitHistoryOutput={commitHistoryOutput}
        rememberCommittedLine={rememberCommittedLine}
      />
    </TokenPickerProvider>
  )
}
