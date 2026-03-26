import React from 'react'
import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import { render, type Key } from 'ink'
import { formatVersionOutput } from '../version.js'
import {
  filterCommandsByPrefix,
  getTabCompletion,
  interactiveDocs,
} from '../help.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userVisibleOutcomeFromCommandError,
} from '../fetchAbort.js'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  cancelInteractiveFetchWaitFor,
  getInteractiveFetchWaitLine,
  runInteractiveFetchWait,
} from '../interactiveFetchWait.js'
import { saveCliCommandHistory } from '../cliCommandHistoryFile.js'
import { getConfigDir } from '../configDir.js'
import { maskInteractiveInputForHistory } from '../inputHistoryMask.js'
import {
  afterBareSlashEscape,
  appendCommittedCommand,
  applyLastLineEdit,
  caretOneLeft,
  caretOneRight,
  clearLiveCommandLine,
  deleteBeforeCaret,
  insertIntoDraft,
  onArrowDown,
  onArrowUp,
  ttyArrowKeyUsesSlashSuggestionCycle,
} from '../interactiveCommandInput.js'
import type { RecallInkConfirmChoice } from '../interactions/recallYesNo.js'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListKeyEventFromInk,
} from '../interactions/selectListInteraction.js'
import {
  getLastLine,
  getTerminalWidth,
  isCommittedInteractiveInput,
} from '../renderer.js'
import { interactiveTtyStdout } from './interactiveTtyStdout.js'
import {
  commandTurnBufferAppendError,
  commandTurnBufferAppendLog,
  commandTurnBufferAppendUserNotice,
  emptyCommandTurnBuffer,
  scrollbackAppendOutput,
  scrollbackCommitInputLine,
  scrollbackFlushCommandTurnIfNonEmpty,
} from '../shell/scrollbackModel.js'
import {
  applyShellSessionPatch,
  createInitialShellSessionState,
  type ShellSessionState,
} from '../shell/shellSessionState.js'
import type { AccessTokenEntry } from '../accessToken.js'
import type {
  AccessTokenPickerAction,
  ChatHistoryOutputTone,
  OutputAdapter,
} from '../types.js'
import {
  isAlternateLivePanel,
  isCommandPrefixWithSuggestions,
  isInkSubmitPressed,
  ShellSessionRoot,
  type ShellSessionInkHandlers,
} from '../ui/ShellSessionRoot.js'
import type { TTYDeps } from './ttyDeps.js'

type ReadlineKey = Pick<readline.Key, 'name' | 'shift' | 'ctrl' | 'meta'>

type TTYInput = NodeJS.ReadableStream & {
  setRawMode?: (mode: boolean) => void
  resume?: () => void
  setEncoding?: (encoding: BufferEncoding) => void
}

