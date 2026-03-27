import React from 'react'
import type { Key } from 'ink'
import {
  filterCommandsByPrefix,
  getTabCompletion,
  interactiveDocs,
} from '../commands/help.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userVisibleOutcomeFromCommandError,
} from '../fetchAbort.js'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  getInteractiveFetchWaitLine,
  runInteractiveFetchWait,
} from '../interactiveFetchWait.js'
import { saveUserInputHistory } from '../userInputHistoryFile.js'
import { getConfigDir } from '../configDir.js'
import { maskInteractiveInputLineForStorage } from '../inputHistoryMask.js'
import {
  afterBareSlashEscape,
  appendUserInputHistoryLine,
  clearLiveCommandLine,
  deleteBeforeCaret,
  insertIntoDraft,
  onArrowDown,
  onArrowUp,
  replaceLiveCommandDraft,
  ttyArrowKeyUsesSlashSuggestionCycle,
  type InteractiveCommandInput,
} from '../interactiveCommandInput.js'
import type { RecallInkConfirmChoice } from '../interactions/recallYesNo.js'
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
  commandTurnBufferAppendError,
  commandTurnBufferAppendLog,
  commandTurnBufferAppendUserNotice,
  emptyCommandTurnBuffer,
  pastMessagesAppendCliAssistantBlock,
  pastMessagesCommitUserLine,
  pastMessagesFlushCommandTurnIfNonEmpty,
} from '../shell/pastMessagesModel.js'
import {
  applyShellSessionPatch,
  type ShellSessionState,
} from '../shell/shellSessionState.js'
import {
  listAccessTokens,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
  type AccessTokenEntry,
} from '../commands/accessToken.js'
import type {
  AccessTokenPickerAction,
  CliAssistantMessageTone,
  OutputAdapter,
} from '../types.js'
import { hasInteractiveSlashCompletions } from '../slashCompletion.js'
import {
  isInkSubmitPressed,
  ShellSessionRoot,
  type ShellSessionInkHandlers,
} from './ShellSessionRoot.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'

type InkKeyWithName = Key & { name?: string }

type InteractiveAppProps = {
  initialSession: ShellSessionState
  deps: InteractiveShellDeps
  latestSessionRef: React.MutableRefObject<ShellSessionState>
  terminalContract: InteractiveAppTerminalContract
  ttyOutputRef: React.MutableRefObject<OutputAdapter | null>
  exitSession: () => void
}

function isEscapeInkKey(key: Key): boolean {
  const withName = key as InkKeyWithName
  return !!(key.escape || withName.name === 'escape')
}

export type InteractiveAppTerminalContract = {
  writeCurrentPromptLine: (msg: string) => void
  beginCurrentPrompt: () => void
  onShellSessionLayoutEffect: (
    session: ShellSessionState,
    deps: InteractiveShellDeps
  ) => void
  writeExitFarewellBlock: (options: {
    previousInputContent: string | undefined
    outputLines: readonly string[]
    tone: CliAssistantMessageTone
  }) => void
  signalConfirmInputReady: () => void
  writeCtrlCExitNewline: () => void
}

