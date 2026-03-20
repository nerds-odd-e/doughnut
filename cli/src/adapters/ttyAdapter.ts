import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import type { AccessTokenEntry } from '../accessToken.js'
import type { CommandDoc } from '../help.js'
import { isFetchAbortedByCaller } from '../fetchAbort.js'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  cancelInteractiveFetchWaitFor,
  type InteractiveFetchWaitLine,
} from '../interactiveFetchWait.js'
import {
  formatInteractiveFetchWaitPromptLine,
  interactiveInputReadyOscSuffix,
  isCommittedInteractiveInput,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  wrapTextToLines,
  type LiveRegionPaintOptions,
  type PlaceholderContext,
} from '../renderer.js'
import type { ChatHistory, McqRecallPending, OutputAdapter } from '../types.js'

export type TokenListAction = 'set-default' | 'remove' | 'remove-completely'

export interface TokenListCommandConfig {
  action: TokenListAction
  /** Shown in Current prompt (above input box) when list is non-empty. */
  currentPrompt?: string
}

export interface TTYDeps {
  processInput: (input: string, output?: OutputAdapter) => Promise<boolean>
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
  removeAccessTokenCompletely: (label: string) => Promise<void>
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
  formatMcqChoiceLines: (choices: string[]) => string[]
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
  renderBox: (lines: string[], width: number) => string
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
  getInteractiveFetchWaitLine: () => InteractiveFetchWaitLine | null
  runInteractiveFetchWait: <T>(
    output: OutputAdapter,
    line: InteractiveFetchWaitLine,
    fn: () => Promise<T>
  ) => Promise<T>
  isGreyDisabledInputChrome: (ctx: PlaceholderContext) => boolean
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

function writeError(err: unknown): void {
  process.stdout.write(`${err instanceof Error ? err.message : String(err)}\n`)
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
    getInteractiveFetchWaitLine,
    runInteractiveFetchWait,
    isGreyDisabledInputChrome,
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
  let highlightIndex = 0
  let suggestionsDismissed = false
  const livePaint: LiveRegionPaintCursor = {
    cursorUpStepsToLiveRegionTop: 0,
    lastPaintedLineCount: 0,
  }
  let tokenListItems: AccessTokenEntry[] | null = null
  let tokenListCommand = ''
  let tokenHighlightIndex = 0
  let tokenListAction: TokenListAction = 'set-default'
  let mcqChoiceHighlightIndex = 0
  const INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS = 400
  let interactiveFetchWaitEllipsisTick = 0
  let interactiveFetchWaitRepaintTimer: ReturnType<typeof setInterval> | null =
    null

  function endTokenListSelection(outputMsg: string) {
    const command = tokenListCommand
    clearLiveRegionForRepaint(livePaint)
    process.stdout.write(`${outputMsg}\n`)
    chatHistory.push({ type: 'input', content: command })
    chatHistory.push({ type: 'output', lines: [outputMsg] })
    tokenListItems = null
    tokenListCommand = ''
    tokenHighlightIndex = 0
    tokenListAction = 'set-default'
    buffer = ''
    livePaint.lastPaintedLineCount = 0
    drawBox()
  }

  const collectedOutputLines: string[] = []
  let ttyOutput: OutputAdapter

  /** Builds lines for input box, recalling indicator, Current prompt (above box), and Current guidance (below box). */
  function getDisplayContent() {
    const width = getTerminalWidth()
    const placeholderContext = getPlaceholderContext(!!tokenListItems)
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
      const currentPromptText =
        tokenListItems && tokenListCommand
          ? TOKEN_LIST_COMMANDS[tokenListCommand]?.currentPrompt
          : undefined
      currentPromptWrappedLines = currentPromptText
        ? wrapTextToLines(currentPromptText, width)
        : []
      currentPromptSgr = undefined
    }
    const currentPromptLines = currentPromptWrappedLines.length
      ? 1 + currentPromptWrappedLines.length
      : 0
    const suggestionLines = tokenListItems
      ? buildTokenListLines(
          tokenListItems,
          getDefaultTokenLabel(),
          width,
          tokenHighlightIndex
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
      interactiveFetchWaitLine: waitLine,
    }
  }

  /** Row index of input box content from top of drawn area. Used for cursor positioning. */
  const inputRowFromTop = (
    currentPromptLines: number,
    contentLinesLength: number
  ) => currentPromptLines + contentLinesLength

