import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import type { AccessTokenEntry } from '../accessToken.js'
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
} from '../interactiveFetchWait.js'
import {
  applyArrowDown,
  applyArrowUp,
  clampCursorToBuffer,
  deleteBackward,
  insertAtCursor,
  moveCursorEnd,
  moveCursorHome,
  moveCursorLeft,
  moveCursorRight,
  pushSubmittedLine,
  resetLiveDraftFields,
  type InputNavState,
} from '../inputHistoryNav.js'
import {
  applyChatHistoryOutputTone,
  formatInteractiveFetchWaitPromptLine,
  interactiveInputReadyOscSuffix,
  isCommittedInteractiveInput,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  isGreyDisabledInputChrome,
  wrapTextToLines,
  type LiveRegionPaintOptions,
  type PlaceholderContext,
} from '../renderer.js'
import type {
  ChatHistory,
  ChatHistoryOutputTone,
  McqRecallPending,
  OutputAdapter,
} from '../types.js'

export type TokenListAction = 'set-default' | 'remove' | 'remove-completely'

export interface TokenListCommandConfig {
  action: TokenListAction
  /** Shown in Current prompt (above input box) when list is non-empty. */
  currentPrompt?: string
}

export interface TTYDeps {
  processInput: (
    input: string,
    output?: OutputAdapter,
    interactiveUi?: boolean
  ) => Promise<boolean>
  getPendingRecallAnswer: () => unknown
  isPendingRecallStopConfirmation: () => boolean
  setPendingRecallStopConfirmation: (value: boolean) => void
  isInRecallSubstate: () => boolean
  exitRecallMode: () => void
  isMcqPrompt: (p: unknown) => p is McqRecallPending
  buildTokenListLines: (
    tokens: AccessTokenEntry[],
    defaultLabel: string | undefined,
    width: number,
    highlightIndex: number
  ) => string[]
  getDefaultTokenLabel: () => string | undefined
  listAccessTokens: () => AccessTokenEntry[]
  removeAccessToken: (label: string) => boolean
  removeAccessTokenCompletely: (
    label: string,
    signal?: AbortSignal
  ) => Promise<void>
  setDefaultTokenLabel: (label: string) => void
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
  formatMcqChoiceLines: (choices: readonly string[]) => string[]
  getTerminalWidth: () => number
  buildCurrentPromptSeparator: (width: number) => string
  buildLiveRegionLines: (
    buffer: string,
    width: number,
    currentPromptWrappedLines: string[],
    suggestionLines: string[],
    recallingIndicator: string[],
    options?: LiveRegionPaintOptions
  ) => string[]
  needsGapBeforeBox: (
    history: ChatHistory,
    currentPromptWrappedLines: string[]
  ) => boolean
  renderFullDisplay: (
    history: ChatHistory,
    buffer: string,
    width: number,
    suggestionLines: string[],
    recallingIndicator: string[],
    currentPromptLines?: string[],
    options?: LiveRegionPaintOptions
  ) => string[]
  renderPastInput: (input: string, width: number) => string
  GREY: string
  HIDE_CURSOR: string
  SHOW_CURSOR: string
  CLEAR_SCREEN: string
  RECALLING_INDICATOR: string
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
  formatHighlightedList: (
    lines: string[],
    maxVisible?: number,
    highlightIndex?: number
  ) => string[]
  TOKEN_LIST_COMMANDS: Record<string, TokenListCommandConfig>
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
  action: TokenListAction
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
  recallingIndicator: string[]
  placeholderContext: PlaceholderContext
  currentPromptSgr: string | undefined
  liveLines: string[]
  /** Line count of the live block (used for CUU cursor positioning). */
  liveLineCount: number
  /** Rows from live block top to the input line (prompt + box content lines). */
  inputLineRowInLiveBlock: number
  terminalWidth: number
}