export function InteractiveApp({
  initialSession,
  deps,
  latestSessionRef,
  terminalContract,
  ttyOutputRef,
  exitSession,
}: InteractiveAppProps): React.ReactElement {
  const {
    processInput,
    setPendingStopConfirmation,
    isInCommandSessionSubstate,
    exitCommandSession,
    getStopConfirmationYesOutcomeLines,
    getPlaceholderContext,
    getDefaultTokenLabel,
    TOKEN_LIST_COMMANDS,
    getNumberedChoiceListChoices,
  } = deps

  const writeCurrentPromptLine = (msg: string) =>
    terminalContract.writeCurrentPromptLine(msg)

  const doBeginCurrentPrompt = () => terminalContract.beginCurrentPrompt()

  const [session, setSession] = React.useReducer(
    (_state: ShellSessionState, next: ShellSessionState) => next,
    initialSession
  )

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

  const bumpTtyContractEpoch = React.useCallback(() => {
    patch((s) => ({ ...s, ttyContractEpoch: s.ttyContractEpoch + 1 }))
  }, [patch])

  React.useLayoutEffect(() => {
    latestSessionRef.current = session
    terminalContract.onShellSessionLayoutEffect(session, deps)
  }, [session])

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

  function commitTokenListResult(
    message: string,
    tone: CliAssistantMessageTone = 'plain'
  ) {
    patch((s) => ({
      ...s,
      tokenSelection: null,
      commandInput: clearLiveCommandLine(s.commandInput),
      pastMessages: pastMessagesAppendCliAssistantBlock(
        s.pastMessages,
        [message],
        tone
      ),
    }))
  }

  function beginTokenSelection(
    command: string,
    action: AccessTokenPickerAction,
    items: AccessTokenEntry[]
  ): void {
    const defaultLabel = getDefaultTokenLabel()
    patch((s) => ({
      ...s,
      tokenSelection: {
        items,
        command,
        action,
        highlightIndex: Math.max(
          0,
          items.findIndex((token) => token.label === defaultLabel)
        ),
      },
    }))
  }

  function resetCommandTurnBuffer(): void {
    patch((s) => ({ ...s, commandTurn: emptyCommandTurnBuffer() }))
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

  function resetLiveLineDraftAndSlashSuggestions(): void {
    patch((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
      highlightIndex: 0,
      suggestionsDismissed: false,
    }))
  }

  const enterStopConfirmationFromEsc = (): void => {
    setPendingStopConfirmation(true)
    patch((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
    }))
  }

  const ttyOutputHolder = React.useRef<OutputAdapter | null>(null)
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

  const signalConfirmInputReady = () =>
    terminalContract.signalConfirmInputReady()

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
        commitHistoryOutput(getStopConfirmationYesOutcomeLines())
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
      getPlaceholderContext(false) === RECALL_SESSION_YES_NO_PLACEHOLDER
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
          if (
            getPlaceholderContext(!!latestSessionRef.current.tokenSelection) !==
            RECALL_SESSION_YES_NO_PLACEHOLDER
          ) {
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

  async function routeAccessTokenPickerInkStdin(
    input: string,
    key: Key
  ): Promise<void> {
    const activeTokenSelection = latestSessionRef.current.tokenSelection
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
        patch((s) => {
          if (!s.tokenSelection) return s
          return {
            ...s,
            tokenSelection: {
              ...s.tokenSelection,
              highlightIndex: cycleListSelectionIndex(
                s.tokenSelection.highlightIndex,
                listDispatch.delta,
                s.tokenSelection.items.length
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

      const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
      if (tokenSelect) {
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
          return
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
        beginTokenSelection(trimmedInput, tokenSelect.action, tokens)
        return
      }

      resetCommandTurnBuffer()
      if (isCommittedInteractiveInput(inputLine)) {
        if (
          getPlaceholderContext(!!latest.tokenSelection) !==
          RECALL_SESSION_YES_NO_PLACEHOLDER
        ) {
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

  const handlers: ShellSessionInkHandlers = {
    onInterrupt: () => {
      terminalContract.writeCtrlCExitNewline()
      exitSession()
    },
    onStopConfirmResult: handleStopConfirmDispatch,
    onSessionYesNoResult: handleSessionYesNoDispatch,
    onRecallMcqGuidanceKey: routeRecallMcqChoicesInkStdin,
    onTokenPickerGuidanceKey: routeAccessTokenPickerInkStdin,
    onCommandLineKey: handleCommandLineInkInput,
    onCommandLineTyping: applyCommandLineTypingFromInk,
    signalConfirmInputReady,
    onEnterStopConfirmationFromEsc: enterStopConfirmationFromEsc,
    whenInActiveRecallSession: isInCommandSessionSubstate,
  }

  return <ShellSessionRoot session={session} deps={deps} handlers={handlers} />
}
