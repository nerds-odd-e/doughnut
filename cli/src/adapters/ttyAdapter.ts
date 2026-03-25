import React from 'react'
import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import { render, type Key } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
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
  INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS,
  INTERACTIVE_FETCH_WAIT_LINES,
  cancelInteractiveFetchWaitFor,
  getInteractiveFetchWaitLine,
  runInteractiveFetchWait,
  type InteractiveFetchWaitLine,
} from '../interactiveFetchWait.js'
import {
  loadCliCommandHistory,
  saveCliCommandHistory,
} from '../cliCommandHistoryFile.js'
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
  emptyInteractiveCommandInput,
  insertIntoDraft,
  onArrowDown,
  onArrowUp,
  ttyArrowKeyUsesSlashSuggestionCycle,
} from '../interactiveCommandInput.js'
import {
  RECALL_STOP_CONFIRM_GUIDANCE_LINE,
  type SessionYesNoLineDispatchResult,
} from '../interactions/sessionYesNoInteraction.js'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
  selectListKeyEventFromInk,
} from '../interactions/selectListInteraction.js'
import {
  buildSuggestionLinesForInk,
  buildTokenListLines,
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
  getLastLine,
  getTerminalWidth,
  greyCurrentStageIndicatorLabel,
  interactiveFetchWaitStageIndicatorLine,
  isCommittedInteractiveInput,
  needsGapBeforeBox,
  recallMcqCurrentGuidanceLines,
  wrapTextToLines,
  type PlaceholderContext,
} from '../renderer.js'
import { interactiveTtyStdout } from './interactiveTtyStdout.js'
import type {
  AccessTokenPickerAction,
  AccessTokenPickerCommandConfig,
  ChatHistory,
  ChatHistoryOutputTone,
  OutputAdapter,
} from '../types.js'
import { CommandLineLivePanel } from '../ui/CommandLineLivePanel.js'
import { ConfirmLivePanel } from '../ui/ConfirmLivePanel.js'
import { FetchWaitDisplay } from '../ui/FetchWaitDisplay.js'
import { InteractiveShellDisplay } from '../ui/InteractiveShellDisplay.js'
import { NumberedChoiceListLivePanel } from '../ui/NumberedChoiceListLivePanel.js'
import { TokenListLivePanel } from '../ui/TokenListLivePanel.js'

export interface TTYDeps {
  processInput: (
    input: string,
    output?: OutputAdapter,
    interactiveUi?: boolean
  ) => Promise<boolean>
  isPendingStopConfirmation: () => boolean
  setPendingStopConfirmation: (value: boolean) => void
  isInCommandSessionSubstate: () => boolean
  exitCommandSession: () => void
  getStopConfirmationYesOutcomeLines: () => readonly string[]
  getStopConfirmationLiveView: (ctx: PlaceholderContext) => {
    promptLines: string[]
    placeholder: string
    guidance: string[]
  }
  isNumberedChoiceListActive: () => boolean
  getNumberedChoiceListChoices: () => readonly string[] | null
  getNumberedChoiceListCurrentPromptWrappedLines: (
    width: number
  ) => string[] | null
  usesSessionYesNoInputChrome: (inTokenList: boolean) => boolean
  getDefaultTokenLabel: () => AccessTokenLabel | undefined
  listAccessTokens: () => AccessTokenEntry[]
  removeAccessToken: (label: AccessTokenLabel) => boolean
  removeAccessTokenCompletely: (
    label: AccessTokenLabel,
    signal?: AbortSignal
  ) => Promise<void>
  setDefaultTokenLabel: (label: AccessTokenLabel) => void
  TOKEN_LIST_COMMANDS: Record<string, AccessTokenPickerCommandConfig>
  getPlaceholderContext: (inTokenList: boolean) => PlaceholderContext
}

function isInkSubmitPressed(key: Key, input: string): boolean {
  return key.return || input === '\n' || input === '\r'
}

type ReadlineKey = Pick<readline.Key, 'name' | 'shift' | 'ctrl' | 'meta'>

type TokenSelectionState = {
  items: AccessTokenEntry[]
  command: string
  action: AccessTokenPickerAction
  highlightIndex: number
}

/** One submitted command's buffered scrollback: lines + dominant tone for `commitHistoryOutput`. */
type BufferedCommandTurn = { lines: string[]; tone: ChatHistoryOutputTone }

