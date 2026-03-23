import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import type { CommandDoc } from '../help.js'
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
import { formatRecallNotebookCurrentPromptLine } from '../recall.js'
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
  dispatchRecallStopConfirmKey,
  recallStopConfirmViewModelForContext,
} from '../interactions/recallStopConfirmInteraction.js'
import {
  applyChatHistoryOutputTone,
  countPromptBlockLinesAboveInputBoxTop,
  greyCurrentStageIndicatorLabel,
  interactiveFetchWaitStageIndicatorLine,
  interactiveInputReadyOscSuffix,
  isCommittedInteractiveInput,
  isGreyDisabledInputChrome,
  RECALL_SESSION_YES_NO_PLACEHOLDER,
  wrapTextToLines,
  type LiveRegionPaintOptions,
  type PlaceholderContext,
} from '../renderer.js'
import type {
  AccessTokenPickerAction,
  AccessTokenPickerCommandConfig,
  ChatHistory,
  ChatHistoryOutputTone,
  McqRecallPending,
  OutputAdapter,
  PendingRecallAnswer,
  RecallMcqChoiceTexts,
} from '../types.js'

export interface TTYDeps {
  processInput: (
    input: string,
    output?: OutputAdapter,
    interactiveUi?: boolean
  ) => Promise<boolean>
  getPendingRecallAnswer: () => PendingRecallAnswer
  isPendingRecallStopConfirmation: () => boolean
  setPendingRecallStopConfirmation: (value: boolean) => void
  isInRecallSubstate: () => boolean
  exitRecallMode: () => void
  isMcqRecallPending: (p: PendingRecallAnswer) => p is McqRecallPending
  buildTokenListLines: (
    tokens: AccessTokenEntry[],
    defaultLabel: AccessTokenLabel | undefined,
    width: number,
    highlightIndex: number
  ) => string[]
  getDefaultTokenLabel: () => AccessTokenLabel | undefined
  listAccessTokens: () => AccessTokenEntry[]
  removeAccessToken: (label: AccessTokenLabel) => boolean
  removeAccessTokenCompletely: (
    label: AccessTokenLabel,
    signal?: AbortSignal
  ) => Promise<void>
  setDefaultTokenLabel: (label: AccessTokenLabel) => void
  formatVersionOutput: () => string
  getLastLine: (buffer: string) => string
  buildBoxLines: (
    buffer: string,
    width: number,
    options?: { placeholderContext?: PlaceholderContext }
  ) => string[]
  buildSuggestionLines: (
    buffer: string,
    highlightIndex: number,
    width: number,
    options?: { forceCommandsHint?: boolean }
  ) => string[]
  wrapMarkdownTerminalToLines: (text: string, width: number) => string[]
  recallMcqCurrentGuidanceLines: (
    choices: RecallMcqChoiceTexts,
    selectedChoiceIndex: number,
    width: number
  ) => string[]
  getTerminalWidth: () => number
  buildCurrentPromptSeparator: (width: number) => string
  buildLiveRegionLines: (
    buffer: string,
    width: number,
    currentPromptWrappedLines: string[],
    suggestionLines: string[],
    currentStageIndicatorLines: string[],
    options?: LiveRegionPaintOptions
  ) => string[]
  needsGapBeforeBox: (
    history: ChatHistory,
    currentPromptWrappedLines: string[],
    currentStageIndicatorLines: string[]
  ) => boolean
  renderFullDisplay: (
    history: ChatHistory,
    buffer: string,
    width: number,
    suggestionLines: string[],
    currentStageIndicatorLines: string[],
    currentPromptLines?: string[],
    options?: LiveRegionPaintOptions
  ) => string[]
  renderPastInput: (input: string, width: number) => string
  GREY: string
  HIDE_CURSOR: string
  SHOW_CURSOR: string
  CLEAR_SCREEN: string
  DEFAULT_RECALL_LOADING_STAGE_INDICATOR: string
  PROMPT: string
  filterCommandsByPrefix: (
    commands: readonly CommandDoc[],
    prefix: string
  ) => readonly CommandDoc[]
  getTabCompletion: (
    buffer: string,
    commands: readonly CommandDoc[]
  ) => { completed: string; count: number }
  interactiveDocs: readonly CommandDoc[]
  TOKEN_LIST_COMMANDS: Record<string, AccessTokenPickerCommandConfig>
  getPlaceholderContext: (inTokenList: boolean) => PlaceholderContext
}

