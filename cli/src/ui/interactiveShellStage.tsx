import React from 'react'
import type { Key } from 'ink'
import { cancelInteractiveFetchWaitFor } from '../interactiveFetchWait.js'
import { maskInteractiveInputLineForStorage } from '../inputHistoryMask.js'
import {
  afterBareSlashEscape,
  clearLiveCommandLine,
  onArrowDown,
  onArrowUp,
  singleLineCommandDraft,
  type InteractiveCommandInput,
} from '../interactiveCommandInput.js'
import { isCommittedInteractiveInput } from '../renderer.js'
import {
  emptyCommandTurnBuffer,
  pastMessagesAppendCliAssistantBlock,
  pastMessagesCommitUserLine,
} from '../shell/pastMessagesModel.js'
import type { ShellSessionState } from '../shell/shellSessionState.js'
import type { CliAssistantMessageTone, OutputAdapter } from '../types.js'
import {
  isInkSubmitPressed,
  type ShellSessionInkHandlers,
} from './ShellSessionRoot.js'
import type { InteractiveAppTerminalContract } from './interactiveAppTerminalContract.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'
import type {
  AccessTokenListStageNavigation,
  TokenListSlashSubmitResult,
} from './accessTokenListStage.js'

type InkKeyWithName = Key & { name?: string }

function isEscapeInkKey(key: Key): boolean {
  const withName = key as InkKeyWithName
  return !!(key.escape || withName.name === 'escape')
}

export type InteractiveShellSharedContext = {
  deps: InteractiveShellDeps
  patch: (reducerPatch: (s: ShellSessionState) => ShellSessionState) => void
  latestSessionRef: React.MutableRefObject<ShellSessionState>
  terminalContract: InteractiveAppTerminalContract
  ttyOutputRef: React.MutableRefObject<OutputAdapter | null>
  ttyOutput: OutputAdapter
  exitSession: () => void
  commitHistoryOutput: (
    lines: readonly string[],
    tone?: CliAssistantMessageTone
  ) => void
  rememberCommittedLine: (raw: string) => void
  applyAccessTokenListNavigation: (nav: AccessTokenListStageNavigation) => void
}

export type InteractiveShellInkHandlers = Omit<
  ShellSessionInkHandlers,
  'onTokenPickerGuidanceKey'
>