/** Measured live region for one paint: props for {@link CommandLineLivePanel} and shell gap. */
type LiveRegionLayout = {
  currentPromptWrappedLines: string[]
  suggestionLines: string[]
  currentStageIndicatorLines: string[]
  placeholderContext: PlaceholderContext
  terminalWidth: number
}

/** Prompt + stage lines for `needsGapBeforeBox`; cheap vs full command-line guidance. */
type LiveRegionPromptStageSnapshot = {
  width: number
  placeholderContext: PlaceholderContext
  stopConfirmDisplay: {
    promptLines: string[]
    placeholder: string
    guidance: string[]
  } | null
  currentPromptWrappedLines: string[]
  currentStageIndicatorLines: string[]
}

type TTYInput = NodeJS.ReadableStream & {
  setRawMode?: (mode: boolean) => void
  resume?: () => void
  setEncoding?: (encoding: BufferEncoding) => void
}

export async function runTTY(stdin: TTYInput, deps: TTYDeps): Promise<void> {
  const {
    processInput,
    isPendingStopConfirmation,
    setPendingStopConfirmation,
    isInCommandSessionSubstate,
    exitCommandSession,
    getStopConfirmationYesOutcomeLines,
    getStopConfirmationLiveView,
    isNumberedChoiceListActive,
    getNumberedChoiceListChoices,
    getNumberedChoiceListCurrentPromptWrappedLines,
    usesSessionYesNoInputChrome,
    getDefaultTokenLabel,
    listAccessTokens,
    removeAccessToken,
    removeAccessTokenCompletely,
    setDefaultTokenLabel,
    TOKEN_LIST_COMMANDS,
    getPlaceholderContext,
  } = deps

  function currentStageIndicatorLinesForLiveRegion(
    waitLine: InteractiveFetchWaitLine | null,
    fetchWaitEllipsisTick: number,
    tokenPicker: AccessTokenPickerCommandConfig | undefined,
    sessionPayloadLoading: boolean
  ): string[] {
    if (waitLine != null) {
      return [
        interactiveFetchWaitStageIndicatorLine(waitLine, fetchWaitEllipsisTick),
      ]
    }
    if (tokenPicker != null) {
      return [greyCurrentStageIndicatorLabel(tokenPicker.stageIndicator)]
    }
    if (sessionPayloadLoading) {
      return [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
    }
    return []
  }

  const writeCurrentPromptLine = (msg: string) =>
    interactiveTtyStdout.greyCurrentPromptLine(msg)

  const doBeginCurrentPrompt = () => {
    interactiveTtyStdout.currentPromptSeparator(getTerminalWidth())
  }

  function isCommandPrefixWithSuggestions(lineDraft: string): boolean {
    const lastLine = getLastLine(lineDraft)
    if (!lastLine.startsWith('/') || lastLine.endsWith(' ')) return false
    const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
    return filtered.length > 0
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

  let chatHistory: ChatHistory = []
  let commandInput = {
    ...emptyInteractiveCommandInput(),
    committedCommands: loadCliCommandHistory(getConfigDir()),
  }
  let highlightIndex = 0
  let suggestionsDismissed = false
  let tokenSelection: TokenSelectionState | null = null
  let numberedChoiceHighlightIndex = 0
  let interactiveFetchWaitEllipsisTick = 0
  let interactiveFetchWaitRepaintTimer: ReturnType<typeof setInterval> | null =
    null

  // --- Ink shell: Static history + live panel (Phase H) ---
  let shellInstance: ReturnType<typeof render> | null = null

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
      usesSessionYesNoInputChrome(!!tokenSelection)
    ) {
      interactiveTtyStdout.inputReadyOsc()
      return
    }
    if (getNumberedChoiceListChoices() !== null) {
      interactiveTtyStdout.inputReadyOsc()
      return
    }
    if (tokenSelection) {
      interactiveTtyStdout.inputReadyOsc()
      return
    }
    finalizeInteractiveLiveRegionPaint()
  }

  function getLiveRegionPromptAndStageLines(): LiveRegionPromptStageSnapshot {
    const width = getTerminalWidth()
    const placeholderContext = getPlaceholderContext(!!tokenSelection)
    const stopConfirmDisplay = isPendingStopConfirmation()
      ? getStopConfirmationLiveView(placeholderContext)
      : null
    const numberedChoicePromptLines =
      getNumberedChoiceListCurrentPromptWrappedLines(width)
    const waitLine = getInteractiveFetchWaitLine()
    const tokenListConfig = tokenSelection
      ? TOKEN_LIST_COMMANDS[tokenSelection.command]
      : undefined
    let currentPromptWrappedLines: string[]
    if (waitLine) {
      currentPromptWrappedLines = []
    } else if (stopConfirmDisplay) {
      currentPromptWrappedLines = stopConfirmDisplay.promptLines
    } else {
      const currentPromptText = tokenListConfig?.currentPrompt
      if (
        !tokenSelection &&
        numberedChoicePromptLines !== null &&
        !isPendingStopConfirmation()
      ) {
        currentPromptWrappedLines = numberedChoicePromptLines
      } else if (currentPromptText) {
        currentPromptWrappedLines = wrapTextToLines(currentPromptText, width)
      } else {
        currentPromptWrappedLines = []
      }
    }
    const currentStageIndicatorLines = currentStageIndicatorLinesForLiveRegion(
      waitLine,
      interactiveFetchWaitEllipsisTick,
      tokenListConfig,
      isInCommandSessionSubstate()
    )
    return {
      width,
      placeholderContext,
      stopConfirmDisplay,
      currentPromptWrappedLines,
      currentStageIndicatorLines,
    }
  }

  function buildSuggestionLinesFromPromptStage(
    promptStage: LiveRegionPromptStageSnapshot
  ): string[] {
    const width = promptStage.width
    const numberedChoices = getNumberedChoiceListChoices()
    if (tokenSelection) {
      return buildTokenListLines(
        tokenSelection.items,
        getDefaultTokenLabel(),
        width,
        tokenSelection.highlightIndex
      )
    }
    if (promptStage.stopConfirmDisplay) {
      return promptStage.stopConfirmDisplay.guidance
    }
    if (numberedChoices !== null) {
      return recallMcqCurrentGuidanceLines(
        numberedChoices,
        numberedChoiceHighlightIndex,
        width
      )
    }
    return buildSuggestionLinesForInk(commandInput.lineDraft, highlightIndex, {
      forceCommandsHint:
        suggestionsDismissed &&
        isCommandPrefixWithSuggestions(commandInput.lineDraft),
    })
  }

  function measureLiveRegionLayoutFromSnapshot(
    promptStage: LiveRegionPromptStageSnapshot
  ): LiveRegionLayout {
    return {
      currentPromptWrappedLines: promptStage.currentPromptWrappedLines,
      suggestionLines: buildSuggestionLinesFromPromptStage(promptStage),
      currentStageIndicatorLines: promptStage.currentStageIndicatorLines,
      placeholderContext: promptStage.placeholderContext,
      terminalWidth: promptStage.width,
    }
  }

  function isAlternateLivePanel(): boolean {
    if (getInteractiveFetchWaitLine() !== null) return true
    if (isPendingStopConfirmation()) return true
    if (usesSessionYesNoInputChrome(!!tokenSelection)) return true
    if (getNumberedChoiceListChoices() !== null) return true
    if (tokenSelection) return true
    return false
  }

  const stdinTty = stdin as TTYInput
  const innerSetRawMode = stdinTty.setRawMode?.bind(stdinTty)
  if (typeof innerSetRawMode === 'function') {
    stdinTty.setRawMode = (enable: boolean) => {
      if (!enable && isAlternateLivePanel()) {
        return stdinTty
      }
      return innerSetRawMode(enable)
    }
  }

  function buildLivePanel(
    commandLineLayout: LiveRegionLayout | undefined
  ): React.ReactElement {
    const waitLine = getInteractiveFetchWaitLine()
    if (waitLine !== null) {
      return React.createElement(FetchWaitDisplay, {
        waitLine,
        ellipsisTick: interactiveFetchWaitEllipsisTick,
      })
    }
    if (isPendingStopConfirmation()) {
      const placeholderCtx = getPlaceholderContext(false)
      const view = getStopConfirmationLiveView(placeholderCtx)
      const stageLines = isInCommandSessionSubstate()
        ? [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
        : []
      return React.createElement(ConfirmLivePanel, {
        key: 'confirm-stop-recall',
        guidanceLines: [
          ...stageLines,
          ...view.promptLines,
          RECALL_STOP_CONFIRM_GUIDANCE_LINE,
        ],
        placeholderText: view.placeholder,
        emptySubmit: 'treat-as-no',
        escapeToNestedStopConfirm: false,
        isInCommandSessionSubstate,
        onNestedStopConfirm: enterStopConfirmationFromEsc,
        onInputReadySignal: signalConfirmInputReady,
        onInterrupt: () => {
          interactiveTtyStdout.ctrlCExitNewline()
          doExit()
        },
        onDispatchResult: (d) => handleStopConfirmDispatch(d),
      })
    }
    if (usesSessionYesNoInputChrome(!!tokenSelection)) {
      return React.createElement(ConfirmLivePanel, {
        key: 'confirm-session-yes-no',
        guidanceLines: [],
        placeholderText: 'y or n; /stop to exit recall',
        emptySubmit: 'treat-as-invalid',
        escapeToNestedStopConfirm: true,
        isInCommandSessionSubstate,
        onNestedStopConfirm: enterStopConfirmationFromEsc,
        onInputReadySignal: signalConfirmInputReady,
        onInterrupt: () => {
          interactiveTtyStdout.ctrlCExitNewline()
          doExit()
        },
        onDispatchResult: (d) => handleSessionYesNoDispatch(d),
      })
    }
    const numberedChoices = getNumberedChoiceListChoices()
    if (numberedChoices !== null) {
      const width = getTerminalWidth()
      const promptLines =
        getNumberedChoiceListCurrentPromptWrappedLines(width) ?? []
      return React.createElement(NumberedChoiceListLivePanel, {
        stageIndicatorLine: DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
        currentPromptLines: promptLines,
        choices: numberedChoices,
        highlightIndex: numberedChoiceHighlightIndex,
        lineDraft: commandInput.lineDraft,
        width,
        onInterrupt: () => {
          interactiveTtyStdout.ctrlCExitNewline()
          doExit()
        },
        onInkKey: (inp, ky) =>
          Promise.resolve(handleNumberedChoiceListInkInput(inp, ky)).catch(
            () => undefined
          ),
      })
    }
    if (tokenSelection) {
      const tokenListConfig = TOKEN_LIST_COMMANDS[tokenSelection.command]
      const width = getTerminalWidth()
      const promptLines = tokenListConfig?.currentPrompt
        ? wrapTextToLines(tokenListConfig.currentPrompt, width)
        : []
      const stageIndicatorLine = tokenListConfig
        ? greyCurrentStageIndicatorLabel(tokenListConfig.stageIndicator)
        : ''
      return React.createElement(TokenListLivePanel, {
        stageIndicatorLine,
        currentPromptLines: promptLines,
        items: tokenSelection.items,
        defaultLabel: getDefaultTokenLabel(),
        highlightIndex: tokenSelection.highlightIndex,
        onInterrupt: () => {
          interactiveTtyStdout.ctrlCExitNewline()
          doExit()
        },
        onInkKey: (inp, ky) =>
          Promise.resolve(handleTokenListInkInput(inp, ky)).catch(
            () => undefined
          ),
      })
    }
    const layout = commandLineLayout!
    return React.createElement(CommandLineLivePanel, {
      buffer: commandInput.lineDraft,
      caretOffset: commandInput.caretOffset,
      width: layout.terminalWidth,
      currentPromptWrappedLines: layout.currentPromptWrappedLines,
      suggestionLines: layout.suggestionLines,
      currentStageIndicatorLines: layout.currentStageIndicatorLines,
      placeholderContext: layout.placeholderContext,
      onCommandKey: (input, key) =>
        Promise.resolve(handleCommandLineInkInput(input, key)).catch(
          () => undefined
        ),
      onInterrupt: () => {
        interactiveTtyStdout.ctrlCExitNewline()
        doExit()
      },
    })
  }

  function buildShellTree(
    liveLeadingGap: boolean,
    commandLineLayout: LiveRegionLayout | undefined
  ): React.ReactElement {
    return React.createElement(InteractiveShellDisplay, {
      history: chatHistory,
      terminalWidth: getTerminalWidth(),
      liveLeadingGap,
      livePanel: buildLivePanel(commandLineLayout),
    })
  }

  function drawBox(): void {
    const stdinForInk = stdin as NodeJS.ReadableStream & {
      ref?: () => void
      unref?: () => void
    }
    patchStdinForInk(stdinForInk)
    const promptStage = getLiveRegionPromptAndStageLines()
    const liveLeadingGap = needsGapBeforeBox(
      chatHistory,
      promptStage.currentPromptWrappedLines,
      promptStage.currentStageIndicatorLines
    )
    const commandLineLayout = isAlternateLivePanel()
      ? undefined
      : measureLiveRegionLayoutFromSnapshot(promptStage)
    const tree = buildShellTree(liveLeadingGap, commandLineLayout)
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
    if (isAlternateLivePanel()) {
      stdinTty.ref?.()
      stdinTty.setRawMode?.(true)
    }
  }

  function rememberCommittedLine(raw: string): void {
    commandInput = {
      ...commandInput,
      committedCommands: appendCommittedCommand(
        commandInput.committedCommands,
        maskInteractiveInputForHistory(raw)
      ),
    }
    saveCliCommandHistory(getConfigDir(), commandInput.committedCommands)
  }

  function commitHistoryOutput(
    lines: readonly string[],
    tone: ChatHistoryOutputTone = 'plain',
    options?: { skipDrawBox?: boolean }
  ): void {
    chatHistory = [...chatHistory, { type: 'output', lines: [...lines], tone }]
    if (!options?.skipDrawBox) {
      drawBox()
    }
  }

  /** Ends token-list mode: outcome line in scrollback (slash command was committed when the picker opened). */
  function commitTokenListResult(
    message: string,
    tone: ChatHistoryOutputTone = 'plain'
  ) {
    tokenSelection = null
    commandInput = clearLiveCommandLine(commandInput)
    commitHistoryOutput([message], tone)
  }

  function beginTokenSelection(
    command: string,
    action: AccessTokenPickerAction,
    items: AccessTokenEntry[]
  ): void {
    const defaultLabel = getDefaultTokenLabel()
    tokenSelection = {
      items,
      command,
      action,
      highlightIndex: Math.max(
        0,
        items.findIndex((token) => token.label === defaultLabel)
      ),
    }
  }

  /** Buffered `processInput` stdout for one submitted command (numbered-choice path or normal Enter). */
  let commandTurn: BufferedCommandTurn = { lines: [], tone: 'plain' }
  let ttyOutput: OutputAdapter

  let handleCommandLineInkInput: (input: string, key: Key) => Promise<void>

  function resetCommandTurnBuffer(): void {
    commandTurn = { lines: [], tone: 'plain' }
  }

  /** Hardware cursor stays hidden; caret is reverse-video inside Ink. Then input-ready OSC when applicable. */
  function finalizeInteractiveLiveRegionPaint(): void {
    interactiveTtyStdout.finalizeDefaultLiveAfterInk({
      lineDraft: commandInput.lineDraft,
      interactiveFetchWaitLine: getInteractiveFetchWaitLine(),
    })
  }

  /** Before fetch-wait chrome paints and emits input-ready OSC, persist buffered `log` lines so PTY captures are not ahead of scrollback. */
  function flushCommandTurnToScrollbackBeforeFetchWait(): void {
    if (commandTurn.lines.length === 0) return
    commitHistoryOutput(commandTurn.lines, commandTurn.tone, {
      skipDrawBox: true,
    })
    resetCommandTurnBuffer()
    drawBox()
  }

  function commitExitTurnToScrollback(): void {
    chatHistory = [
      ...chatHistory,
      {
        type: 'output',
        lines: [...commandTurn.lines],
        tone: commandTurn.tone,
      },
    ]
    const last = chatHistory[chatHistory.length - 1]
    const prev = chatHistory[chatHistory.length - 2]
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

  function stopInteractiveFetchWaitRepaintTimer(): void {
    if (interactiveFetchWaitRepaintTimer) {
      clearInterval(interactiveFetchWaitRepaintTimer)
      interactiveFetchWaitRepaintTimer = null
    }
    interactiveFetchWaitEllipsisTick = 0
  }

  /** Clears the live input line and `/` command-picker state (used by `/clear` and when fetch-wait ends). */
  function resetLiveLineDraftAndSlashSuggestions(): void {
    commandInput = clearLiveCommandLine(commandInput)
    highlightIndex = 0
    suggestionsDismissed = false
  }

  const enterStopConfirmationFromEsc = (): void => {
    setPendingStopConfirmation(true)
    commandInput = clearLiveCommandLine(commandInput)
    drawBox()
  }

  ttyOutput = {
    log: (msg) => {
      commandTurn.tone = 'plain'
      commandTurn.lines.push(...msg.split('\n'))
    },
    logError: (err) => {
      const msg = err instanceof Error ? err.message : String(err)
      commandTurn.tone = 'error'
      commandTurn.lines.push(...msg.split('\n'))
    },
    logUserNotice: (msg) => {
      commandTurn.tone = 'userNotice'
      commandTurn.lines.push(...msg.split('\n'))
    },
    writeCurrentPrompt: writeCurrentPromptLine,
    beginCurrentPrompt: doBeginCurrentPrompt,
    clearAndRedraw: () => {
      interactiveTtyStdout.clearScreen()
      chatHistory = []
      resetLiveLineDraftAndSlashSuggestions()
      tokenSelection = null
      if (shellInstance) {
        shellInstance.unmount()
        shellInstance = null
      }
      if (isInCommandSessionSubstate()) exitCommandSession()
      setPendingStopConfirmation(false)
      numberedChoiceHighlightIndex = 0
      drawBox()
    },
    onInteractiveFetchWaitChanged: () => {
      stopInteractiveFetchWaitRepaintTimer()
      const activeWaitPrompt = getInteractiveFetchWaitLine()
      if (activeWaitPrompt) {
        flushCommandTurnToScrollbackBeforeFetchWait()
        drawBox()
        interactiveFetchWaitRepaintTimer = setInterval(() => {
          interactiveFetchWaitEllipsisTick =
            (interactiveFetchWaitEllipsisTick + 1) % 3
          drawBox()
        }, INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS)
      } else {
        resetLiveLineDraftAndSlashSuggestions()
        drawBox()
      }
    },
  }

  const signalConfirmInputReady = () => {
    interactiveTtyStdout.inputReadyOsc()
  }

  async function handleStopConfirmDispatch(
    dispatch: SessionYesNoLineDispatchResult
  ): Promise<void> {
    switch (dispatch.result) {
      case 'cancel':
        setPendingStopConfirmation(false)
        commandInput = clearLiveCommandLine(commandInput)
        drawBox()
        break
      case 'submit-yes':
        commandInput = clearLiveCommandLine(commandInput)
        setPendingStopConfirmation(false)
        exitCommandSession()
        numberedChoiceHighlightIndex = 0
        commitHistoryOutput(getStopConfirmationYesOutcomeLines())
        break
      case 'submit-no':
        commandInput = clearLiveCommandLine(commandInput)
        setPendingStopConfirmation(false)
        drawBox()
        break
      default:
        break
    }
  }

  async function handleSessionYesNoDispatch(
    dispatch: SessionYesNoLineDispatchResult
  ): Promise<void> {
    switch (dispatch.result) {
      case 'cancel':
        break
      case 'submit-yes':
      case 'submit-no': {
        const effectiveLine = dispatch.result === 'submit-yes' ? 'y' : 'n'
        commandInput = clearLiveCommandLine(commandInput)
        resetCommandTurnBuffer()
        if (isCommittedInteractiveInput(effectiveLine)) {
          if (!usesSessionYesNoInputChrome(!!tokenSelection)) {
            chatHistory = [
              ...chatHistory,
              {
                type: 'input',
                content: maskInteractiveInputForHistory(effectiveLine),
              },
            ]
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

  handleCommandLineInkInput = async (
    input: string,
    key: Key
  ): Promise<void> => {
    const submitPressed = isInkSubmitPressed(key, input)
    if (key.escape) {
      if (isInCommandSessionSubstate()) {
        enterStopConfirmationFromEsc()
        return
      }
      if (isCommandPrefixWithSuggestions(commandInput.lineDraft)) {
        highlightIndex = 0
        const lastLine = getLastLine(commandInput.lineDraft)
        if (lastLine === '/') {
          commandInput = afterBareSlashEscape(commandInput)
        } else {
          suggestionsDismissed = true
        }
        drawBox()
      }
      return
    }
    if (submitPressed) {
      if (key.shift) {
        commandInput = insertIntoDraft(commandInput, '\n')
        drawBox()
      } else {
        const trimmedInput = commandInput.lineDraft.trim()

        if (trimmedInput === '/clear') {
          ttyOutput.clearAndRedraw?.()
          return
        }

        if (isCommandPrefixWithSuggestions(commandInput.lineDraft)) {
          const filtered = filterCommandsByPrefix(
            interactiveDocs,
            getLastLine(commandInput.lineDraft)
          )
          const selectedCommand = `${filtered[highlightIndex].usage} `
          commandInput = applyLastLineEdit(commandInput, selectedCommand)
          highlightIndex = 0
          drawBox()
          return
        }

        const inputLine = commandInput.lineDraft
        commandInput = clearLiveCommandLine(commandInput)

        const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
        if (tokenSelect) {
          const tokens = listAccessTokens()
          if (tokens.length === 0) {
            chatHistory = [
              ...chatHistory,
              {
                type: 'input',
                content: maskInteractiveInputForHistory(trimmedInput),
              },
            ]
            rememberCommittedLine(trimmedInput)
            commitHistoryOutput(['No access tokens stored.'])
            return
          } else {
            if (isCommittedInteractiveInput(inputLine)) {
              chatHistory = [
                ...chatHistory,
                {
                  type: 'input',
                  content: maskInteractiveInputForHistory(inputLine),
                },
              ]
            }
            rememberCommittedLine(trimmedInput)
            beginTokenSelection(trimmedInput, tokenSelect.action, tokens)
          }
          drawBox()
          return
        }

        resetCommandTurnBuffer()
        if (isCommittedInteractiveInput(inputLine)) {
          if (!usesSessionYesNoInputChrome(!!tokenSelection)) {
            chatHistory = [
              ...chatHistory,
              {
                type: 'input',
                content: maskInteractiveInputForHistory(inputLine),
              },
            ]
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
        if (isNumberedChoiceListActive()) {
          numberedChoiceHighlightIndex = 0
        }
        if (inputLine.trim() === '' && shellInstance) {
          shellInstance.clear()
          shellInstance.unmount()
          shellInstance = null
        }
        drawBox()
      }
    } else if (key.backspace || key.delete) {
      const prevLen = commandInput.lineDraft.length
      commandInput = deleteBeforeCaret(commandInput)
      if (commandInput.lineDraft.length !== prevLen) {
        highlightIndex = 0
        suggestionsDismissed = false
      }
      drawBox()
    } else if (key.upArrow || key.downArrow) {
      const dir = key.upArrow ? 'up' : 'down'
      if (
        ttyArrowKeyUsesSlashSuggestionCycle(
          dir,
          commandInput,
          suggestionsDismissed,
          isCommandPrefixWithSuggestions(commandInput.lineDraft)
        )
      ) {
        const filtered = filterCommandsByPrefix(
          interactiveDocs,
          getLastLine(commandInput.lineDraft)
        )
        const delta = key.upArrow ? -1 : 1
        highlightIndex = cycleListSelectionIndex(
          highlightIndex,
          delta,
          filtered.length
        )
      } else {
        const prevDraft = commandInput.lineDraft
        commandInput =
          dir === 'up'
            ? onArrowUp(commandInput, isCommandPrefixWithSuggestions)
            : onArrowDown(commandInput, isCommandPrefixWithSuggestions)
        if (commandInput.lineDraft !== prevDraft) {
          highlightIndex = 0
          suggestionsDismissed = false
        }
      }
      drawBox()
    } else if (key.leftArrow) {
      commandInput = caretOneLeft(commandInput)
      drawBox()
    } else if (key.rightArrow) {
      commandInput = caretOneRight(commandInput)
      drawBox()
    } else if (key.home) {
      commandInput = { ...commandInput, caretOffset: 0 }
      drawBox()
    } else if (key.end) {
      commandInput = {
        ...commandInput,
        caretOffset: commandInput.lineDraft.length,
      }
      drawBox()
    } else if (key.tab) {
      const lastLine = getLastLine(commandInput.lineDraft)
      if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
        const { completed, count } = getTabCompletion(lastLine, interactiveDocs)
        if (count > 0 && completed !== lastLine) {
          commandInput = applyLastLineEdit(commandInput, completed)
          highlightIndex = 0
          suggestionsDismissed = false
          drawBox()
        }
      }
    } else if (input.length > 0 && !key.ctrl && !key.meta) {
      commandInput = insertIntoDraft(commandInput, input)
      highlightIndex = 0
      suggestionsDismissed = false
      drawBox()
    }
  }

  drawBox()

  process.stdout.on('resize', drawBox)
  const removeResizeListener = () => process.stdout.off('resize', drawBox)
  const doExit = () => {
    stopInteractiveFetchWaitRepaintTimer()
    removeResizeListener()
    if (shellInstance) {
      shellInstance.unmount()
      shellInstance = null
    }
    interactiveTtyStdout.showCursor()
    rl.close()
    process.exit(0)
  }

  function finishProcessInputTurnAfterAwait(): void {
    const newSessionYesNo = usesSessionYesNoInputChrome(false)
    if (commandTurn.lines.length > 0) {
      commitHistoryOutput(commandTurn.lines, commandTurn.tone, {
        skipDrawBox: newSessionYesNo,
      })
    } else if (!newSessionYesNo) {
      drawBox()
    }
    if (newSessionYesNo) {
      drawBox()
    }
  }

  async function handleNumberedChoiceListInkInput(
    input: string,
    key: Key
  ): Promise<void> {
    const numberedChoices = getNumberedChoiceListChoices()
    if (numberedChoices === null) return

    const ev = selectListKeyEventFromInk(input, key, commandInput.lineDraft)
    const listDispatch = dispatchSelectListKey(
      ev,
      numberedChoiceHighlightIndex,
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
        numberedChoiceHighlightIndex = cycleListSelectionIndex(
          numberedChoiceHighlightIndex,
          listDispatch.delta,
          numberedChoices.length
        )
        drawBox()
        break
      case 'submit-with-line': {
        const effectiveInput = listDispatch.lineForProcessInput
        const inputForHistory = commandInput.lineDraft || effectiveInput
        commandInput = clearLiveCommandLine(commandInput)
        numberedChoiceHighlightIndex = 0
        resetCommandTurnBuffer()
        chatHistory = [
          ...chatHistory,
          {
            type: 'input',
            content: maskInteractiveInputForHistory(inputForHistory),
          },
        ]
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
        commandInput = deleteBeforeCaret(commandInput)
        drawBox()
        break
      case 'edit-char':
        commandInput = insertIntoDraft(commandInput, listDispatch.char)
        drawBox()
        break
      case 'redraw':
        drawBox()
        break
      default:
        drawBox()
        break
    }
  }

  async function handleTokenListInkInput(
    input: string,
    key: Key
  ): Promise<void> {
    if (!tokenSelection) return

    const ev = selectListKeyEventFromInk(input, key, commandInput.lineDraft)
    const listDispatch = dispatchSelectListKey(
      ev,
      tokenSelection.highlightIndex,
      { kind: 'highlight-only' },
      'abort-list'
    )
    switch (listDispatch.result) {
      case 'abort-highlight-only-list':
        commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
        break
      case 'move-highlight':
        tokenSelection.highlightIndex = cycleListSelectionIndex(
          tokenSelection.highlightIndex,
          listDispatch.delta,
          tokenSelection.items.length
        )
        drawBox()
        break
      case 'submit-highlight-index': {
        const selectedLabel = tokenSelection.items[listDispatch.index]!.label
        const action = tokenSelection.action
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

  stdin.on('keypress', async (str: string | undefined, key: ReadlineKey) => {
    if (key.ctrl && key.name === 'c') {
      interactiveTtyStdout.ctrlCExitNewline()
      doExit()
      return
    }
    if (key.name === 'escape' && cancelInteractiveFetchWaitFor(ttyOutput)) {
      drawBox()
      return
    }
    if (!isAlternateLivePanel()) {
      return
    }
    if (
      isPendingStopConfirmation() ||
      usesSessionYesNoInputChrome(!!tokenSelection)
    ) {
      return
    }
    if (key.name === 'escape' && getNumberedChoiceListChoices() !== null) {
      enterStopConfirmationFromEsc()
      return
    }
    if (key.name === 'escape' && tokenSelection) {
      commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
      return
    }
    if (getNumberedChoiceListChoices() !== null || tokenSelection) {
      return
    }
  })
}
