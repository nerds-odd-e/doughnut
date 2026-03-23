import React from 'react'
import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import { render } from 'ink'
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
import type {
  SessionYesNoLineDispatchResult,
  SessionYesNoLineEmptySubmit,
  SessionYesNoLineKeyEvent,
} from '../interactions/recallSessionConfirmInteraction.js'
import {
  cycleListSelectionIndex,
  dispatchSelectListKey,
} from '../interactions/selectListInteraction.js'
import {
  applyChatHistoryOutputTone,
  buildBoxLines,
  buildCurrentPromptSeparator,
  buildLiveRegionLines,
  buildSuggestionLines,
  buildTokenListLines,
  CLEAR_SCREEN,
  countPromptBlockLinesAboveInputBoxTop,
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
  getLastLine,
  getTerminalWidth,
  GREY,
  greyCurrentStageIndicatorLabel,
  HIDE_CURSOR,
  interactiveFetchWaitStageIndicatorLine,
  INTERACTIVE_INPUT_READY_OSC,
  interactiveInputReadyOscSuffix,
  isCommittedInteractiveInput,
  isGreyDisabledInputChrome,
  needsGapBeforeBox,
  PROMPT,
  recallMcqCurrentGuidanceLines,
  renderFullDisplay,
  renderPastInput,
  SHOW_CURSOR,
  wrapTextToLines,
  type PlaceholderContext,
} from '../renderer.js'
import type {
  AccessTokenPickerAction,
  AccessTokenPickerCommandConfig,
  ChatHistory,
  ChatHistoryOutputTone,
  OutputAdapter,
} from '../types.js'
import { ConfirmDisplay } from '../ui/ConfirmDisplay.js'
import { McqDisplay } from '../ui/McqDisplay.js'
import { TokenListDisplay } from '../ui/TokenListDisplay.js'

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
  dispatchSessionYesNoKey: (
    e: SessionYesNoLineKeyEvent,
    emptySubmit: SessionYesNoLineEmptySubmit
  ) => SessionYesNoLineDispatchResult
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

/**
 * Tracks incremental repaint of the TTY live region (Current prompt + input box + Current guidance).
 * After {@link clearLiveRegionForRepaint}, the cursor sits on the top line of that region and
 * {@link LiveRegionPaintCursor.cursorUpStepsToLiveRegionTop} must be 0 before the next paint.
 */
type LiveRegionPaintCursor = {
  /**
   * CUU count from the input-line cursor row to the top line of the live region — same value
   * written at the end of the previous paint as `inputRowFromTop(...)`.
   */
  cursorUpStepsToLiveRegionTop: number
  /** Line count of the last painted live block; used to erase lines when the block shrinks. */
  lastPaintedLineCount: number
}

function clearLiveRegionForRepaint(cursor: LiveRegionPaintCursor): void {
  const up = cursor.cursorUpStepsToLiveRegionTop
  const n = cursor.lastPaintedLineCount
  if (up > 0) process.stdout.write(`\x1b[${up}A`)
  for (let i = 0; i < n; i++) {
    process.stdout.write('\r\x1b[2K')
    if (i < n - 1) process.stdout.write('\x1b[1B')
  }
  if (n > 1) process.stdout.write(`\x1b[${n - 1}A`)
  cursor.cursorUpStepsToLiveRegionTop = 0
}