export function runInteractiveTtySession(stdin: TTYInput, deps: TTYDeps): void {
  const {
    processInput,
    isPendingStopConfirmation,
    setPendingStopConfirmation,
    isInCommandSessionSubstate,
    exitCommandSession,
    getStopConfirmationYesOutcomeLines,
    usesSessionYesNoInputChrome,
    getDefaultTokenLabel,
    listAccessTokens,
    removeAccessToken,
    removeAccessTokenCompletely,
    setDefaultTokenLabel,
    TOKEN_LIST_COMMANDS,
    getNumberedChoiceListChoices,
  } = deps

  const writeCurrentPromptLine = (msg: string) =>
    interactiveTtyStdout.greyCurrentPromptLine(msg)

  const doBeginCurrentPrompt = () => {
    interactiveTtyStdout.currentPromptSeparator(getTerminalWidth())
  }

  console.log(formatVersionOutput())
  console.log()

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  const noopOutput = new Writable({
    write(_chunk, _encoding, cb) {
      cb()
    },
  })
  const rl = readline.createInterface({
    input: stdin,
    output: noopOutput,
    escapeCodeTimeout: 50,
  })
  readline.emitKeypressEvents(stdin, rl)

  let session: ShellSessionState = createInitialShellSessionState()
  let shellInstance: ReturnType<typeof render> | null = null

  const stdinTty = stdin as TTYInput
  const innerSetRawMode = stdinTty.setRawMode?.bind(stdinTty)
  if (typeof innerSetRawMode === 'function') {
    stdinTty.setRawMode = (enable: boolean) => {
      if (!enable && isAlternateLivePanel(session, deps)) {
        return stdinTty
      }
      return innerSetRawMode(enable)
    }
  }

  function patch(p: (s: ShellSessionState) => ShellSessionState): void {
    session = applyShellSessionPatch(session, p)
  }

  function patchAndDraw(p: (s: ShellSessionState) => ShellSessionState): void {
    patch(p)
    drawBox()
  }

  function patchStdinForInk(
    stream: NodeJS.ReadableStream & { ref?: () => void; unref?: () => void }
  ): void {
    if (typeof stream.ref !== 'function')
      stream.ref = () => {
        /* noop — keep event loop alive */
      }
    if (typeof stream.unref !== 'function')
      stream.unref = () => {
        /* noop */
      }
  }

  function handleShellRendered(): void {
    if (getInteractiveFetchWaitLine() !== null) {
      interactiveTtyStdout.hideCursor()
      return
    }
    if (
      isPendingStopConfirmation() ||
      usesSessionYesNoInputChrome(!!session.tokenSelection)
    ) {
      interactiveTtyStdout.inputReadyOsc()
      return
    }
    if (getNumberedChoiceListChoices() !== null) {
      interactiveTtyStdout.inputReadyOsc()
      return
    }
    if (session.tokenSelection) {
      interactiveTtyStdout.inputReadyOsc()
      return
    }
    finalizeInteractiveLiveRegionPaint()
  }

  function finalizeInteractiveLiveRegionPaint(): void {
    interactiveTtyStdout.finalizeDefaultLiveAfterInk({
      lineDraft: session.commandInput.lineDraft,
      interactiveFetchWaitLine: getInteractiveFetchWaitLine(),
    })
  }

  function drawBox(): void {
    const stdinForInk = stdin as NodeJS.ReadableStream & {
      ref?: () => void
      unref?: () => void
    }
    patchStdinForInk(stdinForInk)
    const tree = React.createElement(ShellSessionRoot, {
      session,
      deps,
      handlers,
    })
    if (!shellInstance) {
      shellInstance = render(tree, {
        stdin: stdinForInk as NodeJS.ReadStream,
        stdout: process.stdout,
        patchConsole: false,
        exitOnCtrlC: false,
        maxFps: 0,
      })
    } else {
      shellInstance.rerender(tree)
    }
    handleShellRendered()
    if (isAlternateLivePanel(session, deps)) {
      stdinTty.ref?.()
      stdinTty.setRawMode?.(true)
    }
  }

  function rememberCommittedLine(raw: string): void {
    patch((s) => ({
      ...s,
      commandInput: {
        ...s.commandInput,
        committedCommands: appendCommittedCommand(
          s.commandInput.committedCommands,
          maskInteractiveInputForHistory(raw)
        ),
      },
    }))
    saveCliCommandHistory(
      getConfigDir(),
      session.commandInput.committedCommands
    )
  }

  function commitHistoryOutput(
    lines: readonly string[],
    tone: ChatHistoryOutputTone = 'plain',
    options?: { skipDrawBox?: boolean }
  ): void {
    patch((s) => ({
      ...s,
      chatHistory: scrollbackAppendOutput(s.chatHistory, lines, tone),
    }))
    if (!options?.skipDrawBox) {
      drawBox()
    }
  }

  function commitTokenListResult(
    message: string,
    tone: ChatHistoryOutputTone = 'plain'
  ) {
    patch((s) => ({
      ...s,
      tokenSelection: null,
      commandInput: clearLiveCommandLine(s.commandInput),
      chatHistory: scrollbackAppendOutput(s.chatHistory, [message], tone),
    }))
    drawBox()
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

  let ttyOutput: OutputAdapter

  function resetCommandTurnBuffer(): void {
    patch((s) => ({ ...s, commandTurn: emptyCommandTurnBuffer() }))
  }

  function flushCommandTurnToScrollbackBeforeFetchWait(): void {
    if (session.commandTurn.lines.length === 0) return
    const flushed = scrollbackFlushCommandTurnIfNonEmpty(
      session.chatHistory,
      session.commandTurn
    )
    patch((s) => ({
      ...s,
      chatHistory: flushed.history,
      commandTurn: flushed.turn,
    }))
    drawBox()
  }

  function commitExitTurnToScrollback(): void {
    patch((s) => ({
      ...s,
      chatHistory: scrollbackAppendOutput(
        s.chatHistory,
        s.commandTurn.lines,
        s.commandTurn.tone
      ),
    }))
    const ch = session.chatHistory
    const last = ch[ch.length - 1]
    const prev = ch[ch.length - 2]
    if (!last || last.type !== 'output') return
    const width = getTerminalWidth()
    const outTone = last.tone ?? 'plain'
    interactiveTtyStdout.exitFarewellBlock({
      width,
      previousInputContent: prev?.type === 'input' ? prev.content : undefined,
      outputLines: last.lines,
      tone: outTone,
    })
  }

  function resetLiveLineDraftAndSlashSuggestions(): void {
    patchAndDraw((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
      highlightIndex: 0,
      suggestionsDismissed: false,
    }))
  }

  const enterStopConfirmationFromEsc = (): void => {
    setPendingStopConfirmation(true)
    patchAndDraw((s) => ({
      ...s,
      commandInput: clearLiveCommandLine(s.commandInput),
    }))
  }

  const removeResizeListenerRef: { current: (() => void) | null } = {
    current: null,
  }
  const doExit = () => {
    removeResizeListenerRef.current?.()
    if (shellInstance) {
      shellInstance.unmount()
      shellInstance = null
    }
    interactiveTtyStdout.showCursor()
    rl.close()
    process.exit(0)
  }

  ttyOutput = {
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
        flushCommandTurnToScrollbackBeforeFetchWait()
        drawBox()
      } else {
        resetLiveLineDraftAndSlashSuggestions()
      }
    },
  }

  const signalConfirmInputReady = () => {
    interactiveTtyStdout.inputReadyOsc()
  }

  async function handleStopConfirmDispatch(
    dispatch: RecallInkConfirmChoice
  ): Promise<void> {
    switch (dispatch.result) {
      case 'cancel':
        setPendingStopConfirmation(false)
        patchAndDraw((s) => ({
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
        patchAndDraw((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
        }))
        break
      default:
        break
    }
  }

  function finishProcessInputTurnAfterAwait(): void {
    const newSessionYesNo = usesSessionYesNoInputChrome(false)
    if (session.commandTurn.lines.length > 0) {
      commitHistoryOutput(session.commandTurn.lines, session.commandTurn.tone, {
        skipDrawBox: newSessionYesNo,
      })
    } else if (!newSessionYesNo) {
      drawBox()
    }
    if (newSessionYesNo) {
      drawBox()
    }
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
          if (!usesSessionYesNoInputChrome(!!session.tokenSelection)) {
            patch((s) => ({
              ...s,
              chatHistory: scrollbackCommitInputLine(
                s.chatHistory,
                maskInteractiveInputForHistory(effectiveLine)
              ),
            }))
            rememberCommittedLine(effectiveLine)
          }
          if (await processInput(effectiveLine, ttyOutput, true)) {
            commitExitTurnToScrollback()
            doExit()
            return
          }
          finishProcessInputTurnAfterAwait()
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

    const ev = selectListKeyEventFromInk(
      input,
      key,
      session.commandInput.lineDraft
    )
    const listDispatch = dispatchSelectListKey(
      ev,
      session.numberedChoiceHighlightIndex,
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
        patchAndDraw((s) => ({
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
        const inputForHistory = session.commandInput.lineDraft || effectiveInput
        patch((s) => ({
          ...s,
          commandInput: clearLiveCommandLine(s.commandInput),
          numberedChoiceHighlightIndex: 0,
          commandTurn: emptyCommandTurnBuffer(),
          chatHistory: scrollbackCommitInputLine(
            s.chatHistory,
            maskInteractiveInputForHistory(inputForHistory)
          ),
        }))
        if (isCommittedInteractiveInput(inputForHistory)) {
          rememberCommittedLine(inputForHistory)
        }
        if (await processInput(effectiveInput, ttyOutput, true)) {
          commitExitTurnToScrollback()
          doExit()
          return
        }
        finishProcessInputTurnAfterAwait()
        break
      }
      case 'edit-backspace':
        patchAndDraw((s) => ({
          ...s,
          commandInput: deleteBeforeCaret(s.commandInput),
        }))
        break
      case 'edit-char':
        patchAndDraw((s) => ({
          ...s,
          commandInput: insertIntoDraft(s.commandInput, listDispatch.char),
        }))
        break
      case 'redraw':
        drawBox()
        break
      default:
        drawBox()
        break
    }
  }

  async function routeAccessTokenPickerInkStdin(
    input: string,
    key: Key
  ): Promise<void> {
    if (!session.tokenSelection) return

    const ev = selectListKeyEventFromInk(
      input,
      key,
      session.commandInput.lineDraft
    )
    const listDispatch = dispatchSelectListKey(
      ev,
      session.tokenSelection.highlightIndex,
      { kind: 'highlight-only' },
      'abort-list'
    )
    switch (listDispatch.result) {
      case 'abort-highlight-only-list':
        commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
        break
      case 'move-highlight':
        patchAndDraw((s) => {
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
          session.tokenSelection.items[listDispatch.index]!.label
        const action = session.tokenSelection.action
        let message = ''
        let tone: ChatHistoryOutputTone = 'plain'
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
    if (key.escape) {
      if (isInCommandSessionSubstate()) {
        enterStopConfirmationFromEsc()
        return
      }
      if (isCommandPrefixWithSuggestions(session.commandInput.lineDraft)) {
        patch((s) => {
          const lastLine = getLastLine(s.commandInput.lineDraft)
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
        drawBox()
      }
      return
    }
    if (submitPressed) {
      if (key.shift) {
        patchAndDraw((s) => ({
          ...s,
          commandInput: insertIntoDraft(s.commandInput, '\n'),
        }))
      } else {
        const trimmedInput = session.commandInput.lineDraft.trim()

        if (isCommandPrefixWithSuggestions(session.commandInput.lineDraft)) {
          const pickIndex = session.highlightIndex
          const filtered = filterCommandsByPrefix(
            interactiveDocs,
            getLastLine(session.commandInput.lineDraft)
          )
          const selectedCommand = `${filtered[pickIndex].usage} `
          patchAndDraw((s) => ({
            ...s,
            commandInput: applyLastLineEdit(s.commandInput, selectedCommand),
            highlightIndex: 0,
          }))
          return
        }

        const inputLine = session.commandInput.lineDraft
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
              chatHistory: scrollbackCommitInputLine(
                s.chatHistory,
                maskInteractiveInputForHistory(trimmedInput)
              ),
            }))
            rememberCommittedLine(trimmedInput)
            commitHistoryOutput(['No access tokens stored.'])
            return
          } else {
            if (isCommittedInteractiveInput(inputLine)) {
              patch((s) => ({
                ...s,
                chatHistory: scrollbackCommitInputLine(
                  s.chatHistory,
                  maskInteractiveInputForHistory(inputLine)
                ),
              }))
            }
            rememberCommittedLine(trimmedInput)
            beginTokenSelection(trimmedInput, tokenSelect.action, tokens)
          }
          drawBox()
          return
        }

        resetCommandTurnBuffer()
        if (isCommittedInteractiveInput(inputLine)) {
          if (!usesSessionYesNoInputChrome(!!session.tokenSelection)) {
            patch((s) => ({
              ...s,
              chatHistory: scrollbackCommitInputLine(
                s.chatHistory,
                maskInteractiveInputForHistory(inputLine)
              ),
            }))
            rememberCommittedLine(inputLine)
          }
          if (await processInput(inputLine, ttyOutput, true)) {
            commitExitTurnToScrollback()
            doExit()
            return
          }
          finishProcessInputTurnAfterAwait()
          return
        }
        if (deps.isNumberedChoiceListActive()) {
          patch((s) => ({ ...s, numberedChoiceHighlightIndex: 0 }))
        }
        if (inputLine.trim() === '' && shellInstance) {
          shellInstance.clear()
          shellInstance.unmount()
          shellInstance = null
        }
        drawBox()
      }
    } else if (key.backspace || key.delete) {
      const prevLen = session.commandInput.lineDraft.length
      patch((s) => ({
        ...s,
        commandInput: deleteBeforeCaret(s.commandInput),
      }))
      if (session.commandInput.lineDraft.length !== prevLen) {
        patch((s) => ({
          ...s,
          highlightIndex: 0,
          suggestionsDismissed: false,
        }))
      }
      drawBox()
    } else if (key.upArrow || key.downArrow) {
      const dir = key.upArrow ? 'up' : 'down'
      if (
        ttyArrowKeyUsesSlashSuggestionCycle(
          dir,
          session.commandInput,
          session.suggestionsDismissed,
          isCommandPrefixWithSuggestions(session.commandInput.lineDraft)
        )
      ) {
        const filtered = filterCommandsByPrefix(
          interactiveDocs,
          getLastLine(session.commandInput.lineDraft)
        )
        const delta = key.upArrow ? -1 : 1
        patchAndDraw((s) => ({
          ...s,
          highlightIndex: cycleListSelectionIndex(
            s.highlightIndex,
            delta,
            filtered.length
          ),
        }))
      } else {
        const prevDraft = session.commandInput.lineDraft
        patch((s) => ({
          ...s,
          commandInput:
            dir === 'up'
              ? onArrowUp(s.commandInput, isCommandPrefixWithSuggestions)
              : onArrowDown(s.commandInput, isCommandPrefixWithSuggestions),
        }))
        if (session.commandInput.lineDraft !== prevDraft) {
          patch((s) => ({
            ...s,
            highlightIndex: 0,
            suggestionsDismissed: false,
          }))
        }
        drawBox()
      }
    } else if (key.leftArrow) {
      patchAndDraw((s) => ({
        ...s,
        commandInput: caretOneLeft(s.commandInput),
      }))
    } else if (key.rightArrow) {
      patchAndDraw((s) => ({
        ...s,
        commandInput: caretOneRight(s.commandInput),
      }))
    } else if (key.home) {
      patchAndDraw((s) => ({
        ...s,
        commandInput: { ...s.commandInput, caretOffset: 0 },
      }))
    } else if (key.end) {
      patchAndDraw((s) => ({
        ...s,
        commandInput: {
          ...s.commandInput,
          caretOffset: s.commandInput.lineDraft.length,
        },
      }))
    } else if (key.tab) {
      const lastLine = getLastLine(session.commandInput.lineDraft)
      if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
        const { completed, count } = getTabCompletion(lastLine, interactiveDocs)
        if (count > 0 && completed !== lastLine) {
          patchAndDraw((s) => ({
            ...s,
            commandInput: applyLastLineEdit(s.commandInput, completed),
            highlightIndex: 0,
            suggestionsDismissed: false,
          }))
        }
      }
    } else if (input.length > 0 && !key.ctrl && !key.meta) {
      patchAndDraw((s) => ({
        ...s,
        commandInput: insertIntoDraft(s.commandInput, input),
        highlightIndex: 0,
        suggestionsDismissed: false,
      }))
    }
  }

  const handlers: ShellSessionInkHandlers = {
    onInterrupt: () => {
      interactiveTtyStdout.ctrlCExitNewline()
      doExit()
    },
    onStopConfirmResult: handleStopConfirmDispatch,
    onSessionYesNoResult: handleSessionYesNoDispatch,
    onRecallMcqGuidanceKey: routeRecallMcqChoicesInkStdin,
    onTokenPickerGuidanceKey: routeAccessTokenPickerInkStdin,
    onCommandLineKey: handleCommandLineInkInput,
    signalConfirmInputReady,
    onEnterStopConfirmationFromEsc: enterStopConfirmationFromEsc,
    whenInActiveRecallSession: isInCommandSessionSubstate,
  }

  drawBox()

  process.stdout.on('resize', drawBox)
  removeResizeListenerRef.current = () => process.stdout.off('resize', drawBox)

  /**
   * Readline `emitKeypressEvents` runs alongside Ink on the same stdin. Ink owns normal typing
   * and list navigation (`useInput` in live panels). This listener is only for:
   * - Ctrl+C (always; exit before other routing).
   * - Fetch-wait Esc cancel — {@link FetchWaitDisplay} has no `useInput`.
   * - Esc on MCQ / token list when keypress ordering means the list panel’s Ink handler is unreliable
   *   (Ink still handles Esc when it receives it; handlers are idempotent where both fire).
   * When the default command line is active, return immediately so readline does not handle command-line keys.
   */
  stdin.on('keypress', (_str, key: ReadlineKey) => {
    if (key.ctrl && key.name === 'c') {
      interactiveTtyStdout.ctrlCExitNewline()
      doExit()
      return
    }
    if (key.name === 'escape' && cancelInteractiveFetchWaitFor(ttyOutput)) {
      drawBox()
      return
    }
    if (!isAlternateLivePanel(session, deps)) {
      return
    }
    if (
      isPendingStopConfirmation() ||
      usesSessionYesNoInputChrome(!!session.tokenSelection)
    ) {
      return
    }
    if (key.name === 'escape' && getNumberedChoiceListChoices() !== null) {
      enterStopConfirmationFromEsc()
      return
    }
    if (key.name === 'escape' && session.tokenSelection) {
      commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
      return
    }
  })
}
