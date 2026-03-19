import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import type { AccessTokenEntry } from '../accessToken.js'
import type { CommandDoc } from '../help.js'
import { wrapTextToLines, type PlaceholderContext } from '../renderer.js'
import type { ChatHistory, OutputAdapter } from '../types.js'

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
  isMcqPrompt: (p: unknown) => boolean
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
  renderBox: (lines: string[], width: number) => string
  renderFullDisplay: (
    history: ChatHistory,
    buffer: string,
    width: number,
    suggestionLines: string[],
    recallingIndicator: string[],
    currentPromptLines?: string[],
    options?: { placeholderContext?: PlaceholderContext }
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
  grayBoxLinesForSelectionMode: (lines: string[]) => string[]
  isSelectionMode: (ctx: PlaceholderContext) => boolean
}

function cycleIndex(current: number, delta: number, length: number): number {
  return (current + delta + length) % length
}

function clearTTYDisplay(
  linesAboveCursor: number,
  prevTotalLines: number
): void {
  if (linesAboveCursor > 0) {
    process.stdout.write(`\x1b[${linesAboveCursor}A`)
  }
  process.stdout.write('\r')
  for (let i = 0; i < prevTotalLines; i++) {
    process.stdout.write('\x1b[2K\n')
  }
  if (prevTotalLines > 1) {
    process.stdout.write(`\x1b[${prevTotalLines - 1}A`)
  }
}