function isSubmitKey(keyName: string): boolean {
  return keyName === 'return' || keyName === 'enter'
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

/**
 * Measured live region for one paint: Current prompt, input box, and Current guidance
 * (same inputs as {@link buildLiveRegionLines} / full-display tail).
 */
type LiveRegionLayout = {
  currentPromptWrappedLines: string[]
  currentPromptLines: number
  suggestionLines: string[]
  currentStageIndicatorLines: string[]
  placeholderContext: PlaceholderContext
  liveLines: string[]
  /** Line count of the live block (used for CUU cursor positioning). */
  liveLineCount: number
  /** Rows from live block top to the input line (prompt + box content lines). */
  inputLineRowInLiveBlock: number
  terminalWidth: number
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
    dispatchSessionYesNoKey,
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
    process.stdout.write(`${GREY}${msg}\x1b[0m\n`)

  const doBeginCurrentPrompt = () => {
    const sep = buildCurrentPromptSeparator(getTerminalWidth())
    process.stdout.write(`${sep}\n`)
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
  const livePaint: LiveRegionPaintCursor = {
    cursorUpStepsToLiveRegionTop: 0,
    lastPaintedLineCount: 0,
  }
  let tokenSelection: TokenSelectionState | null = null
  let numberedChoiceHighlightIndex = 0
  let interactiveFetchWaitEllipsisTick = 0
  let interactiveFetchWaitRepaintTimer: ReturnType<typeof setInterval> | null =
    null
  /**
   * True while `processInput` is executing. Used to suppress premature
   * INTERACTIVE_INPUT_READY_OSC from `onInteractiveFetchWaitChanged` drawBox
   * calls that fire inside `processInput` before `commitHistoryOutput` paints
   * the results.
   */
  let insideProcessInput = false

  // --- Ink island for confirm flows (Phase F1) ---
  // The ANSI keypress handler dispatches keys; Ink is display-only (no useInput).
  // MCQ and token list still use ANSI drawBox (Phase F2).
  let inkDisplayInstance: ReturnType<typeof render> | null = null
  let stopConfirmDraft = ''
  let stopConfirmHint = ''
  let sessionYesNoDraft = ''
  let sessionYesNoHint = ''

  function startOrUpdateInkDisplay(element: React.ReactElement): void {
    if (inkDisplayInstance) {
      inkDisplayInstance.rerender(element)
      return
    }
    clearLiveRegionForRepaint(livePaint)
    resetLivePaintCursor()
    // Ink calls stdin.ref() / stdin.unref() to manage the event loop.
    // Mock stdinpassed in tests don't have these; add noops to avoid errors.
    const stdinForInk = stdin as NodeJS.ReadableStream & {
      ref?: () => void
      unref?: () => void
    }
    if (typeof stdinForInk.ref !== 'function')
      stdinForInk.ref = () => {
        /* noop — keep event loop alive */
      }
    if (typeof stdinForInk.unref !== 'function')
      stdinForInk.unref = () => {
        /* noop */
      }
    inkDisplayInstance = render(element, {
      stdin: stdinForInk as NodeJS.ReadableStream,
      stdout: process.stdout,
      patchConsole: false,
      exitOnCtrlC: false,
      // maxFps:0 makes throttle interval 0ms so rerenders happen synchronously,
      // allowing test write spies to capture output immediately after rerender().
      maxFps: 0,
    })
  }

  function unmountInkDisplay(): void {
    if (inkDisplayInstance) {
      // Rerender with empty content before unmounting so the final write
      // (in debug/non-debug mode) doesn't repeat the previous UI state.
      inkDisplayInstance.rerender(React.createElement(React.Fragment))
      inkDisplayInstance.unmount()
      inkDisplayInstance = null
    }
  }

  function renderInkStopConfirm(): void {
    const placeholderCtx = getPlaceholderContext(false)
    const view = getStopConfirmationLiveView(placeholderCtx)
    // Include stage indicator if still in a command session (e.g., recall active)
    const stageLines = isInCommandSessionSubstate()
      ? [DEFAULT_RECALL_LOADING_STAGE_INDICATOR]
      : []
    startOrUpdateInkDisplay(
      React.createElement(ConfirmDisplay, {
        guidanceLines: [
          ...stageLines,
          ...view.promptLines,
          'Stop recall? (y/n)',
        ],
        placeholderText: view.placeholder,
        hint: stopConfirmHint,
        draft: stopConfirmDraft,
      })
    )
  }

  function renderInkSessionYesNo(): void {
    startOrUpdateInkDisplay(
      React.createElement(ConfirmDisplay, {
        guidanceLines: [],
        placeholderText: 'y or n; /stop to exit recall',
        hint: sessionYesNoHint,
        draft: sessionYesNoDraft,
      })
    )
    process.stdout.write(INTERACTIVE_INPUT_READY_OSC)
  }

  function renderInkMcqDisplay(): void {
    const width = getTerminalWidth()
    const choices = getNumberedChoiceListChoices() ?? []
    const promptLines =
      getNumberedChoiceListCurrentPromptWrappedLines(width) ?? []
    startOrUpdateInkDisplay(
      React.createElement(McqDisplay, {
        stageIndicatorLine: DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
        currentPromptLines: promptLines,
        choices,
        highlightIndex: numberedChoiceHighlightIndex,
      })
    )
    process.stdout.write(INTERACTIVE_INPUT_READY_OSC)
  }

  function renderInkTokenListDisplay(): void {
    if (!tokenSelection) return
    const tokenListConfig = TOKEN_LIST_COMMANDS[tokenSelection.command]
    const width = getTerminalWidth()
    const promptLines = tokenListConfig?.currentPrompt
      ? wrapTextToLines(tokenListConfig.currentPrompt, width)
      : []
    const stageIndicatorLine = tokenListConfig
      ? greyCurrentStageIndicatorLabel(tokenListConfig.stageIndicator)
      : ''
    startOrUpdateInkDisplay(
      React.createElement(TokenListDisplay, {
        stageIndicatorLine,
        currentPromptLines: promptLines,
        items: tokenSelection.items,
        defaultLabel: getDefaultTokenLabel(),
        highlightIndex: tokenSelection.highlightIndex,
      })
    )
    process.stdout.write(INTERACTIVE_INPUT_READY_OSC)
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

  function resetLivePaintCursor(): void {
    livePaint.cursorUpStepsToLiveRegionTop = 0
    livePaint.lastPaintedLineCount = 0
  }

  function commitHistoryOutput(
    lines: readonly string[],
    tone: ChatHistoryOutputTone = 'plain',
    options?: {
      inputAlreadyPaintedAboveLiveRegion?: boolean
      skipDrawBox?: boolean
    }
  ): void {
    chatHistory.push({ type: 'output', lines: [...lines], tone })
    clearLiveRegionForRepaint(livePaint)
    resetLivePaintCursor()
    paintCommittedScrollbackAppend(
      options?.inputAlreadyPaintedAboveLiveRegion ?? false,
      { skipDrawBox: options?.skipDrawBox }
    )
  }

  /** Ends token-list mode: records the slash command as input, then the outcome line in scrollback. */
  function commitTokenListResult(
    message: string,
    tone: ChatHistoryOutputTone = 'plain'
  ) {
    unmountInkDisplay()
    const command = tokenSelection?.command ?? ''
    clearLiveRegionForRepaint(livePaint)
    chatHistory.push({
      type: 'input',
      content: maskInteractiveInputForHistory(command),
    })
    tokenSelection = null
    commandInput = clearLiveCommandLine(commandInput)
    commitHistoryOutput([message], tone, {
      inputAlreadyPaintedAboveLiveRegion: true,
    })
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

  function resetCommandTurnBuffer(): void {
    commandTurn = { lines: [], tone: 'plain' }
  }

  async function runProcessInput(input: string): Promise<boolean> {
    insideProcessInput = true
    try {
      return await processInput(input, ttyOutput, true)
    } finally {
      insideProcessInput = false
      // Reset livePaint so clearLiveRegionForRepaint in commitHistoryOutput is a
      // no-op: any ANSI boxes drawn inside processInput stay in the transcript,
      // keeping the section-parser-visible separator above the final ┌─┐ box.
      resetLivePaintCursor()
    }
  }

  /** Builds lines for input box, optional Current Stage Indicator + Current prompt (above box), and Current guidance (below box). */
  function getDisplayContent() {
    const width = getTerminalWidth()
    const placeholderContext = getPlaceholderContext(!!tokenSelection)
    const stopConfirmDisplay = isPendingStopConfirmation()
      ? getStopConfirmationLiveView(placeholderContext)
      : null
    const contentLines = buildBoxLines(commandInput.lineDraft, width, {
      placeholderContext,
    })
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
    const currentPromptLines = countPromptBlockLinesAboveInputBoxTop(
      currentStageIndicatorLines,
      currentPromptWrappedLines
    )
    const numberedChoices = getNumberedChoiceListChoices()
    const suggestionLines = tokenSelection
      ? buildTokenListLines(
          tokenSelection.items,
          getDefaultTokenLabel(),
          width,
          tokenSelection.highlightIndex
        )
      : stopConfirmDisplay
        ? stopConfirmDisplay.guidance
        : numberedChoices !== null
          ? recallMcqCurrentGuidanceLines(
              numberedChoices,
              numberedChoiceHighlightIndex,
              width
            )
          : buildSuggestionLines(
              commandInput.lineDraft,
              highlightIndex,
              width,
              {
                forceCommandsHint:
                  suggestionsDismissed &&
                  isCommandPrefixWithSuggestions(commandInput.lineDraft),
              }
            )
    return {
      contentLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      currentStageIndicatorLines,
      placeholderContext,
    }
  }

  /** Row index of input box content from top of drawn area. Used for cursor positioning. */
  const inputRowFromTop = (
    currentPromptLines: number,
    contentLinesLength: number
  ) => currentPromptLines + contentLinesLength

  function measureLiveRegionLayout(): LiveRegionLayout {
    const {
      contentLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      currentStageIndicatorLines,
      placeholderContext,
    } = getDisplayContent()
    const terminalWidth = getTerminalWidth()
    const liveLines = buildLiveRegionLines(
      commandInput.lineDraft,
      terminalWidth,
      currentPromptWrappedLines,
      suggestionLines,
      currentStageIndicatorLines,
      { placeholderContext }
    )
    return {
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      currentStageIndicatorLines,
      placeholderContext,
      liveLines,
      liveLineCount: liveLines.length,
      inputLineRowInLiveBlock: inputRowFromTop(
        currentPromptLines,
        contentLines.length
      ),
      terminalWidth,
    }
  }

  const positionCursorInInputBox = () => {
    const { lineDraft, caretOffset } = commandInput
    const contentLineCount = lineDraft.split('\n').length
    let row = 0
    let col = 0
    for (let i = 0; i < caretOffset; i++) {
      if (lineDraft[i] === '\n') {
        row++
        col = 0
      } else {
        col++
      }
    }
    const lastRow = Math.max(0, contentLineCount - 1)
    const deltaUp = lastRow - row
    if (deltaUp > 0) process.stdout.write(`\x1b[${deltaUp}A`)
    else if (deltaUp < 0) process.stdout.write(`\x1b[${-deltaUp}B`)
    const prefixLen = row === 0 ? PROMPT.length : 2
    const colG = 3 + prefixLen + col
    process.stdout.write(`\x1b[${colG}G`)
  }

  /** Cursor visibility and column, then `INTERACTIVE_INPUT_READY_OSC` when the box accepts input. */
  function finalizeInteractiveLiveRegionPaint(
    placeholderContext: PlaceholderContext
  ): void {
    if (isGreyDisabledInputChrome(placeholderContext)) {
      process.stdout.write(HIDE_CURSOR)
    } else {
      process.stdout.write(SHOW_CURSOR)
      positionCursorInInputBox()
    }
    if (!insideProcessInput) {
      process.stdout.write(
        interactiveInputReadyOscSuffix({
          lineDraft: commandInput.lineDraft,
          interactiveFetchWaitLine: getInteractiveFetchWaitLine(),
        })
      )
    }
  }

  function doFullRedraw() {
    const layout = measureLiveRegionLayout()

    process.stdout.write(CLEAR_SCREEN)
    const fullLines = renderFullDisplay(
      chatHistory,
      commandInput.lineDraft,
      layout.terminalWidth,
      layout.suggestionLines,
      layout.currentStageIndicatorLines,
      layout.currentPromptWrappedLines,
      {
        placeholderContext: layout.placeholderContext,
      }
    )
    for (const line of fullLines) {
      process.stdout.write(`${line}\n`)
    }

    livePaint.lastPaintedLineCount = layout.liveLineCount
    livePaint.cursorUpStepsToLiveRegionTop = layout.inputLineRowInLiveBlock

    process.stdout.write(
      `\x1b[${layout.liveLineCount - layout.inputLineRowInLiveBlock}A`
    )
    finalizeInteractiveLiveRegionPaint(layout.placeholderContext)
  }

  function drawBox() {
    if (getNumberedChoiceListChoices() !== null) {
      renderInkMcqDisplay()
      return
    }
    if (tokenSelection) {
      renderInkTokenListDisplay()
      return
    }
    const layout = measureLiveRegionLayout()
    const { liveLines, liveLineCount, inputLineRowInLiveBlock } = layout

    if (livePaint.cursorUpStepsToLiveRegionTop > 0) {
      process.stdout.write(`\x1b[${livePaint.cursorUpStepsToLiveRegionTop}A`)
    } else if (
      livePaint.lastPaintedLineCount === 0 &&
      needsGapBeforeBox(
        chatHistory,
        layout.currentPromptWrappedLines,
        layout.currentStageIndicatorLines
      )
    ) {
      process.stdout.write('\n')
    }
    process.stdout.write('\r')

    for (const line of liveLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    const extra = livePaint.lastPaintedLineCount - liveLineCount
    for (let i = 0; i < extra; i++) {
      process.stdout.write('\x1b[2K\n')
    }

    const totalWritten = Math.max(liveLineCount, livePaint.lastPaintedLineCount)
    process.stdout.write(`\x1b[${totalWritten - inputLineRowInLiveBlock}A`)
    livePaint.cursorUpStepsToLiveRegionTop = inputLineRowInLiveBlock
    livePaint.lastPaintedLineCount = liveLineCount

    finalizeInteractiveLiveRegionPaint(layout.placeholderContext)
  }

  /**
   * Writes the newest `chatHistory` slice to stdout (no {@link CLEAR_SCREEN}), then {@link drawBox} unless
   * {@link options.skipDrawBox}. Normal turn is grey input + output; session yes/no steps add output only (no
   * grey history-input row). Token-list already wrote `renderPastInput` before this.
   */
  function paintCommittedScrollbackAppend(
    inputAlreadyPainted: boolean,
    options?: { skipDrawBox?: boolean }
  ): void {
    const h = chatHistory
    const last = h[h.length - 1]
    const prev = h[h.length - 2]
    if (!last || last.type !== 'output') {
      doFullRedraw()
      return
    }

    const width = getTerminalWidth()
    const outTone = last.tone ?? 'plain'
    if (prev?.type === 'input') {
      if (!inputAlreadyPainted) {
        process.stdout.write(renderPastInput(prev.content, width))
        process.stdout.write('\n')
      }
      for (const line of last.lines) {
        process.stdout.write(`${applyChatHistoryOutputTone(line, outTone)}\n`)
      }
    } else {
      for (const line of last.lines) {
        process.stdout.write(`${applyChatHistoryOutputTone(line, outTone)}\n`)
      }
    }
    if (!options?.skipDrawBox) {
      drawBox()
    }
  }

  function commitExitTurnToScrollback(): void {
    chatHistory.push({
      type: 'output',
      lines: [...commandTurn.lines],
      tone: commandTurn.tone,
    })
    resetLivePaintCursor()
    paintCommittedScrollbackAppend(false, { skipDrawBox: true })
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
    stopConfirmDraft = ''
    stopConfirmHint = ''
    commandInput = clearLiveCommandLine(commandInput)
    // Unmount any existing Ink confirm (e.g., session y/n) before showing stop confirm
    if (inkDisplayInstance) unmountInkDisplay()
    renderInkStopConfirm()
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
      chatHistory = []
      resetLiveLineDraftAndSlashSuggestions()
      tokenSelection = null
      unmountInkDisplay()
      if (isInCommandSessionSubstate()) exitCommandSession()
      setPendingStopConfirmation(false)
      numberedChoiceHighlightIndex = 0
      doFullRedraw()
    },
    onInteractiveFetchWaitChanged: () => {
      stopInteractiveFetchWaitRepaintTimer()
      const activeWaitPrompt = getInteractiveFetchWaitLine()
      if (activeWaitPrompt) {
        drawBox()
        interactiveFetchWaitRepaintTimer = setInterval(() => {
          interactiveFetchWaitEllipsisTick =
            (interactiveFetchWaitEllipsisTick + 1) % 3
          drawBox()
        }, INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS)
      } else {
        resetLiveLineDraftAndSlashSuggestions()
        // Skip drawBox when inside processInput: the fetch-end ANSI box would
        // land before the separator written by showRecallPrompt, confusing the
        // section parser. commitHistoryOutput draws the final box after processInput.
        if (!insideProcessInput) {
          drawBox()
        }
      }
    },
  }

  drawBox()

  process.stdout.on('resize', doFullRedraw)
  const removeResizeListener = () => process.stdout.off('resize', doFullRedraw)
  const doExit = () => {
    stopInteractiveFetchWaitRepaintTimer()
    removeResizeListener()
    unmountInkDisplay()
    process.stdout.write(SHOW_CURSOR)
    rl.close()
    process.exit(0)
  }

  stdin.on('keypress', async (str: string | undefined, key: ReadlineKey) => {
    const submitPressed =
      (key.name !== undefined && isSubmitKey(key.name)) ||
      str === '\n' ||
      str === '\r'
    if (key.ctrl && key.name === 'c') {
      process.stdout.write(`\x1b[${1}B\r\n`)
      doExit()
    }
    if (key.name === 'escape' && cancelInteractiveFetchWaitFor(ttyOutput)) {
      drawBox()
      return
    }
    if (isPendingStopConfirmation()) {
      const dispatch = dispatchSessionYesNoKey(
        {
          keyName: key.name,
          str,
          ctrl: !!key.ctrl,
          meta: !!key.meta,
          shift: !!key.shift,
          lineDraft: stopConfirmDraft,
          submitPressed,
        },
        'treat-as-no'
      )
      switch (dispatch.result) {
        case 'cancel':
          unmountInkDisplay()
          setPendingStopConfirmation(false)
          stopConfirmDraft = ''
          stopConfirmHint = ''
          commandInput = clearLiveCommandLine(commandInput)
          if (usesSessionYesNoInputChrome(false)) {
            sessionYesNoDraft = ''
            sessionYesNoHint = ''
            renderInkSessionYesNo()
          } else {
            drawBox()
          }
          break
        case 'submit-yes':
          unmountInkDisplay()
          stopConfirmDraft = ''
          stopConfirmHint = ''
          commandInput = clearLiveCommandLine(commandInput)
          setPendingStopConfirmation(false)
          exitCommandSession()
          numberedChoiceHighlightIndex = 0
          commitHistoryOutput(getStopConfirmationYesOutcomeLines())
          break
        case 'submit-no':
          unmountInkDisplay()
          stopConfirmDraft = ''
          stopConfirmHint = ''
          commandInput = clearLiveCommandLine(commandInput)
          setPendingStopConfirmation(false)
          if (usesSessionYesNoInputChrome(false)) {
            sessionYesNoDraft = ''
            sessionYesNoHint = ''
            renderInkSessionYesNo()
          } else {
            drawBox()
          }
          break
        case 'invalid-submit':
          stopConfirmDraft = ''
          stopConfirmHint = dispatch.hint
          renderInkStopConfirm()
          break
        case 'edit-backspace':
          stopConfirmDraft = stopConfirmDraft.slice(0, -1)
          stopConfirmHint = ''
          renderInkStopConfirm()
          break
        case 'edit-char':
          stopConfirmDraft += dispatch.char
          stopConfirmHint = ''
          renderInkStopConfirm()
          break
        case 'redraw':
          renderInkStopConfirm()
          break
      }
      return
    }
    const sessionYesNoInputChrome = usesSessionYesNoInputChrome(
      !!tokenSelection
    )
    if (sessionYesNoInputChrome) {
      const keysForConfirmDispatch =
        submitPressed ||
        key.name === 'backspace' ||
        (str !== undefined && str !== '' && !key.ctrl && !key.meta)
      const bareEnterOnSessionYesNo =
        submitPressed && !key.shift && sessionYesNoDraft.trim() === ''
      if (keysForConfirmDispatch && !bareEnterOnSessionYesNo) {
        const dispatch = dispatchSessionYesNoKey(
          {
            keyName: key.name,
            str,
            ctrl: !!key.ctrl,
            meta: !!key.meta,
            shift: !!key.shift,
            lineDraft: sessionYesNoDraft,
            submitPressed,
          },
          'treat-as-invalid'
        )
        switch (dispatch.result) {
          case 'cancel':
            break
          case 'submit-yes':
          case 'submit-no': {
            const effectiveLine = dispatch.result === 'submit-yes' ? 'y' : 'n'
            unmountInkDisplay()
            sessionYesNoDraft = ''
            sessionYesNoHint = ''
            commandInput = clearLiveCommandLine(commandInput)
            resetCommandTurnBuffer()
            if (isCommittedInteractiveInput(effectiveLine)) {
              if (!usesSessionYesNoInputChrome(!!tokenSelection)) {
                chatHistory.push({
                  type: 'input',
                  content: maskInteractiveInputForHistory(effectiveLine),
                })
                rememberCommittedLine(effectiveLine)
              }
              if (await runProcessInput(effectiveLine)) {
                commitExitTurnToScrollback()
                doExit()
                return
              }
              // Commit history then check if we need Ink again (another y/n).
              // drawBox always runs so ANSI ┌─┐ appears after history content,
              // allowing the E2E section parser to find it in currentPromptLines.
              const newSessionYesNo = usesSessionYesNoInputChrome(false)
              commitHistoryOutput(commandTurn.lines, commandTurn.tone)
              if (newSessionYesNo) {
                sessionYesNoDraft = ''
                sessionYesNoHint = ''
                renderInkSessionYesNo()
              }
            }
            break
          }
          case 'invalid-submit':
            sessionYesNoHint = dispatch.hint
            renderInkSessionYesNo()
            break
          case 'edit-backspace':
            sessionYesNoDraft = sessionYesNoDraft.slice(0, -1)
            sessionYesNoHint = ''
            renderInkSessionYesNo()
            break
          case 'edit-char':
            sessionYesNoDraft += dispatch.char
            sessionYesNoHint = ''
            renderInkSessionYesNo()
            break
          case 'redraw':
            renderInkSessionYesNo()
            break
        }
        return
      }
      if (key.name === 'escape' && isInCommandSessionSubstate()) {
        enterStopConfirmationFromEsc()
        return
      }
    }
    const numberedChoices = getNumberedChoiceListChoices()
    if (numberedChoices !== null) {
      const listDispatch = dispatchSelectListKey(
        {
          keyName: key.name,
          str,
          ctrl: !!key.ctrl,
          meta: !!key.meta,
          shift: !!key.shift,
          lineDraft: commandInput.lineDraft,
          submitPressed,
        },
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
          unmountInkDisplay()
          clearLiveRegionForRepaint(livePaint)
          const inputForHistory = commandInput.lineDraft || effectiveInput
          commandInput = clearLiveCommandLine(commandInput)
          numberedChoiceHighlightIndex = 0
          livePaint.lastPaintedLineCount = 0
          resetCommandTurnBuffer()
          chatHistory.push({
            type: 'input',
            content: maskInteractiveInputForHistory(inputForHistory),
          })
          if (isCommittedInteractiveInput(inputForHistory)) {
            rememberCommittedLine(inputForHistory)
          }
          if (await runProcessInput(effectiveInput)) {
            commitExitTurnToScrollback()
            doExit()
            return
          }
          // After MCQ submit, commit history (always runs drawBox so ANSI ┌─┐
          // lands after history content, keeping section parser working), then
          // optionally render session y/n confirm on top.
          const newSessionYesNo = usesSessionYesNoInputChrome(false)
          commitHistoryOutput(commandTurn.lines, commandTurn.tone)
          if (newSessionYesNo) {
            sessionYesNoDraft = ''
            sessionYesNoHint = ''
            renderInkSessionYesNo()
          }
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
      return
    }
    if (tokenSelection) {
      const listDispatch = dispatchSelectListKey(
        {
          keyName: key.name,
          str,
          ctrl: !!key.ctrl,
          meta: !!key.meta,
          shift: !!key.shift,
          lineDraft: commandInput.lineDraft,
          submitPressed,
        },
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
      return
    }
    if (key.name === 'escape') {
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

        const width = getTerminalWidth()
        const input = commandInput.lineDraft
        commandInput = clearLiveCommandLine(commandInput)

        clearLiveRegionForRepaint(livePaint)

        const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
        if (tokenSelect) {
          const tokens = listAccessTokens()
          if (tokens.length === 0) {
            chatHistory.push({
              type: 'input',
              content: maskInteractiveInputForHistory(trimmedInput),
            })
            rememberCommittedLine(trimmedInput)
            commitHistoryOutput(['No access tokens stored.'])
            return
          } else {
            if (isCommittedInteractiveInput(input)) {
              process.stdout.write(
                renderPastInput(maskInteractiveInputForHistory(input), width)
              )
              process.stdout.write('\n')
            }
            rememberCommittedLine(trimmedInput)
            beginTokenSelection(trimmedInput, tokenSelect.action, tokens)
          }
          livePaint.lastPaintedLineCount = 0
          drawBox()
          return
        }

        resetCommandTurnBuffer()
        if (isCommittedInteractiveInput(input)) {
          if (!usesSessionYesNoInputChrome(!!tokenSelection)) {
            chatHistory.push({
              type: 'input',
              content: maskInteractiveInputForHistory(input),
            })
            rememberCommittedLine(input)
          }
          if (await runProcessInput(input)) {
            commitExitTurnToScrollback()
            doExit()
            return
          }
          // Commit history (drawBox always runs for correct section parsing),
          // then render session y/n confirm on top if needed.
          const newSessionYesNo = usesSessionYesNoInputChrome(false)
          commitHistoryOutput(commandTurn.lines, commandTurn.tone)
          if (newSessionYesNo) {
            sessionYesNoDraft = ''
            sessionYesNoHint = ''
            renderInkSessionYesNo()
          }
          return
        }
        if (isNumberedChoiceListActive()) {
          numberedChoiceHighlightIndex = 0
        }
        drawBox()
      }
    } else if (key.name === 'backspace') {
      const prevLen = commandInput.lineDraft.length
      commandInput = deleteBeforeCaret(commandInput)
      if (commandInput.lineDraft.length !== prevLen) {
        highlightIndex = 0
        suggestionsDismissed = false
      }
      drawBox()
    } else if (key.name === 'up' || key.name === 'down') {
      if (
        ttyArrowKeyUsesSlashSuggestionCycle(
          key.name === 'up' ? 'up' : 'down',
          commandInput,
          suggestionsDismissed,
          isCommandPrefixWithSuggestions(commandInput.lineDraft)
        )
      ) {
        const filtered = filterCommandsByPrefix(
          interactiveDocs,
          getLastLine(commandInput.lineDraft)
        )
        const delta = key.name === 'up' ? -1 : 1
        highlightIndex = cycleListSelectionIndex(
          highlightIndex,
          delta,
          filtered.length
        )
      } else {
        const prevDraft = commandInput.lineDraft
        commandInput =
          key.name === 'up'
            ? onArrowUp(commandInput, isCommandPrefixWithSuggestions)
            : onArrowDown(commandInput, isCommandPrefixWithSuggestions)
        if (commandInput.lineDraft !== prevDraft) {
          highlightIndex = 0
          suggestionsDismissed = false
        }
      }
      drawBox()
    } else if (key.name === 'left') {
      commandInput = caretOneLeft(commandInput)
      drawBox()
    } else if (key.name === 'right') {
      commandInput = caretOneRight(commandInput)
      drawBox()
    } else if (key.name === 'home') {
      commandInput = { ...commandInput, caretOffset: 0 }
      drawBox()
    } else if (key.name === 'end') {
      commandInput = {
        ...commandInput,
        caretOffset: commandInput.lineDraft.length,
      }
      drawBox()
    } else if (key.name === 'tab') {
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
    } else if (str && !key.ctrl && !key.meta) {
      commandInput = insertIntoDraft(commandInput, str)
      highlightIndex = 0
      suggestionsDismissed = false
      drawBox()
    }
  })
}
