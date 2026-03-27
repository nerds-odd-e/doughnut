import React from 'react'
import type { Key } from 'ink'
import {
  filterCommandsByPrefix,
  getTabCompletion,
  interactiveDocs,
} from '../commands/help.js'
import { cancelInteractiveFetchWaitFor } from '../interactiveFetchWait.js'
import { maskInteractiveInputLineForStorage } from '../inputHistoryMask.js'
import {
  afterBareSlashEscape,
  clearLiveCommandLine,
  deleteBeforeCaret,
  insertIntoDraft,
  onArrowDown,
  onArrowUp,
  replaceLiveCommandDraft,
  ttyArrowKeyUsesSlashSuggestionCycle,
  type InteractiveCommandInput,
} from '../interactiveCommandInput.js'
import {
  RECALL_STOP_CONFIRM_YES_LINES,
  type RecallInkConfirmChoice,
} from '../interactions/recallYesNo.js'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListKeyEventFromInk,
} from '../interactions/selectListInteraction.js'
import {
  isCommittedInteractiveInput,
  RECALL_SESSION_YES_NO_PLACEHOLDER,
} from '../renderer.js'
import {
  emptyCommandTurnBuffer,
  pastMessagesAppendCliAssistantBlock,
  pastMessagesCommitUserLine,
} from '../shell/pastMessagesModel.js'
import type { ShellSessionState } from '../shell/shellSessionState.js'
import type { CliAssistantMessageTone, OutputAdapter } from '../types.js'
import { hasInteractiveSlashCompletions } from '../slashCompletion.js'
import {
  isInkSubmitPressed,
  type ShellSessionInkHandlers,
} from './ShellSessionRoot.js'
import type { InteractiveAppTerminalContract } from './interactiveAppTerminalContract.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'
import type { TokenListSlashSubmitResult } from './accessTokenListStage.js'

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
  } = shared
  const {
    processInput,
    setPendingStopConfirmation,
    isInCommandSessionSubstate,
    exitCommandSession,
    getPlaceholderContext,
    getNumberedChoiceListChoices,
  } = deps

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

  const enterStopConfirmationFromEsc = (): void => {
    setPendingStopConfirmation(true)
    patch((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
    }))
  }

  async function handleStopConfirmDispatch(
    dispatch: RecallInkConfirmChoice
  ): Promise<void> {
    switch (dispatch.result) {
      case 'cancel':
        setPendingStopConfirmation(false)
        patch((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
        }))
        break
      case 'submit-yes':
        setPendingStopConfirmation(false)
        patch((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
          numberedChoiceHighlightIndex: 0,
        }))
        exitCommandSession()
        commitHistoryOutput(RECALL_STOP_CONFIRM_YES_LINES)
        break
      case 'submit-no':
        setPendingStopConfirmation(false)
        patch((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
        }))
        break
      default:
        break
    }
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
    const newSessionYesNo =
      getPlaceholderContext() === RECALL_SESSION_YES_NO_PLACEHOLDER
    const latest = latestSessionRef.current
    if (latest.commandTurn.lines.length > 0) {
      commitHistoryOutput(latest.commandTurn.lines, latest.commandTurn.tone)
    }
    if (newSessionYesNo || latest.commandTurn.lines.length === 0)
      bumpTtyContractEpoch()
  }

  async function handleSessionYesNoDispatch(
    dispatch: RecallInkConfirmChoice
  ): Promise<void> {
    switch (dispatch.result) {
      case 'cancel':
        break
      case 'submit-yes':
      case 'submit-no': {
        const effectiveLine = dispatch.result === 'submit-yes' ? 'y' : 'n'
        patch((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
          commandTurn: emptyCommandTurnBuffer(),
        }))
        if (isCommittedInteractiveInput(effectiveLine)) {
          if (getPlaceholderContext() !== RECALL_SESSION_YES_NO_PLACEHOLDER) {
            patch((s) => ({
              ...s,
              pastMessages: pastMessagesCommitUserLine(
                s.pastMessages,
                maskInteractiveInputLineForStorage(effectiveLine)
              ),
            }))
            rememberCommittedLine(effectiveLine)
          }
          if ((await runProcessInputTurn(effectiveLine)) === 'exited') return
        }
        break
      }
      default:
        break
    }
  }

  async function routeRecallMcqChoicesInkStdin(
    input: string,
    key: Key
  ): Promise<void> {
    const numberedChoices = getNumberedChoiceListChoices()
    if (numberedChoices === null) return
    const latest = latestSessionRef.current

    const ev = selectListKeyEventFromInk(
      input,
      key,
      latest.commandInput.lineDraft
    )
    const listDispatch = dispatchSelectListKey(
      ev,
      latest.numberedChoiceHighlightIndex,
      {
        kind: 'slash-and-number-or-highlight',
        choiceCount: numberedChoices.length,
      },
      'signal-escape'
    )
    switch (listDispatch.result) {
      case 'escape-signaled':
        enterStopConfirmationFromEsc()
        break
      case 'move-highlight':
        patch((s) => ({
          ...s,
          numberedChoiceHighlightIndex: cycleListSelectionIndex(
            s.numberedChoiceHighlightIndex,
            listDispatch.delta,
            numberedChoices.length
          ),
        }))
        break
      case 'submit-with-line': {
        const effectiveInput = listDispatch.lineForProcessInput
        const inputForHistory = latest.commandInput.lineDraft || effectiveInput
        patch((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
          numberedChoiceHighlightIndex: 0,
          commandTurn: emptyCommandTurnBuffer(),
          pastMessages: pastMessagesCommitUserLine(
            s.pastMessages,
            maskInteractiveInputLineForStorage(inputForHistory)
          ),
        }))
        if ((await runProcessInputTurn(effectiveInput)) === 'exited') return
        break
      }
      case 'edit-backspace':
        patch((s) => ({
          ...s,
          commandInput: deleteBeforeCaret(s.commandInput),
        }))
        break
      case 'edit-char':
        patch((s) => ({
          ...s,
          commandInput: insertIntoDraft(s.commandInput, listDispatch.char),
        }))
        break
      case 'redraw':
        bumpTtyContractEpoch()
        break
      default:
        bumpTtyContractEpoch()
        break
    }
  }

  async function handleCommandLineInkInput(
    input: string,
    key: Key
  ): Promise<void> {
    const submitPressed = isInkSubmitPressed(key, input)
    const latest = latestSessionRef.current

    if (isEscapeInkKey(key) || input === '\u001b') {
      if (isInCommandSessionSubstate()) {
        enterStopConfirmationFromEsc()
        return
      }
      if (hasInteractiveSlashCompletions(latest.commandInput.lineDraft)) {
        patch((s) => {
          const lastLine = s.commandInput.lineDraft
          let next = { ...s, highlightIndex: 0 }
          if (lastLine === '/') {
            next = {
              ...next,
              commandInput: afterBareSlashEscape(next.commandInput),
            }
          } else {
            next = { ...next, suggestionsDismissed: true }
          }
          return next
        })
      }
      return
    }

    if (submitPressed) {
      const trimmedInput = latest.commandInput.lineDraft.trim()

      if (hasInteractiveSlashCompletions(latest.commandInput.lineDraft)) {
        const pickIndex = latest.highlightIndex
        const filtered = filterCommandsByPrefix(
          interactiveDocs,
          latest.commandInput.lineDraft
        )
        const selected = filtered[pickIndex] ?? filtered[0]
        if (!selected) {
          return
        }
        const selectedCommand = `${selected.usage} `
        patch((s) => ({
          ...s,
          commandInput: replaceLiveCommandDraft(
            s.commandInput,
            selectedCommand
          ),
          highlightIndex: 0,
        }))
        return
      }

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
        return
      }

      resetCommandTurnBuffer()
      if (isCommittedInteractiveInput(inputLine)) {
        if (getPlaceholderContext() !== RECALL_SESSION_YES_NO_PLACEHOLDER) {
          patch((s) => ({
            ...s,
            pastMessages: pastMessagesCommitUserLine(
              s.pastMessages,
              maskInteractiveInputLineForStorage(inputLine)
            ),
          }))
          rememberCommittedLine(inputLine)
        }
        if ((await runProcessInputTurn(inputLine)) === 'exited') return
        return
      }

      if (deps.getNumberedChoiceListChoices() !== null) {
        patch((s) => ({ ...s, numberedChoiceHighlightIndex: 0 }))
      }
      bumpTtyContractEpoch()
    } else if (key.upArrow || key.downArrow) {
      const dir = key.upArrow ? 'up' : 'down'
      const now = latestSessionRef.current
      if (
        ttyArrowKeyUsesSlashSuggestionCycle(
          dir,
          now.commandInput,
          now.suggestionsDismissed,
          hasInteractiveSlashCompletions(now.commandInput.lineDraft)
        )
      ) {
        const filtered = filterCommandsByPrefix(
          interactiveDocs,
          now.commandInput.lineDraft
        )
        const delta = key.upArrow ? -1 : 1
        patch((s) => ({
          ...s,
          highlightIndex: cycleListSelectionIndex(
            s.highlightIndex,
            delta,
            filtered.length
          ),
        }))
      } else {
        const prevDraft = now.commandInput.lineDraft
        const nextCommandInput =
          dir === 'up'
            ? onArrowUp(now.commandInput, hasInteractiveSlashCompletions)
            : onArrowDown(now.commandInput, hasInteractiveSlashCompletions)
        patch((s) => ({
          ...s,
          commandInput: nextCommandInput,
          ...(nextCommandInput.lineDraft !== prevDraft
            ? { highlightIndex: 0, suggestionsDismissed: false }
            : {}),
        }))
      }
    } else if (key.tab) {
      const lastLine = latest.commandInput.lineDraft
      if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
        const { completed, count } = getTabCompletion(lastLine, interactiveDocs)
        if (count > 0 && completed !== lastLine) {
          patch((s) => ({
            ...s,
            commandInput: replaceLiveCommandDraft(s.commandInput, completed),
            highlightIndex: 0,
            suggestionsDismissed: false,
          }))
        }
      }
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
    onStopConfirmResult: handleStopConfirmDispatch,
    onSessionYesNoResult: handleSessionYesNoDispatch,
    onRecallMcqGuidanceKey: routeRecallMcqChoicesInkStdin,
    onCommandLineKey: handleCommandLineInkInput,
    onCommandLineTyping: applyCommandLineTypingFromInk,
    onEnterStopConfirmationFromEsc: enterStopConfirmationFromEsc,
    whenInActiveRecallSession: isInCommandSessionSubstate,
  }

  return { handlers }
}