export async function runTTY(
  stdin: NodeJS.ReadableStream,
  deps: TTYDeps
): Promise<void> {
  const {
    processInput,
    getPendingRecallAnswer,
    isPendingRecallStopConfirmation,
    setPendingRecallStopConfirmation,
    isInRecallSubstate,
    exitRecallMode,
    isMcqPrompt,
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
    formatMcqChoiceLines,
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
    RECALLING_INDICATOR,
    PROMPT,
    filterCommandsByPrefix,
    getTabCompletion,
    interactiveDocs,
    formatHighlightedList,
    TOKEN_LIST_COMMANDS,
    getPlaceholderContext,
  } = deps

  const writeCurrentPromptLine = (msg: string) =>
    process.stdout.write(`${GREY}${msg}\x1b[0m\n`)

  const doBeginCurrentPrompt = () => {
    const sep = buildCurrentPromptSeparator(getTerminalWidth())
    process.stdout.write(`${sep}\n`)
  }

  function isCommandPrefixWithSuggestions(buffer: string): boolean {
    const lastLine = getLastLine(buffer)
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
  let buffer = ''
  let cursorOffset = 0
  let submittedCommandLines: string[] = []
  let historyBrowseIndex: number | null = null
  let historyDraftCache: string | null = null
  let highlightIndex = 0
  let suggestionsDismissed = false
  const livePaint: LiveRegionPaintCursor = {
    cursorUpStepsToLiveRegionTop: 0,
    lastPaintedLineCount: 0,
  }
  let tokenSelection: TokenSelectionState | null = null
  let mcqChoiceHighlightIndex = 0
  let interactiveFetchWaitEllipsisTick = 0
  let interactiveFetchWaitRepaintTimer: ReturnType<typeof setInterval> | null =
    null

  function nav(): InputNavState {
    return {
      buffer,
      cursorOffset,
      submittedLines: submittedCommandLines,
      historyBrowseIndex,
      historyDraftCache,
    }
  }

  function applyNav(next: InputNavState): void {
    buffer = next.buffer
    cursorOffset = next.cursorOffset
    submittedCommandLines = next.submittedLines
    historyBrowseIndex = next.historyBrowseIndex
    historyDraftCache = next.historyDraftCache
  }

  function recordSubmittedCommand(raw: string): void {
    submittedCommandLines = pushSubmittedLine(submittedCommandLines, raw)
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
    paintCommittedTurnAppend(
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
    chatHistory.push({ type: 'input', content: command })
    tokenSelection = null
    applyNav(resetLiveDraftFields(nav()))
    commitHistoryOutput([message], tone, {
      inputAlreadyPaintedAboveLiveRegion: true,
    })
  }

  function beginTokenSelection(
    command: string,
    action: TokenListAction,
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

  /** Builds lines for input box, recalling indicator, Current prompt (above box), and Current guidance (below box). */
  function getDisplayContent() {
    const width = getTerminalWidth()
    const placeholderContext = getPlaceholderContext(!!tokenSelection)
    const contentLines = buildBoxLines(buffer, width, {
      placeholderContext,
    })
    const pendingRecallAnswer = getPendingRecallAnswer()
    const waitLine = getInteractiveFetchWaitLine()
    let currentPromptWrappedLines: string[]
    let currentPromptSgr: string | undefined
    if (waitLine) {
      currentPromptWrappedLines = [
        formatInteractiveFetchWaitPromptLine(
          waitLine,
          interactiveFetchWaitEllipsisTick
        ),
      ]
      currentPromptSgr = INTERACTIVE_FETCH_WAIT_PROMPT_FG
    } else {
      const currentPromptText = tokenSelection
        ? TOKEN_LIST_COMMANDS[tokenSelection.command]?.currentPrompt
        : undefined
      currentPromptWrappedLines = currentPromptText
        ? wrapTextToLines(currentPromptText, width)
        : []
      currentPromptSgr = undefined
    }
    const currentPromptLines = currentPromptWrappedLines.length
      ? 1 + currentPromptWrappedLines.length
      : 0
    const suggestionLines = tokenSelection
      ? buildTokenListLines(
          tokenSelection.items,
          getDefaultTokenLabel(),
          width,
          tokenSelection.highlightIndex
        )
      : isPendingRecallStopConfirmation()
        ? ['Stop recall? (y/n)']
        : isMcqPrompt(pendingRecallAnswer)
          ? formatHighlightedList(
              formatMcqChoiceLines(pendingRecallAnswer.choices),
              undefined,
              mcqChoiceHighlightIndex
            )
          : buildSuggestionLines(buffer, highlightIndex, width, {
              forceCommandsHint:
                suggestionsDismissed && isCommandPrefixWithSuggestions(buffer),
            })
    const recallingIndicator = isInRecallSubstate() ? [RECALLING_INDICATOR] : []
    return {
      contentLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
      currentPromptSgr,
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
      recallingIndicator,
      placeholderContext,
      currentPromptSgr,
    } = getDisplayContent()
    const terminalWidth = getTerminalWidth()
    const liveLines = buildLiveRegionLines(
      buffer,
      terminalWidth,
      currentPromptWrappedLines,
      suggestionLines,
      recallingIndicator,
      { placeholderContext, currentPromptSgr }
    )
    return {
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
      currentPromptSgr,
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
    const contentLineCount = buffer.split('\n').length
    let row = 0
    let col = 0
    for (let i = 0; i < cursorOffset; i++) {
      if (buffer[i] === '\n') {
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
        lineDraft: buffer,
        interactiveFetchWaitLine: getInteractiveFetchWaitLine(),
      })
    )
  }

  function doFullRedraw() {
    const layout = measureLiveRegionLayout()

    process.stdout.write(CLEAR_SCREEN)
    const fullLines = renderFullDisplay(
      chatHistory,
      buffer,
      layout.terminalWidth,
      layout.suggestionLines,
      layout.recallingIndicator,
      layout.currentPromptWrappedLines,
      {
        placeholderContext: layout.placeholderContext,
        currentPromptSgr: layout.currentPromptSgr,
      }
    )
    for (const line of fullLines) {
      process.stdout.write(`${line}\n`)
    }

    livePaint.cursorUpStepsToLiveRegionTop = layout.inputLineRowInLiveBlock
    livePaint.lastPaintedLineCount = layout.liveLineCount

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
      needsGapBeforeBox(chatHistory, layout.currentPromptWrappedLines)
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
    finalizeInteractiveLiveRegionPaint(layout.placeholderContext)

    livePaint.cursorUpStepsToLiveRegionTop = inputLineRowInLiveBlock
    livePaint.lastPaintedLineCount = liveLineCount
  }

  /**
   * Paints the last history turn (input grey block + output lines) without {@link CLEAR_SCREEN}, then
   * {@link drawBox} unless {@link options.skipDrawBox} (e.g. quit after `exit` — keep scrollback, no live input box).
   * Token-list completion already wrote `renderPastInput` when entering selection mode.
   */
  function paintCommittedTurnAppend(
    inputAlreadyPainted: boolean,
    options?: { skipDrawBox?: boolean }
  ): void {
    const h = chatHistory
    const last = h[h.length - 1]
    const prev = h[h.length - 2]
    if (!(last && prev) || last.type !== 'output' || prev.type !== 'input') {
      doFullRedraw()
      return
    }

    const width = getTerminalWidth()
    if (!inputAlreadyPainted) {
      process.stdout.write(renderPastInput(prev.content, width))
      process.stdout.write('\n')
    }
    const outTone = last.tone ?? 'plain'
    for (const line of last.lines) {
      process.stdout.write(`${applyChatHistoryOutputTone(line, outTone)}\n`)
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
    paintCommittedTurnAppend(false, { skipDrawBox: true })
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
    applyNav(resetLiveDraftFields(nav()))
    highlightIndex = 0
    suggestionsDismissed = false
  }

  /** MCQ recall turn: line passed to `processInput` (slash escapes, number, or highlighted choice). */
  function recallMcqSubmittedLine(
    trimmedBuffer: string,
    choices: readonly string[],
    highlightedChoiceIndex: number
  ): string {
    if (trimmedBuffer === '/stop') return '/stop'
    if (trimmedBuffer === '/contest') return '/contest'
    const n = Number.parseInt(trimmedBuffer, 10)
    if (n >= 1 && n <= choices.length) return String(n)
    return String(highlightedChoiceIndex + 1)
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
      mcqChoiceHighlightIndex = 0
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
    const submitPressed = isSubmitKey(key.name) || str === '\n' || str === '\r'
    if (key.ctrl && key.name === 'c') {
      process.stdout.write(`\x1b[${1}B\r\n`)
      doExit()
    }
    if (key.name === 'escape' && cancelInteractiveFetchWaitFor(ttyOutput)) {
      drawBox()
      return
    }
    if (isPendingRecallStopConfirmation()) {
      if (key.name === 'escape') {
        setPendingRecallStopConfirmation(false)
        applyNav(resetLiveDraftFields(nav()))
        drawBox()
      } else if (submitPressed && !key.shift) {
        const trimmed = buffer.trim()
        const answer = trimmed.toLowerCase()
        const isYes = answer === 'y' || answer === 'yes'
        const isNo = answer === 'n' || answer === 'no'
        applyNav(resetLiveDraftFields(nav()))
        setPendingRecallStopConfirmation(false)
        if (isYes) {
          exitRecallMode()
          mcqChoiceHighlightIndex = 0
          chatHistory.push({ type: 'input', content: trimmed })
          recordSubmittedCommand(trimmed)
          commitHistoryOutput(['Stopped recall'])
          return
        }
        if (!isNo && trimmed) {
          writeCurrentPromptLine('Please answer y or n')
          setPendingRecallStopConfirmation(true)
        }
        drawBox()
      } else if (str && !key.ctrl && !key.meta) {
        applyNav(insertAtCursor(nav(), str))
        drawBox()
      } else if (key.name === 'backspace') {
        applyNav(deleteBackward(nav()))
        drawBox()
      } else {
        drawBox()
      }
      return
    }
    const pendingRecallAnswer = getPendingRecallAnswer()
    if (isMcqPrompt(pendingRecallAnswer)) {
      const choices = pendingRecallAnswer.choices
      if (key.name === 'escape') {
        setPendingRecallStopConfirmation(true)
        applyNav(resetLiveDraftFields(nav()))
        doBeginCurrentPrompt()
        writeCurrentPromptLine('Stop recall? (y/n)')
        drawBox()
      } else if (key.name === 'up' || key.name === 'down') {
        const delta = key.name === 'up' ? -1 : 1
        mcqChoiceHighlightIndex = cycleIndex(
          mcqChoiceHighlightIndex,
          delta,
          choices.length
        )
        drawBox()
      } else if (submitPressed && !key.shift) {
        const trimmedBuffer = buffer.trim()
        const effectiveInput = recallMcqSubmittedLine(
          trimmedBuffer,
          choices,
          mcqChoiceHighlightIndex
        )
        clearLiveRegionForRepaint(livePaint)
        const inputForHistory = buffer || effectiveInput
        applyNav(resetLiveDraftFields(nav()))
        mcqChoiceHighlightIndex = 0
        livePaint.lastPaintedLineCount = 0
        resetCommandTurnBuffer()
        chatHistory.push({ type: 'input', content: inputForHistory })
        if (isCommittedInteractiveInput(inputForHistory)) {
          recordSubmittedCommand(inputForHistory)
        }
        if (await processInput(effectiveInput, ttyOutput, true)) {
          commitExitTurnToScrollback()
          doExit()
          return
        }
        commitHistoryOutput(commandTurn.lines, commandTurn.tone)
      } else if (str && !key.ctrl && !key.meta) {
        applyNav(insertAtCursor(nav(), str))
        drawBox()
      } else if (key.name === 'backspace') {
        applyNav(deleteBackward(nav()))
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
        exitRecallMode()
        applyNav(resetLiveDraftFields(nav()))
        mcqChoiceHighlightIndex = 0
        resetLivePaintCursor()
        drawBox()
        return
      }
      if (isCommandPrefixWithSuggestions(buffer)) {
        highlightIndex = 0
        const lastLine = getLastLine(buffer)
        if (lastLine === '/') {
          const bufferLines = buffer.split('\n')
          const nextBuf =
            bufferLines.length === 1 ? '' : bufferLines.slice(0, -1).join('\n')
          applyNav(
            clampCursorToBuffer({
              ...nav(),
              buffer: nextBuf,
              cursorOffset: nextBuf.length,
            })
          )
        } else {
          suggestionsDismissed = true
        }
        drawBox()
      }
      return
    }
    if (submitPressed) {
      if (key.shift) {
        applyNav(insertAtCursor(nav(), '\n'))
        drawBox()
      } else {
        const trimmedInput = buffer.trim()

        if (trimmedInput === '/clear') {
          ttyOutput.clearAndRedraw?.()
          return
        }

        if (isCommandPrefixWithSuggestions(buffer)) {
          const filtered = filterCommandsByPrefix(
            interactiveDocs,
            getLastLine(buffer)
          )
          const selectedCommand = `${filtered[highlightIndex].usage} `
          const bufferLines = buffer.split('\n')
          const nextBuf =
            bufferLines.slice(0, -1).concat(selectedCommand).join('\n') || ''
          applyNav(
            clampCursorToBuffer({
              ...nav(),
              buffer: nextBuf,
              cursorOffset: nextBuf.length,
            })
          )
          highlightIndex = 0
          drawBox()
          return
        }

        const width = getTerminalWidth()
        const input = buffer
        applyNav(resetLiveDraftFields(nav()))

        clearLiveRegionForRepaint(livePaint)

        const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
        if (tokenSelect) {
          const tokens = listAccessTokens()
          if (tokens.length === 0) {
            chatHistory.push({ type: 'input', content: trimmedInput })
            recordSubmittedCommand(trimmedInput)
            commitHistoryOutput(['No access tokens stored.'])
            return
          } else {
            if (isCommittedInteractiveInput(input)) {
              process.stdout.write(renderPastInput(input, width))
              process.stdout.write('\n')
            }
            recordSubmittedCommand(trimmedInput)
            beginTokenSelection(trimmedInput, tokenSelect.action, tokens)
          }
          livePaint.lastPaintedLineCount = 0
          drawBox()
          return
        }

        resetCommandTurnBuffer()
        if (isCommittedInteractiveInput(input)) {
          chatHistory.push({ type: 'input', content: input })
          recordSubmittedCommand(input)
          if (await processInput(input, ttyOutput, true)) {
            commitExitTurnToScrollback()
            doExit()
            return
          }
          commitHistoryOutput(commandTurn.lines, commandTurn.tone)
          return
        }
        if (isMcqPrompt(getPendingRecallAnswer())) {
          mcqChoiceHighlightIndex = 0
        }
        drawBox()
      }
    } else if (key.name === 'backspace') {
      const prevLen = buffer.length
      applyNav(deleteBackward(nav()))
      if (buffer.length !== prevLen) {
        highlightIndex = 0
        suggestionsDismissed = false
      }
      drawBox()
    } else if (key.name === 'up' || key.name === 'down') {
      const prevBuf = buffer
      applyNav(key.name === 'up' ? applyArrowUp(nav()) : applyArrowDown(nav()))
      if (buffer !== prevBuf) {
        highlightIndex = 0
        suggestionsDismissed = false
      }
      drawBox()
    } else if (key.name === 'left') {
      applyNav(moveCursorLeft(nav()))
      drawBox()
    } else if (key.name === 'right') {
      applyNav(moveCursorRight(nav()))
      drawBox()
    } else if (key.name === 'home') {
      applyNav(moveCursorHome(nav()))
      drawBox()
    } else if (key.name === 'end') {
      applyNav(moveCursorEnd(nav()))
      drawBox()
    } else if (key.name === 'tab') {
      const lastLine = getLastLine(buffer)
      if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
        const { completed, count } = getTabCompletion(lastLine, interactiveDocs)
        if (count > 0 && completed !== lastLine) {
          const bufferLines = buffer.split('\n')
          const nextBuf =
            bufferLines.slice(0, -1).concat(completed).join('\n') || completed
          applyNav(
            clampCursorToBuffer({
              ...nav(),
              buffer: nextBuf,
              cursorOffset: nextBuf.length,
            })
          )
          highlightIndex = 0
          suggestionsDismissed = false
          drawBox()
        }
      }
    } else if (str && !key.ctrl && !key.meta) {
      applyNav(insertAtCursor(nav(), str))
      highlightIndex = 0
      suggestionsDismissed = false
      drawBox()
    }
  })
}