  const positionCursorInInputBox = () => {
    const bufferLines = buffer.split('\n')
    const lastLine = bufferLines[bufferLines.length - 1] ?? ''
    const lastPrefix = bufferLines.length === 1 ? PROMPT : '  '
    const col = 3 + lastPrefix.length + lastLine.length
    process.stdout.write(`\x1b[${col}G`)
  }

  /** Cursor visibility and column, then `INTERACTIVE_INPUT_READY_OSC` from `renderer.ts` when the box accepts input. */
  function finalizeInteractiveLiveRegionPaint(
    placeholderContext: PlaceholderContext,
    interactiveFetchWaitLine: InteractiveFetchWaitLine | null
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
        interactiveFetchWaitLine,
      })
    )
  }

  function doFullRedraw() {
    const {
      contentLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
      currentPromptSgr,
      interactiveFetchWaitLine,
    } = getDisplayContent()
    const liveLines = buildLiveRegionLines(
      buffer,
      getTerminalWidth(),
      currentPromptWrappedLines,
      suggestionLines,
      recallingIndicator,
      { placeholderContext, currentPromptSgr }
    )
    const newTotalLines = liveLines.length

    process.stdout.write(CLEAR_SCREEN)
    const fullLines = renderFullDisplay(
      chatHistory,
      buffer,
      getTerminalWidth(),
      suggestionLines,
      recallingIndicator,
      currentPromptWrappedLines,
      { placeholderContext, currentPromptSgr }
    )
    for (const line of fullLines) {
      process.stdout.write(`${line}\n`)
    }

    const inputRow = inputRowFromTop(currentPromptLines, contentLines.length)
    livePaint.cursorUpStepsToLiveRegionTop = inputRow
    livePaint.lastPaintedLineCount = newTotalLines

    process.stdout.write(`\x1b[${newTotalLines - inputRow}A`)
    finalizeInteractiveLiveRegionPaint(
      placeholderContext,
      interactiveFetchWaitLine
    )
  }

  function drawBox() {
    const {
      contentLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
      currentPromptSgr,
      interactiveFetchWaitLine,
    } = getDisplayContent()
    const liveLines = buildLiveRegionLines(
      buffer,
      getTerminalWidth(),
      currentPromptWrappedLines,
      suggestionLines,
      recallingIndicator,
      { placeholderContext, currentPromptSgr }
    )
    const newTotalLines = liveLines.length

    if (livePaint.cursorUpStepsToLiveRegionTop > 0) {
      process.stdout.write(`\x1b[${livePaint.cursorUpStepsToLiveRegionTop}A`)
    } else if (
      livePaint.lastPaintedLineCount === 0 &&
      needsGapBeforeBox(chatHistory, currentPromptWrappedLines)
    ) {
      process.stdout.write('\n')
    }
    process.stdout.write('\r')

    for (const line of liveLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    const extra = livePaint.lastPaintedLineCount - newTotalLines
    for (let i = 0; i < extra; i++) {
      process.stdout.write('\x1b[2K\n')
    }

    const totalWritten = Math.max(newTotalLines, livePaint.lastPaintedLineCount)
    const inputRow = inputRowFromTop(currentPromptLines, contentLines.length)
    process.stdout.write(`\x1b[${totalWritten - inputRow}A`)
    finalizeInteractiveLiveRegionPaint(
      placeholderContext,
      interactiveFetchWaitLine
    )

    livePaint.cursorUpStepsToLiveRegionTop = inputRow
    livePaint.lastPaintedLineCount = newTotalLines
  }

  function stopInteractiveFetchWaitRepaintTimer(): void {
    if (interactiveFetchWaitRepaintTimer) {
      clearInterval(interactiveFetchWaitRepaintTimer)
      interactiveFetchWaitRepaintTimer = null
    }
    interactiveFetchWaitEllipsisTick = 0
  }

  ttyOutput = {
    log: (msg) => {
      process.stdout.write(`${msg}\n`)
      collectedOutputLines.push(...msg.split('\n'))
    },
    logError: (err) => {
      const msg = err instanceof Error ? err.message : String(err)
      writeError(err)
      collectedOutputLines.push(msg)
    },
    writeCurrentPrompt: writeCurrentPromptLine,
    beginCurrentPrompt: doBeginCurrentPrompt,
    clearAndRedraw: () => {
      chatHistory = []
      buffer = ''
      tokenListItems = null
      tokenHighlightIndex = 0
      if (isInRecallSubstate()) exitRecallMode()
      setPendingRecallStopConfirmation(false)
      mcqChoiceHighlightIndex = 0
      highlightIndex = 0
      suggestionsDismissed = false
      doFullRedraw()
    },
    onInteractiveFetchWaitChanged: () => {
      if (getInteractiveFetchWaitLine()) {
        stopInteractiveFetchWaitRepaintTimer()
        drawBox()
        interactiveFetchWaitRepaintTimer = setInterval(() => {
          interactiveFetchWaitEllipsisTick =
            (interactiveFetchWaitEllipsisTick + 1) % 3
          drawBox()
        }, INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS)
      } else {
        stopInteractiveFetchWaitRepaintTimer()
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
        buffer = ''
        drawBox()
      } else if (submitPressed && !key.shift) {
        const trimmed = buffer.trim()
        const answer = trimmed.toLowerCase()
        const isYes = answer === 'y' || answer === 'yes'
        const isNo = answer === 'n' || answer === 'no'
        buffer = ''
        setPendingRecallStopConfirmation(false)
        if (isYes) {
          exitRecallMode()
          mcqChoiceHighlightIndex = 0
          livePaint.cursorUpStepsToLiveRegionTop = 0
          livePaint.lastPaintedLineCount = 0
          process.stdout.write('Stopped recall\n')
          chatHistory.push({ type: 'input', content: trimmed })
          chatHistory.push({ type: 'output', lines: ['Stopped recall'] })
        } else if (isNo) {
          // redraw below
        } else if (trimmed) {
          writeCurrentPromptLine('Please answer y or n')
          setPendingRecallStopConfirmation(true)
        }
        drawBox()
      } else if (str && !key.ctrl && !key.meta) {
        buffer += str
        drawBox()
      } else if (key.name === 'backspace') {
        if (buffer.length > 0) {
          buffer = buffer.slice(0, -1)
          drawBox()
        }
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
        buffer = ''
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
        const effectiveInput =
          trimmedBuffer === '/stop'
            ? '/stop'
            : trimmedBuffer === '/contest'
              ? '/contest'
              : (() => {
                  const choiceNum = Number.parseInt(trimmedBuffer, 10)
                  const validTyped =
                    choiceNum >= 1 && choiceNum <= choices.length
                  return validTyped
                    ? String(choiceNum)
                    : String(mcqChoiceHighlightIndex + 1)
                })()
        clearLiveRegionForRepaint(livePaint)
        const inputForHistory = buffer || effectiveInput
        buffer = ''
        mcqChoiceHighlightIndex = 0
        livePaint.lastPaintedLineCount = 0
        collectedOutputLines.length = 0
        chatHistory.push({ type: 'input', content: inputForHistory })
        if (await processInput(effectiveInput, ttyOutput)) {
          doExit()
          return
        }
        chatHistory.push({ type: 'output', lines: [...collectedOutputLines] })
        drawBox()
      } else if (str && !key.ctrl && !key.meta) {
        buffer += str
        drawBox()
      } else if (key.name === 'backspace') {
        if (buffer.length > 0) {
          buffer = buffer.slice(0, -1)
          drawBox()
        }
      } else {
        drawBox()
      }
      return
    }
    if (tokenListItems) {
      if (key.name === 'up' || key.name === 'down') {
        const delta = key.name === 'up' ? -1 : 1
        tokenHighlightIndex = cycleIndex(
          tokenHighlightIndex,
          delta,
          tokenListItems.length
        )
        drawBox()
      } else if (submitPressed && !key.shift) {
        const selectedLabel = tokenListItems[tokenHighlightIndex]!.label
        const action = tokenListAction
        let outputMsg = ''
        if (action === 'set-default') {
          setDefaultTokenLabel(selectedLabel)
          outputMsg = `Default token set to: ${selectedLabel}`
        } else if (action === 'remove') {
          removeAccessToken(selectedLabel)
          outputMsg = `Token "${selectedLabel}" removed.`
        } else {
          try {
            await runInteractiveFetchWait(
              ttyOutput,
              INTERACTIVE_FETCH_WAIT_LINES.removeAccessTokenCompletely,
              (signal) => removeAccessTokenCompletely(selectedLabel, signal)
            )
            outputMsg = `Token "${selectedLabel}" removed locally and from server.`
          } catch (err) {
            if (isFetchAbortedByCaller(err)) {
              outputMsg = 'Cancelled by user.'
            } else {
              writeError(err)
              outputMsg = err instanceof Error ? err.message : String(err)
            }
          }
        }
        endTokenListSelection(outputMsg)
      } else {
        endTokenListSelection('Cancelled by user.')
      }
      return
    }
    if (key.name === 'escape') {
      if (isInRecallSubstate()) {
        exitRecallMode()
        buffer = ''
        mcqChoiceHighlightIndex = 0
        livePaint.cursorUpStepsToLiveRegionTop = 0
        livePaint.lastPaintedLineCount = 0
        drawBox()
        return
      }
      if (isCommandPrefixWithSuggestions(buffer)) {
        highlightIndex = 0
        const lastLine = getLastLine(buffer)
        if (lastLine === '/') {
          const bufferLines = buffer.split('\n')
          buffer =
            bufferLines.length === 1 ? '' : bufferLines.slice(0, -1).join('\n')
        } else {
          suggestionsDismissed = true
        }
        drawBox()
      }
      return
    }
    if (submitPressed) {
      if (key.shift) {
        buffer += '\n'
        drawBox()
      } else {
        const trimmedInput = buffer.trim()

        if (trimmedInput === '/clear') {
          chatHistory = []
          buffer = ''
          tokenListItems = null
          tokenHighlightIndex = 0
          if (isInRecallSubstate()) exitRecallMode()
          setPendingRecallStopConfirmation(false)
          mcqChoiceHighlightIndex = 0
          highlightIndex = 0
          suggestionsDismissed = false
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
          buffer =
            bufferLines.slice(0, -1).concat(selectedCommand).join('\n') || ''
          highlightIndex = 0
          drawBox()
          return
        }

        const width = getTerminalWidth()
        const input = buffer
        buffer = ''

        clearLiveRegionForRepaint(livePaint)

        if (isCommittedInteractiveInput(input)) {
          process.stdout.write(renderPastInput(input, width))
          process.stdout.write('\n')
        }

        const tokenSelect = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
        if (tokenSelect) {
          const tokens = listAccessTokens()
          if (tokens.length === 0) {
            process.stdout.write('No access tokens stored.\n')
            chatHistory.push({ type: 'input', content: trimmedInput })
            chatHistory.push({
              type: 'output',
              lines: ['No access tokens stored.'],
            })
          } else {
            tokenListItems = tokens
            tokenListCommand = trimmedInput
            tokenListAction = tokenSelect.action
            const dl = getDefaultTokenLabel()
            tokenHighlightIndex = Math.max(
              0,
              tokens.findIndex((t) => t.label === dl)
            )
          }
          livePaint.lastPaintedLineCount = 0
          drawBox()
          return
        }

        collectedOutputLines.length = 0
        if (isCommittedInteractiveInput(input)) {
          chatHistory.push({ type: 'input', content: input })
          if (await processInput(input, ttyOutput)) {
            doExit()
          }
          chatHistory.push({ type: 'output', lines: [...collectedOutputLines] })
          livePaint.cursorUpStepsToLiveRegionTop = 0
          livePaint.lastPaintedLineCount = 0
        }
        if (isMcqPrompt(getPendingRecallAnswer())) {
          mcqChoiceHighlightIndex = 0
        }
        drawBox()
      }
    } else if (key.name === 'backspace') {
      if (buffer.length > 0) {
        buffer = buffer.slice(0, -1)
        highlightIndex = 0
        suggestionsDismissed = false
        drawBox()
      }
    } else if (key.name === 'up' || key.name === 'down') {
      if (isCommandPrefixWithSuggestions(buffer)) {
        const filtered = filterCommandsByPrefix(
          interactiveDocs,
          getLastLine(buffer)
        )
        const delta = key.name === 'up' ? -1 : 1
        highlightIndex = cycleIndex(highlightIndex, delta, filtered.length)
        drawBox()
      }
    } else if (key.name === 'tab') {
      const lastLine = getLastLine(buffer)
      if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
        const { completed, count } = getTabCompletion(lastLine, interactiveDocs)
        if (count > 0 && completed !== lastLine) {
          const bufferLines = buffer.split('\n')
          buffer =
            bufferLines.slice(0, -1).concat(completed).join('\n') || completed
          highlightIndex = 0
          suggestionsDismissed = false
          drawBox()
        }
      }
    } else if (str && !key.ctrl && !key.meta) {
      buffer += str
      highlightIndex = 0
      suggestionsDismissed = false
      drawBox()
    }
  })
}