function cycleIndex(current: number, delta: number, length: number): number {
  return (current + delta + length) % length
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

/** One submitted command’s buffered scrollback: lines + dominant tone for `commitHistoryOutput`. */
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
    getPendingRecallAnswer,
    isPendingRecallStopConfirmation,
    setPendingRecallStopConfirmation,
    isInRecallSubstate,
    exitRecallMode,
    isMcqRecallPending,
    buildTokenListLines,
    getDefaultTokenLabel,
    listAccessTokens,
    removeAccessToken,
    removeAccessTokenCompletely,
    setDefaultTokenLabel,
    formatVersionOutput,
    getLastLine,
    buildBoxLines,
    buildSuggestionLines,
    wrapMarkdownTerminalToLines,
    recallMcqCurrentGuidanceLines,
    getTerminalWidth,
    buildCurrentPromptSeparator,
    buildLiveRegionLines,
    needsGapBeforeBox,
    renderFullDisplay,
    renderPastInput,
    GREY,
    HIDE_CURSOR,
    SHOW_CURSOR,
    CLEAR_SCREEN,
    DEFAULT_RECALL_LOADING_STAGE_INDICATOR,
    PROMPT,
    filterCommandsByPrefix,
    getTabCompletion,
    interactiveDocs,
    TOKEN_LIST_COMMANDS,
    getPlaceholderContext,
  } = deps

  function currentStageIndicatorLinesForLiveRegion(
    waitLine: InteractiveFetchWaitLine | null,
    fetchWaitEllipsisTick: number,
    tokenPicker: AccessTokenPickerCommandConfig | undefined,
    recallPayloadLoading: boolean
  ): string[] {
    if (waitLine != null) {
      return [
        interactiveFetchWaitStageIndicatorLine(waitLine, fetchWaitEllipsisTick),
      ]
    }
    if (tokenPicker != null) {
      return [greyCurrentStageIndicatorLabel(tokenPicker.stageIndicator)]
    }
    if (recallPayloadLoading) {
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
  let recallMcqSelectedChoiceIndex = 0
  let interactiveFetchWaitEllipsisTick = 0
  let interactiveFetchWaitRepaintTimer: ReturnType<typeof setInterval> | null =
    null

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
    options?: { inputAlreadyPaintedAboveLiveRegion?: boolean }
  ): void {
    chatHistory.push({ type: 'output', lines: [...lines], tone })
    clearLiveRegionForRepaint(livePaint)
    resetLivePaintCursor()
    paintCommittedScrollbackAppend(
      options?.inputAlreadyPaintedAboveLiveRegion ?? false
    )
  }

  /** Ends token-list mode: records the slash command as input, then the outcome line in scrollback. */
  function commitTokenListResult(
    message: string,
    tone: ChatHistoryOutputTone = 'plain'
  ) {
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

  /** Buffered `processInput` stdout for one submitted command (MCQ path or normal Enter). */
  let commandTurn: BufferedCommandTurn = { lines: [], tone: 'plain' }
  let ttyOutput: OutputAdapter

  function resetCommandTurnBuffer(): void {
    commandTurn = { lines: [], tone: 'plain' }
  }

  /** Builds lines for input box, optional Current Stage Indicator + Current prompt (above box), and Current guidance (below box). */
  function getDisplayContent() {
    const width = getTerminalWidth()
    const placeholderContext = getPlaceholderContext(!!tokenSelection)
    const recallStopConfirmDisplay = isPendingRecallStopConfirmation()
      ? recallStopConfirmViewModelForContext(placeholderContext)
      : null
    const contentLines = buildBoxLines(commandInput.lineDraft, width, {
      placeholderContext,
    })
    const pendingRecallAnswer = getPendingRecallAnswer()
    const waitLine = getInteractiveFetchWaitLine()
    const tokenListConfig = tokenSelection
      ? TOKEN_LIST_COMMANDS[tokenSelection.command]
      : undefined
    let currentPromptWrappedLines: string[]
    if (waitLine) {
      currentPromptWrappedLines = []
    } else if (recallStopConfirmDisplay) {
      currentPromptWrappedLines = recallStopConfirmDisplay.promptLines
    } else {
      const currentPromptText = tokenListConfig?.currentPrompt
      if (
        !tokenSelection &&
        isMcqRecallPending(pendingRecallAnswer) &&
        !isPendingRecallStopConfirmation()
      ) {
        currentPromptWrappedLines = [
          ...wrapTextToLines(
            formatRecallNotebookCurrentPromptLine(
              pendingRecallAnswer.notebookTitle
            ),
            width
          ),
          ...wrapMarkdownTerminalToLines(
            pendingRecallAnswer.stemRenderedForTerminal,
            width
          ),
        ]
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
      isInRecallSubstate()
    )
    const currentPromptLines = countPromptBlockLinesAboveInputBoxTop(
      currentStageIndicatorLines,
      currentPromptWrappedLines
    )
    const suggestionLines = tokenSelection
      ? buildTokenListLines(
          tokenSelection.items,
          getDefaultTokenLabel(),
          width,
          tokenSelection.highlightIndex
        )
      : recallStopConfirmDisplay
        ? recallStopConfirmDisplay.guidance
        : isMcqRecallPending(pendingRecallAnswer)
          ? recallMcqCurrentGuidanceLines(
              pendingRecallAnswer.choices,
              recallMcqSelectedChoiceIndex,
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
    process.stdout.write(
      interactiveInputReadyOscSuffix({
        lineDraft: commandInput.lineDraft,
        interactiveFetchWaitLine: getInteractiveFetchWaitLine(),
      })
    )
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
   * {@link options.skipDrawBox}. Normal turn is grey input + output; recall y/n adds output only (`recallYesNo`
   * placeholder). Token-list already wrote `renderPastInput` before this.
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

  /** MCQ recall turn: line passed to `processInput` (slash escapes, number, or highlighted choice). */
  function recallMcqSubmittedLine(
    trimmedBuffer: string,
    choices: RecallMcqChoiceTexts,
    selectedChoiceIndex: number
  ): string {
    if (trimmedBuffer === '/stop') return '/stop'
    if (trimmedBuffer === '/contest') return '/contest'
    const n = Number.parseInt(trimmedBuffer, 10)
    if (n >= 1 && n <= choices.length) return String(n)
    return String(selectedChoiceIndex + 1)
  }

  const enterRecallStopConfirmationFromEsc = (): void => {
    setPendingRecallStopConfirmation(true)
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
      chatHistory = []
      resetLiveLineDraftAndSlashSuggestions()
      tokenSelection = null
      if (isInRecallSubstate()) exitRecallMode()
      setPendingRecallStopConfirmation(false)
      recallMcqSelectedChoiceIndex = 0
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
        drawBox()
      }
    },
  }

  drawBox()

  process.stdout.on('resize', doFullRedraw)
  const removeResizeListener = () => process.stdout.off('resize', doFullRedraw)
  const doExit = () => {
    stopInteractiveFetchWaitRepaintTimer()
    removeResizeListener()
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
    if (isPendingRecallStopConfirmation()) {
      const dispatch = dispatchRecallStopConfirmKey({
        keyName: key.name,
        str,
        ctrl: !!key.ctrl,
        meta: !!key.meta,
        shift: !!key.shift,
        lineDraft: commandInput.lineDraft,
        submitPressed,
      })
      switch (dispatch.result) {
        case 'cancel':
          setPendingRecallStopConfirmation(false)
          commandInput = clearLiveCommandLine(commandInput)
          drawBox()
          break
        case 'submit-yes':
          commandInput = clearLiveCommandLine(commandInput)
          setPendingRecallStopConfirmation(false)
          exitRecallMode()
          recallMcqSelectedChoiceIndex = 0
          commitHistoryOutput(['Stopped recall'])
          break
        case 'submit-no':
          commandInput = clearLiveCommandLine(commandInput)
          setPendingRecallStopConfirmation(false)
          drawBox()
          break
        case 'invalid-submit':
          commandInput = clearLiveCommandLine(commandInput)
          setPendingRecallStopConfirmation(false)
          writeCurrentPromptLine(dispatch.hint)
          setPendingRecallStopConfirmation(true)
          drawBox()
          break
        case 'edit-backspace':
          commandInput = deleteBeforeCaret(commandInput)
          drawBox()
          break
        case 'edit-char':
          commandInput = insertIntoDraft(commandInput, dispatch.char)
          drawBox()
          break
        case 'redraw':
          drawBox()
          break
      }
      return
    }
    const pendingRecallAnswer = getPendingRecallAnswer()
    if (isMcqRecallPending(pendingRecallAnswer)) {
      const choices = pendingRecallAnswer.choices
      if (key.name === 'escape') {
        enterRecallStopConfirmationFromEsc()
      } else if (key.name === 'up' || key.name === 'down') {
        const delta = key.name === 'up' ? -1 : 1
        recallMcqSelectedChoiceIndex = cycleIndex(
          recallMcqSelectedChoiceIndex,
          delta,
          choices.length
        )
        drawBox()
      } else if (submitPressed && !key.shift) {
        const trimmedBuffer = commandInput.lineDraft.trim()
        const effectiveInput = recallMcqSubmittedLine(
          trimmedBuffer,
          choices,
          recallMcqSelectedChoiceIndex
        )
        clearLiveRegionForRepaint(livePaint)
        const inputForHistory = commandInput.lineDraft || effectiveInput
        commandInput = clearLiveCommandLine(commandInput)
        recallMcqSelectedChoiceIndex = 0
        livePaint.lastPaintedLineCount = 0
        resetCommandTurnBuffer()
        chatHistory.push({
          type: 'input',
          content: maskInteractiveInputForHistory(inputForHistory),
        })
        if (isCommittedInteractiveInput(inputForHistory)) {
          rememberCommittedLine(inputForHistory)
        }
        if (await processInput(effectiveInput, ttyOutput, true)) {
          commitExitTurnToScrollback()
          doExit()
          return
        }
        commitHistoryOutput(commandTurn.lines, commandTurn.tone)
      } else if (key.name === 'backspace') {
        commandInput = deleteBeforeCaret(commandInput)
        drawBox()
      } else if (str && !key.ctrl && !key.meta) {
        commandInput = insertIntoDraft(commandInput, str)
        drawBox()
      } else {
        drawBox()
      }
      return
    }
    if (tokenSelection) {
      if (key.name === 'up' || key.name === 'down') {
        const delta = key.name === 'up' ? -1 : 1
        tokenSelection.highlightIndex = cycleIndex(
          tokenSelection.highlightIndex,
          delta,
          tokenSelection.items.length
        )
        drawBox()
      } else if (submitPressed && !key.shift) {
        const selectedLabel =
          tokenSelection.items[tokenSelection.highlightIndex]!.label
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
      } else {
        commitTokenListResult(CLI_USER_ABORTED_WAIT_MESSAGE, 'userNotice')
      }
      return
    }
    if (key.name === 'escape') {
      if (isInRecallSubstate()) {
        enterRecallStopConfirmationFromEsc()
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
          if (
            getPlaceholderContext(!!tokenSelection) !==
            RECALL_SESSION_YES_NO_PLACEHOLDER
          ) {
            chatHistory.push({
              type: 'input',
              content: maskInteractiveInputForHistory(input),
            })
            rememberCommittedLine(input)
          }
          if (await processInput(input, ttyOutput, true)) {
            commitExitTurnToScrollback()
            doExit()
            return
          }
          commitHistoryOutput(commandTurn.lines, commandTurn.tone)
          return
        }
        if (isMcqRecallPending(getPendingRecallAnswer())) {
          recallMcqSelectedChoiceIndex = 0
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
        highlightIndex = cycleIndex(highlightIndex, delta, filtered.length)
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