export function useInteractiveShellStage(
  shared: InteractiveShellSharedContext,
  token: {
    tryHandleTokenListSlashSubmit: (
      trimmedInput: string,
      inputLine: string
    ) => TokenListSlashSubmitResult
  }
): { handlers: InteractiveShellInkHandlers } {
  const {
    deps,
    patch,
    latestSessionRef,
    terminalContract,
    ttyOutputRef,
    ttyOutput,
    exitSession,
    commitHistoryOutput,
    rememberCommittedLine,
    applyAccessTokenListNavigation,
  } = shared
  const { processInput } = deps

  const bumpTtyContractEpoch = React.useCallback(() => {
    patch((s) => ({ ...s, ttyContractEpoch: s.ttyContractEpoch + 1 }))
  }, [patch])

  ttyOutputRef.current = ttyOutput

  function resetCommandTurnBuffer(): void {
    patch((s) => ({ ...s, commandTurn: emptyCommandTurnBuffer() }))
  }

  function commitExitTurnToPastMessages(): void {
    const sessionAtExit = latestSessionRef.current
    const updatedPastMessages = pastMessagesAppendCliAssistantBlock(
      sessionAtExit.pastMessages,
      sessionAtExit.commandTurn.lines,
      sessionAtExit.commandTurn.tone
    )
    patch((s) => ({ ...s, pastMessages: updatedPastMessages }))

    const last = updatedPastMessages[updatedPastMessages.length - 1]
    const prev = updatedPastMessages[updatedPastMessages.length - 2]
    if (!last || last.role !== 'cli-assistant') return
    terminalContract.writeExitFarewellBlock({
      previousInputContent: prev?.role === 'user' ? prev.content : undefined,
      outputLines: last.lines,
      tone: last.tone ?? 'plain',
    })
  }

  async function runProcessInputTurn(
    effectiveLine: string
  ): Promise<'exited' | 'continued'> {
    if (await processInput(effectiveLine, ttyOutput, true)) {
      commitExitTurnToPastMessages()
      exitSession()
      return 'exited'
    }
    finishProcessInputTurnAfterAwait()
    return 'continued'
  }

  function finishProcessInputTurnAfterAwait(): void {
    const latest = latestSessionRef.current
    if (latest.commandTurn.lines.length > 0) {
      commitHistoryOutput(latest.commandTurn.lines, latest.commandTurn.tone)
    }
    if (latest.commandTurn.lines.length === 0) bumpTtyContractEpoch()
  }

  async function handleCommandLineInkInput(
    input: string,
    key: Key
  ): Promise<void> {
    const submitPressed = isInkSubmitPressed(key, input)
    const latest = latestSessionRef.current

    if (isEscapeInkKey(key) || input === '\u001b') {
      if (singleLineCommandDraft(latest.commandInput.lineDraft) === '/') {
        patch((s) => ({
          ...s,
          commandInput: afterBareSlashEscape(s.commandInput),
          highlightIndex: 0,
        }))
      }
      return
    }

    if (submitPressed) {
      const trimmedInput = latest.commandInput.lineDraft.trim()

      const inputLine = latest.commandInput.lineDraft
      patch((s) => ({
        ...s,
        commandInput: clearLiveCommandLine(s.commandInput),
      }))

      const slashResult = token.tryHandleTokenListSlashSubmit(
        trimmedInput,
        inputLine
      )
      if (slashResult.handled) {
        if (slashResult.navigation) {
          applyAccessTokenListNavigation(slashResult.navigation)
        }
        return
      }

      resetCommandTurnBuffer()
      if (isCommittedInteractiveInput(inputLine)) {
        patch((s) => ({
          ...s,
          pastMessages: pastMessagesCommitUserLine(
            s.pastMessages,
            maskInteractiveInputLineForStorage(inputLine)
          ),
        }))
        rememberCommittedLine(inputLine)
        if ((await runProcessInputTurn(inputLine)) === 'exited') return
        return
      }

      bumpTtyContractEpoch()
    } else if (key.upArrow || key.downArrow) {
      const dir = key.upArrow ? 'up' : 'down'
      const now = latestSessionRef.current
      const prevDraft = now.commandInput.lineDraft
      const nextCommandInput =
        dir === 'up'
          ? onArrowUp(now.commandInput)
          : onArrowDown(now.commandInput)
      patch((s) => ({
        ...s,
        commandInput: nextCommandInput,
        ...(nextCommandInput.lineDraft !== prevDraft
          ? { highlightIndex: 0, suggestionsDismissed: false }
          : {}),
      }))
    }
  }

  function applyCommandLineTypingFromInk(next: InteractiveCommandInput): void {
    const currentDraft = latestSessionRef.current.commandInput.lineDraft
    const normalizedNext =
      currentDraft.startsWith('/') &&
      currentDraft.endsWith(' ') &&
      next.lineDraft.startsWith('/') &&
      !next.lineDraft.startsWith(currentDraft)
        ? {
            ...next,
            lineDraft: `${currentDraft}${next.lineDraft.slice(1)}`,
            caretOffset:
              currentDraft.length + Math.max(0, next.caretOffset - 1),
          }
        : next
    const resetSlashPicker =
      normalizedNext.lineDraft !==
      latestSessionRef.current.commandInput.lineDraft
    patch((s) => ({
      ...s,
      commandInput: normalizedNext,
      ...(resetSlashPicker
        ? { highlightIndex: 0, suggestionsDismissed: false }
        : {}),
    }))
  }

  const handlers: InteractiveShellInkHandlers = {
    onInterrupt: () => {
      terminalContract.writeCtrlCExitNewline()
      exitSession()
    },
    onFetchWaitCancel: () => {
      cancelInteractiveFetchWaitFor(ttyOutput)
    },
    onCommandLineKey: handleCommandLineInkInput,
    onCommandLineTyping: applyCommandLineTypingFromInk,
  }

  return { handlers }
}