function isSubmitKey(keyName: string): boolean {
  return keyName === 'return' || keyName === 'enter'
}

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
    renderBox,
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
    grayBoxLinesForSelectionMode,
    isSelectionMode,
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
  let linesAboveCursor = 0
  let prevTotalLines = 0
  let tokenListItems: AccessTokenEntry[] | null = null
  let tokenListCommand = ''
  let tokenHighlightIndex = 0
  let tokenListAction: TokenListAction = 'set-default'
  let mcqChoiceHighlightIndex = 0

  const collectedOutputLines: string[] = []
  let ttyOutput: OutputAdapter

  /** Builds lines for input box, recalling indicator, Current prompt (above box), and Current guidance (below box). */
  function getDisplayContent() {
    const width = getTerminalWidth()
    const placeholderContext = getPlaceholderContext(!!tokenListItems)
    const contentLines = buildBoxLines(buffer, width, {
      placeholderContext,
    })
    let boxLines = renderBox(contentLines, width).split('\n')
    if (isSelectionMode(placeholderContext)) {
      boxLines = grayBoxLinesForSelectionMode(boxLines)
    }
    const pendingRecallAnswer = getPendingRecallAnswer()
    const currentPromptText =
      tokenListItems && tokenListCommand
        ? TOKEN_LIST_COMMANDS[tokenListCommand]?.currentPrompt
        : undefined
    const currentPromptWrappedLines = currentPromptText
      ? wrapTextToLines(currentPromptText, width)
      : []
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
              formatMcqChoiceLines(
                (pendingRecallAnswer as { choices: string[] }).choices
              ),
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
      boxLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
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

  function doFullRedraw() {
    const {
      contentLines,
      boxLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
    } = getDisplayContent()
    const newTotalLines =
      currentPromptLines +
      boxLines.length +
      recallingIndicator.length +
      suggestionLines.length

    process.stdout.write(CLEAR_SCREEN)
    const fullLines = renderFullDisplay(
      chatHistory,
      buffer,
      getTerminalWidth(),
      suggestionLines,
      recallingIndicator,
      currentPromptWrappedLines,
      { placeholderContext }
    )
    for (const line of fullLines) {
      process.stdout.write(`${line}\n`)
    }

    const inputRow = inputRowFromTop(currentPromptLines, contentLines.length)
    linesAboveCursor = inputRow
    prevTotalLines = newTotalLines

    process.stdout.write(`\x1b[${newTotalLines - inputRow}A`)
    if (isSelectionMode(placeholderContext)) {
      process.stdout.write(HIDE_CURSOR)
    } else {
      process.stdout.write(SHOW_CURSOR)
      positionCursorInInputBox()
    }
  }

  function drawBox() {
    const {
      contentLines,
      boxLines,
      currentPromptWrappedLines,
      currentPromptLines,
      suggestionLines,
      recallingIndicator,
      placeholderContext,
    } = getDisplayContent()
    const newTotalLines =
      currentPromptLines +
      boxLines.length +
      recallingIndicator.length +
      suggestionLines.length

    if (linesAboveCursor > 0) {
      process.stdout.write(`\x1b[${linesAboveCursor}A`)
    }
    process.stdout.write('\r')

    if (currentPromptWrappedLines.length > 0) {
      process.stdout.write(
        `\x1b[2K${buildCurrentPromptSeparator(getTerminalWidth())}\n`
      )
      for (const line of currentPromptWrappedLines) {
        process.stdout.write(`\x1b[2K${GREY}${line}\x1b[0m\n`)
      }
    }
    for (const line of boxLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    for (const line of recallingIndicator) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    for (const line of suggestionLines) {
      process.stdout.write(`\x1b[2K${line}\n`)
    }
    const extra = prevTotalLines - newTotalLines
    for (let i = 0; i < extra; i++) {
      process.stdout.write('\x1b[2K\n')
    }

    const totalWritten = Math.max(newTotalLines, prevTotalLines)
    const inputRow = inputRowFromTop(currentPromptLines, contentLines.length)
    process.stdout.write(`\x1b[${totalWritten - inputRow}A`)
    if (isSelectionMode(placeholderContext)) {
      process.stdout.write(HIDE_CURSOR)
    } else {
      process.stdout.write(SHOW_CURSOR)
      positionCursorInInputBox()
    }

    linesAboveCursor = inputRow
    prevTotalLines = newTotalLines
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
  }

  drawBox()

  process.stdout.on('resize', doFullRedraw)
  const removeResizeListener = () => process.stdout.off('resize', doFullRedraw)
  const doExit = () => {
    removeResizeListener()
    process.stdout.write(SHOW_CURSOR)
    rl.close()
    process.exit(0)
  }

  stdin.on(
    'keypress',
    async (
      str: string,
      key: { name: string; shift?: boolean; ctrl?: boolean; meta?: boolean }
    ) => {
      const submitPressed =
        isSubmitKey(key.name) || str === '\n' || str === '\r'
      if (key.ctrl && key.name === 'c') {
        process.stdout.write(`\x1b[${1}B\r\n`)
        doExit()
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
            linesAboveCursor = 0
            prevTotalLines = 0
            process.stdout.write('Stopped recall\n')
            chatHistory.push({ type: 'input', content: trimmed })
            chatHistory.push({ type: 'output', lines: ['Stopped recall'] })
          } else if (isNo) {
            // Stay in MCQ; drawBox will show choices again
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
        const choices = (pendingRecallAnswer as { choices: string[] }).choices
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
          clearTTYDisplay(linesAboveCursor, prevTotalLines)
          const inputForHistory = buffer || effectiveInput
          buffer = ''
          mcqChoiceHighlightIndex = 0
          linesAboveCursor = 0
          prevTotalLines = 0
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
          clearTTYDisplay(linesAboveCursor, prevTotalLines)
          const action = tokenListAction
          tokenListItems = null
          tokenHighlightIndex = 0
          tokenListAction = 'set-default'
          linesAboveCursor = 0
          prevTotalLines = 0
          let outputMsg = ''
          if (action === 'set-default') {
            setDefaultTokenLabel(selectedLabel)
            outputMsg = `Default token set to: ${selectedLabel}`
            process.stdout.write(`${outputMsg}\n`)
          } else if (action === 'remove') {
            removeAccessToken(selectedLabel)
            outputMsg = `Token "${selectedLabel}" removed.`
            process.stdout.write(`${outputMsg}\n`)
          } else {
            try {
              await removeAccessTokenCompletely(selectedLabel)
              outputMsg = `Token "${selectedLabel}" removed locally and from server.`
              process.stdout.write(`${outputMsg}\n`)
            } catch (err) {
              writeError(err)
              outputMsg = err instanceof Error ? err.message : String(err)
            }
          }
          chatHistory.push({ type: 'input', content: tokenListCommand })
          chatHistory.push({ type: 'output', lines: [outputMsg] })
          drawBox()
        } else {
          const command = tokenListCommand
          clearTTYDisplay(linesAboveCursor, prevTotalLines)
          process.stdout.write('Cancelled by user.\n')
          chatHistory.push({ type: 'input', content: command })
          chatHistory.push({ type: 'output', lines: ['Cancelled by user.'] })
          tokenListItems = null
          tokenListCommand = ''
          tokenHighlightIndex = 0
          tokenListAction = 'set-default'
          buffer = ''
          linesAboveCursor = 0
          prevTotalLines = 0
          drawBox()
        }
        return
      }
      if (key.name === 'escape') {
        if (isInRecallSubstate()) {
          exitRecallMode()
          buffer = ''
          mcqChoiceHighlightIndex = 0
          linesAboveCursor = 0
          prevTotalLines = 0
          drawBox()
          return
        }
        if (isCommandPrefixWithSuggestions(buffer)) {
          highlightIndex = 0
          const lastLine = getLastLine(buffer)
          if (lastLine === '/') {
            const bufferLines = buffer.split('\n')
            buffer =
              bufferLines.length === 1
                ? ''
                : bufferLines.slice(0, -1).join('\n')
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
            linesAboveCursor = 0
            prevTotalLines = 0
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

          clearTTYDisplay(linesAboveCursor, prevTotalLines)

          if (input.trim()) {
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
            linesAboveCursor = 0
            prevTotalLines = 0
            drawBox()
            return
          }

          collectedOutputLines.length = 0
          chatHistory.push({ type: 'input', content: input })
          if (await processInput(input, ttyOutput)) {
            doExit()
          }
          chatHistory.push({ type: 'output', lines: [...collectedOutputLines] })
          linesAboveCursor = 0
          prevTotalLines = 0
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
          const { completed, count } = getTabCompletion(
            lastLine,
            interactiveDocs
          )
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
    }
  )
}
