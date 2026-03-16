import * as readline from 'node:readline'
import {
  addAccessToken,
  createAccessToken,
  formatTokenLines,
  getDefaultTokenLabel,
  listAccessTokens,
  removeAccessToken,
  removeAccessTokenCompletely,
  setDefaultTokenLabel,
} from './accessToken.js'
import { addGmailAccount, getLastEmailSubject } from './gmail.js'
import { renderMarkdownToTerminal } from './markdown.js'
import {
  answerQuiz,
  answerSpelling,
  contestAndRegenerate,
  markAsRecalled,
  recallNext,
  recallStatus,
  type RecallNextResult,
} from './recall.js'
import {
  filterCommandsByPrefix,
  formatCommandSuggestionsWithHighlight,
  formatHelp,
  getTabCompletion,
  interactiveDocs,
} from './help.js'
import { formatHighlightedList } from './listDisplay.js'
import { formatVersionOutput } from './version.js'

const GREY = '\x1b[90m'
const GREY_BG = '\x1b[48;5;236m'
const RESET = '\x1b[0m'
const COMMAND_HIGHLIGHT = '\x1b[1;36m' // bold + cyan

type StatusWriter = (msg: string) => void

const logStatus: StatusWriter = (msg) => console.log(`${GREY}${msg}${RESET}`)
const writeStatus: StatusWriter = (msg) =>
  process.stdout.write(`${GREY}${msg}${RESET}\n`)

function logError(err: unknown): void {
  console.log(err instanceof Error ? err.message : String(err))
}

function writeError(err: unknown): void {
  process.stdout.write(`${err instanceof Error ? err.message : String(err)}\n`)
}

function parseYesNo(input: string): boolean | null {
  const a = input.trim().toLowerCase()
  if (a === 'y' || a === 'yes') return true
  if (a === 'n' || a === 'no') return false
  return null
}

function cycleIndex(current: number, delta: number, length: number): number {
  return (current + delta + length) % length
}

type RecallPromptResult = Exclude<RecallNextResult, { type: 'none' }>

/** User-submitted input in the prompt (what they typed). */
type ChatHistoryInputEntry = { type: 'input'; content: string }
/** Command output lines (what was displayed in response). */
type ChatHistoryOutputEntry = { type: 'output'; lines: readonly string[] }
type ChatHistoryEntry = ChatHistoryInputEntry | ChatHistoryOutputEntry
/** Ordered log of user inputs and command outputs for re-render on resize. */
type ChatHistory = ChatHistoryEntry[]

type McqPrompt = { recallPromptId: number; choices: string[]; shownAt: number }
type SpellingPrompt = {
  recallPromptId: number
  type: 'spelling'
  shownAt: number
}
type JustReviewPrompt = { memoryTrackerId: number }

function formatRecallSessionSummary(count: number): string {
  if (count === 0) return '0 notes to recall today'
  if (count === 1) return 'Recalled 1 note'
  return `Recalled ${count} notes`
}

function showRecallPrompt(
  result: RecallPromptResult,
  status: StatusWriter = logStatus
): void {
  if (result.type === 'spelling') {
    status(`Spell: ${renderMarkdownToTerminal(result.stem || '...')}`)
    pendingRecallAnswer = {
      recallPromptId: result.recallPromptId,
      type: 'spelling',
      shownAt: Date.now(),
    }
    return
  }
  if (result.type === 'mcq') {
    status(renderMarkdownToTerminal(result.stem))
    for (const line of formatMcqChoiceLines(result.choices)) {
      status(line)
    }
    status(`Enter your choice (1-${result.choices.length}):`)
    pendingRecallAnswer = {
      recallPromptId: result.recallPromptId,
      choices: result.choices,
      shownAt: Date.now(),
    }
    return
  }
  status(result.title)
  if (result.details) {
    status(renderMarkdownToTerminal(result.details))
  }
  status('Yes, I remember? (y/n)')
  pendingRecallAnswer = { memoryTrackerId: result.memoryTrackerId }
}

function isMcqPrompt(p: typeof pendingRecallAnswer): p is McqPrompt {
  return p !== null && 'choices' in p
}

function isSpellingPrompt(p: typeof pendingRecallAnswer): p is SpellingPrompt {
  return p !== null && 'type' in p && p.type === 'spelling'
}

function getContestablePromptId(): number | null {
  return isMcqPrompt(pendingRecallAnswer) ||
    isSpellingPrompt(pendingRecallAnswer)
    ? pendingRecallAnswer.recallPromptId
    : null
}

const TOKEN_LIST_COMMANDS: Record<
  string,
  'set-default' | 'remove' | 'remove-completely'
> = {
  '/list-access-token': 'set-default',
  '/remove-access-token': 'remove',
  '/remove-access-token-completely': 'remove-completely',
}

const PLACEHOLDER = '`exit` to quit.'
const PROMPT = '→ '

let pendingRecallAnswer:
  | { memoryTrackerId: number }
  | { recallPromptId: number; choices: string[]; shownAt: number }
  | { recallPromptId: number; type: 'spelling'; shownAt: number }
  | null = null

let recallSessionMode = false
let sessionRecallCount = 0
let recallSessionDueDays = 0
let pendingRecallLoadMore = false
let pendingRecallStopConfirmation = false

export function resetRecallStateForTesting(): void {
  pendingRecallAnswer = null
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
  pendingRecallStopConfirmation = false
}

export function isInRecallSubstate(): boolean {
  return (
    recallSessionMode || pendingRecallAnswer !== null || pendingRecallLoadMore
  )
}

function exitRecallMode(): void {
  recallSessionMode = false
  pendingRecallAnswer = null
  sessionRecallCount = 0
  recallSessionDueDays = 0
  pendingRecallLoadMore = false
  pendingRecallStopConfirmation = false
}

function endRecallSession(): void {
  recallSessionMode = false
  sessionRecallCount = 0
  recallSessionDueDays = 0
}

async function continueRecallSession(fromLoadMore = false): Promise<void> {
  if (!fromLoadMore) sessionRecallCount++
  try {
    const result = await recallNext(recallSessionDueDays)
    if (result.type === 'none') {
      if (recallSessionDueDays === 0) {
        pendingRecallLoadMore = true
        logStatus('Load more from next 3 days? (y/n)')
        return
      }
      console.log(formatRecallSessionSummary(sessionRecallCount))
      endRecallSession()
      return
    }
    showRecallPrompt(result)
  } catch (err) {
    endRecallSession()
    logError(err)
  }
}

function parseCommandWithRequiredParam(
  trimmed: string,
  command: string
): string | 'usage' | null {
  if (trimmed !== command && !trimmed.startsWith(`${command} `)) return null
  const param = trimmed.slice(command.length).trim()
  return param ? param : 'usage'
}

type ParamCommandResult = string | undefined

const PARAM_COMMANDS: Array<{
  command: string
  usage: string
  run: (param: string) => Promise<ParamCommandResult> | ParamCommandResult
}> = [
  {
    command: '/add-access-token',
    usage: 'Usage: /add-access-token <token>',
    run: async (param) => {
      await addAccessToken(param)
      return 'Token added'
    },
  },
  {
    command: '/create-access-token',
    usage: 'Usage: /create-access-token <label>',
    run: async (param) => {
      await createAccessToken(param)
      return 'Token created'
    },
  },
  {
    command: '/remove-access-token-completely',
    usage: 'Usage: /remove-access-token-completely <label>',
    run: async (param) => {
      await removeAccessTokenCompletely(param)
      return `Token "${param}" removed locally and from server.`
    },
  },
  {
    command: '/remove-access-token',
    usage: 'Usage: /remove-access-token <label>',
    run: (param) => {
      if (removeAccessToken(param)) return `Token "${param}" removed.`
      return `Token "${param}" not found.`
    },
  },
]

async function handleParamCommand(
  trimmed: string,
  output: OutputAdapter
): Promise<boolean> {
  for (const { command, usage, run } of PARAM_COMMANDS) {
    const param = parseCommandWithRequiredParam(trimmed, command)
    if (param === null) continue
    if (param === 'usage') {
      output.log(usage)
      return true
    }
    try {
      const msg = await run(param)
      if (msg) output.log(msg)
    } catch (err) {
      logError(err)
    }
    return true
  }
  return false
}

type OutputAdapter = {
  log: (msg: string) => void
  logError: (err: unknown) => void
}

const defaultOutput: OutputAdapter = {
  log: (msg) => console.log(msg),
  logError,
}

export async function processInput(
  input: string,
  output: OutputAdapter = defaultOutput
): Promise<boolean> {
  const trimmed = input.trim()
  if (trimmed === 'exit' || trimmed === '/exit') {
    return true
  }
  if (isInRecallSubstate() && trimmed === '/stop') {
    exitRecallMode()
    output.log('Stopped recall')
    return false
  }
  const contestablePromptId = getContestablePromptId()
  if (isInRecallSubstate() && trimmed === '/contest') {
    if (contestablePromptId == null) {
      logStatus('Type /stop to exit recall')
      return false
    }
    try {
      const outcome = await contestAndRegenerate(contestablePromptId)
      if (!outcome.ok) {
        output.log(outcome.message)
        return false
      }
      showRecallPrompt(outcome.result as RecallPromptResult)
    } catch (err) {
      output.logError(err)
    }
    return false
  }
  if (isInRecallSubstate() && trimmed.startsWith('/')) {
    logStatus(
      contestablePromptId
        ? 'Type /stop to exit, /contest to regenerate'
        : 'Type /stop to exit recall'
    )
    return false
  }
  if (trimmed === '/help') {
    output.log(formatHelp())
    return false
  }
  if (trimmed === '/clear') {
    writeFullRedraw([], '', getTerminalWidth(), buildSuggestionLines('', 0), [])
    return false
  }
  if (await handleParamCommand(trimmed, output)) {
    return false
  }
  if (trimmed === '/list-access-token') {
    const tokens = listAccessTokens()
    if (tokens.length === 0) {
      output.log('No access tokens stored.')
    } else {
      for (const line of formatTokenLines(tokens, getDefaultTokenLabel())) {
        output.log(line)
      }
    }
    return false
  }
  if (trimmed === '/add gmail') {
    try {
      await addGmailAccount()
    } catch (err) {
      output.logError(err)
    }
    return false
  }
  if (trimmed === '/last email') {
    try {
      const subject = await getLastEmailSubject()
      output.log(subject)
    } catch (err) {
      output.logError(err)
    }
    return false
  }
  if (pendingRecallLoadMore) {
    const answer = parseYesNo(trimmed)
    if (answer === true) {
      pendingRecallLoadMore = false
      recallSessionDueDays = 3
      await continueRecallSession(true)
    } else if (answer === false) {
      pendingRecallLoadMore = false
      output.log(formatRecallSessionSummary(sessionRecallCount))
      endRecallSession()
    } else {
      logStatus('Please answer y or n')
      return false
    }
    return false
  }
  if (pendingRecallAnswer) {
    if (isMcqPrompt(pendingRecallAnswer)) {
      const choiceNum = Number.parseInt(trimmed, 10)
      const { recallPromptId, choices } = pendingRecallAnswer
      const validRange = choiceNum >= 1 && choiceNum <= choices.length
      if (validRange) {
        try {
          const thinkingTimeMs = Date.now() - pendingRecallAnswer.shownAt
          const { correct } = await answerQuiz(
            recallPromptId,
            choiceNum - 1,
            thinkingTimeMs
          )
          output.log(correct ? 'Correct!' : 'Incorrect')
          output.log('Recalled successfully')
        } catch (err) {
          output.logError(err)
        }
        pendingRecallAnswer = null
        if (recallSessionMode) await continueRecallSession()
      } else {
        logStatus(`Enter a number from 1 to ${choices.length}`)
        return false
      }
    } else if (isSpellingPrompt(pendingRecallAnswer)) {
      const { recallPromptId } = pendingRecallAnswer
      if (!trimmed) {
        logStatus('Please type your spelling')
        return false
      }
      try {
        const thinkingTimeMs = Date.now() - pendingRecallAnswer.shownAt
        const { correct } = await answerSpelling(
          recallPromptId,
          trimmed,
          thinkingTimeMs
        )
        output.log(correct ? 'Correct!' : 'Incorrect')
        output.log('Recalled successfully')
      } catch (err) {
        output.logError(err)
      }
      pendingRecallAnswer = null
      if (recallSessionMode) await continueRecallSession()
    } else {
      const { memoryTrackerId } = pendingRecallAnswer as JustReviewPrompt
      const answer = parseYesNo(trimmed)
      if (answer === true) {
        try {
          await markAsRecalled(memoryTrackerId, true)
          output.log('Recalled successfully')
        } catch (err) {
          output.logError(err)
        }
      } else if (answer === false) {
        try {
          await markAsRecalled(memoryTrackerId, false)
          output.log('Marked as not recalled')
        } catch (err) {
          output.logError(err)
        }
      } else {
        logStatus('Please answer y or n')
        return false
      }
      pendingRecallAnswer = null
      if (recallSessionMode) await continueRecallSession()
    }
    return false
  }
  if (trimmed === '/recall-status') {
    try {
      const message = await recallStatus()
      output.log(message)
    } catch (err) {
      output.logError(err)
    }
    return false
  }
  if (trimmed === '/recall') {
    try {
      recallSessionMode = true
      sessionRecallCount = 0
      recallSessionDueDays = 0
      const result = await recallNext(0)
      if (result.type === 'none') {
        pendingRecallLoadMore = true
        logStatus('Load more from next 3 days? (y/n)')
      } else {
        showRecallPrompt(result as RecallPromptResult)
      }
    } catch (err) {
      endRecallSession()
      output.logError(err)
    }
    return false
  }
  if (trimmed) {
    output.log('Not supported')
  }
  return false
}

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g

export function visibleLength(str: string): number {
  return str.replace(ANSI_PATTERN, '').length
}

function padEndVisible(str: string, targetLen: number): string {
  const pad = targetLen - visibleLength(str)
  return pad > 0 ? str + ' '.repeat(pad) : str
}

export function renderBox(lines: string[], width: number): string {
  const innerWidth = width - 4
  const top = `┌${'─'.repeat(width - 2)}┐`
  const bottom = `└${'─'.repeat(width - 2)}┘`
  const rows = lines.map((line) => `│ ${padEndVisible(line, innerWidth)} │`)
  return [top, ...rows, bottom].join('\n')
}

export function renderPastInput(input: string, width: number): string {
  const innerWidth = width - 2
  const lines = input.split('\n')
  const emptyRow = `${GREY_BG}${' '.repeat(innerWidth)}${RESET}`
  const contentRows = lines.map(
    (line) => `${GREY_BG} ${padEndVisible(line, innerWidth - 1)}${RESET}`
  )
  return [emptyRow, ...contentRows, emptyRow, ''].join('\n')
}

export function highlightRecognizedCommand(line: string): string {
  if (!line.startsWith('/')) return line

  const usages = interactiveDocs.map((d) => d.usage)
  let highlightLen = 0

  for (const cmd of usages) {
    if (line.startsWith(cmd)) {
      highlightLen = Math.max(highlightLen, cmd.length)
    }
  }

  if (highlightLen === 0) return line

  const prefix = line.slice(0, highlightLen)
  const rest = line.slice(highlightLen)
  return `${COMMAND_HIGHLIGHT}${prefix}${RESET}${rest}`
}

export function buildBoxLines(buffer: string, width: number): string[] {
  const bufferLines = buffer.split('\n')
  return bufferLines.map((line, i) => {
    const prefix = i === 0 ? PROMPT : '  '
    if (i === 0 && buffer === '') {
      return `${prefix}${GREY}${PLACEHOLDER}${RESET}`
    }
    const highlighted = highlightRecognizedCommand(line)
    return prefix + highlighted
  })
}

function getTerminalWidth(): number {
  return process.stdout.columns || 80
}

const CLEAR_SCREEN = '\x1b[H\x1b[2J'
const COMMANDS_HINT = `${GREY}  / commands${RESET}`
const RECALLING_INDICATOR = `${GREY}Recalling${RESET}`

function renderFullDisplay(
  history: ChatHistory,
  buffer: string,
  width: number,
  suggestionLines: string[],
  recallingIndicator: string[]
): string[] {
  const lines: string[] = [formatVersionOutput(), '']
  for (const entry of history) {
    if (entry.type === 'input') {
      lines.push(...renderPastInput(entry.content, width).split('\n'))
    } else {
      lines.push(...entry.lines)
    }
  }
  const boxLines = renderBox(buildBoxLines(buffer, width), width).split('\n')
  lines.push(...boxLines)
  lines.push(...recallingIndicator)
  lines.push(...suggestionLines)
  return lines
}

function writeFullRedraw(
  history: ChatHistory,
  buffer: string,
  width: number,
  suggestionLines: string[],
  recallingIndicator: string[]
): void {
  process.stdout.write(CLEAR_SCREEN)
  const lines = renderFullDisplay(
    history,
    buffer,
    width,
    suggestionLines,
    recallingIndicator
  )
  for (const line of lines) {
    process.stdout.write(`${line}\n`)
  }
}

function formatMcqChoiceLines(choices: string[]): string[] {
  return choices.map((c, i) => `  ${i + 1}. ${renderMarkdownToTerminal(c)}`)
}

function buildSuggestionLines(
  buffer: string,
  highlightIndex: number
): string[] {
  const lastLine = getLastLine(buffer)
  if (lastLine.startsWith('/') && !lastLine.endsWith(' ')) {
    const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
    return formatCommandSuggestionsWithHighlight(filtered, 8, highlightIndex)
  }
  return [COMMANDS_HINT]
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

function getLastLine(buffer: string): string {
  const lines = buffer.split('\n')
  return lines[lines.length - 1] ?? ''
}

function isCommandPrefixWithSuggestions(buffer: string): boolean {
  const lastLine = getLastLine(buffer)
  if (!lastLine.startsWith('/') || lastLine.endsWith(' ')) return false
  const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
  return filtered.length > 0
}

async function runInteractiveTTY(stdin: NodeJS.ReadableStream): Promise<void> {
  console.log(formatVersionOutput())
  console.log()

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  const rl = readline.createInterface({
    input: stdin,
    output: process.stdout,
    escapeCodeTimeout: 50,
  })
  readline.emitKeypressEvents(stdin, rl)

  let chatHistory: ChatHistory = []
  let buffer = ''
  let highlightIndex = 0
  let suggestionsDismissed = false
  let linesAboveCursor = 0
  let prevTotalLines = 0
  let tokenListItems: { label: string; token: string }[] | null = null
  let tokenListCommand = ''
  let tokenHighlightIndex = 0
  let tokenListAction: 'set-default' | 'remove' | 'remove-completely' =
    'set-default'
  let mcqChoiceHighlightIndex = 0

  const collectedOutputLines: string[] = []
  const ttyOutput: OutputAdapter = {
    log: (msg) => {
      process.stdout.write(`${msg}\n`)
      collectedOutputLines.push(...msg.split('\n'))
    },
    logError: (err) => {
      const msg = err instanceof Error ? err.message : String(err)
      writeError(err)
      collectedOutputLines.push(msg)
    },
  }

  function getDisplayContent() {
    const width = getTerminalWidth()
    const contentLines = buildBoxLines(buffer, width)
    const boxLines = renderBox(contentLines, width).split('\n')
    const suggestionLines = tokenListItems
      ? formatHighlightedList(
          formatTokenLines(tokenListItems, getDefaultTokenLabel()),
          8,
          tokenHighlightIndex
        )
      : pendingRecallStopConfirmation
        ? ['Stop recall? (y/n)']
        : isMcqPrompt(pendingRecallAnswer)
          ? formatHighlightedList(
              formatMcqChoiceLines(pendingRecallAnswer.choices),
              8,
              mcqChoiceHighlightIndex
            )
          : (() => {
              if (
                suggestionsDismissed &&
                isCommandPrefixWithSuggestions(buffer)
              )
                return [COMMANDS_HINT]
              return buildSuggestionLines(buffer, highlightIndex)
            })()
    const recallingIndicator = isInRecallSubstate() ? [RECALLING_INDICATOR] : []
    return { contentLines, boxLines, suggestionLines, recallingIndicator }
  }

  function doFullRedraw() {
    const { contentLines, boxLines, suggestionLines, recallingIndicator } =
      getDisplayContent()
    const newTotalLines =
      boxLines.length + recallingIndicator.length + suggestionLines.length

    process.stdout.write(CLEAR_SCREEN)
    const fullLines = renderFullDisplay(
      chatHistory,
      buffer,
      getTerminalWidth(),
      suggestionLines,
      recallingIndicator
    )
    for (const line of fullLines) {
      process.stdout.write(`${line}\n`)
    }

    linesAboveCursor = contentLines.length
    prevTotalLines = newTotalLines

    const cursorRow = contentLines.length
    process.stdout.write(`\x1b[${newTotalLines - cursorRow}A`)

    const bufferLines = buffer.split('\n')
    const lastLine = bufferLines[bufferLines.length - 1]
    const lastPrefix = bufferLines.length === 1 ? PROMPT : '  '
    const col = 3 + lastPrefix.length + lastLine.length
    process.stdout.write(`\x1b[${col}G`)
  }

  function drawBox() {
    const { contentLines, boxLines, suggestionLines, recallingIndicator } =
      getDisplayContent()
    const newTotalLines =
      boxLines.length + recallingIndicator.length + suggestionLines.length

    if (linesAboveCursor > 0) {
      process.stdout.write(`\x1b[${linesAboveCursor}A`)
    }
    process.stdout.write('\r')

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
    const cursorRow = contentLines.length
    process.stdout.write(`\x1b[${totalWritten - cursorRow}A`)

    const bufferLines = buffer.split('\n')
    const lastLine = bufferLines[bufferLines.length - 1]
    const lastPrefix = bufferLines.length === 1 ? PROMPT : '  '
    const col = 3 + lastPrefix.length + lastLine.length
    process.stdout.write(`\x1b[${col}G`)

    linesAboveCursor = contentLines.length
    prevTotalLines = newTotalLines
  }

  drawBox()

  process.stdout.on('resize', drawBox)
  const removeResizeListener = () => process.stdout.off('resize', drawBox)
  const doExit = () => {
    removeResizeListener()
    rl.close()
    process.exit(0)
  }

  stdin.on(
    'keypress',
    async (
      str: string,
      key: { name: string; shift?: boolean; ctrl?: boolean; meta?: boolean }
    ) => {
      if (key.ctrl && key.name === 'c') {
        process.stdout.write(`\x1b[${1}B\r\n`)
        doExit()
      }
      if (pendingRecallStopConfirmation) {
        if (key.name === 'escape') {
          pendingRecallStopConfirmation = false
          buffer = ''
          drawBox()
        } else if (key.name === 'return' && !key.shift) {
          const trimmed = buffer.trim()
          const answer = parseYesNo(trimmed)
          buffer = ''
          pendingRecallStopConfirmation = false
          if (answer === true) {
            exitRecallMode()
            mcqChoiceHighlightIndex = 0
            linesAboveCursor = 0
            prevTotalLines = 0
            process.stdout.write('Stopped recall\n')
            chatHistory.push({ type: 'input', content: trimmed })
            chatHistory.push({ type: 'output', lines: ['Stopped recall'] })
          } else if (answer === false) {
            // Stay in MCQ; drawBox will show choices again
          } else if (trimmed) {
            writeStatus('Please answer y or n')
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
      if (isMcqPrompt(pendingRecallAnswer)) {
        const { choices } = pendingRecallAnswer
        if (key.name === 'escape') {
          pendingRecallStopConfirmation = true
          buffer = ''
          writeStatus('Stop recall? (y/n)')
          drawBox()
        } else if (key.name === 'up' || key.name === 'down') {
          const delta = key.name === 'up' ? -1 : 1
          mcqChoiceHighlightIndex = cycleIndex(
            mcqChoiceHighlightIndex,
            delta,
            choices.length
          )
          drawBox()
        } else if (key.name === 'return' && !key.shift) {
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
        } else if (key.name === 'escape') {
          tokenListItems = null
          tokenListCommand = ''
          tokenHighlightIndex = 0
          tokenListAction = 'set-default'
          drawBox()
        } else if (key.name === 'return' && !key.shift) {
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
          tokenListItems = null
          tokenHighlightIndex = 0
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
      if (key.name === 'return') {
        if (key.shift) {
          buffer += '\n'
          drawBox()
        } else {
          const lastLine = getLastLine(buffer)
          const trimmedInput = buffer.trim()

          if (trimmedInput === '/clear') {
            chatHistory = []
            buffer = ''
            tokenListItems = null
            tokenHighlightIndex = 0
            if (isInRecallSubstate()) exitRecallMode()
            pendingRecallStopConfirmation = false
            mcqChoiceHighlightIndex = 0
            highlightIndex = 0
            suggestionsDismissed = false
            doFullRedraw()
            return
          }

          const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
          const suggestionsVisible =
            lastLine.startsWith('/') &&
            !lastLine.endsWith(' ') &&
            filtered.length > 0

          if (suggestionsVisible) {
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

          const tokenSelectAction = TOKEN_LIST_COMMANDS[trimmedInput] ?? null
          if (tokenSelectAction) {
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
              tokenListAction = tokenSelectAction
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
          if (isMcqPrompt(pendingRecallAnswer)) {
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

async function runInteractivePiped(
  stdin: NodeJS.ReadableStream
): Promise<void> {
  const width = getTerminalWidth()
  console.log(formatVersionOutput())
  console.log()
  const hintLines = buildSuggestionLines('', 0)
  console.log(renderBox(buildBoxLines('', width), width))
  for (const line of hintLines) {
    console.log(line)
  }
  console.log()

  const rl = readline.createInterface({
    input: stdin,
    output: process.stdout,
    terminal: false,
  })

  let processing = false
  const lineQueue: string[] = []
  async function processNextLine() {
    if (processing || lineQueue.length === 0) return
    processing = true
    const line = lineQueue.shift()!
    if (line.trim()) {
      console.log(renderPastInput(line, getTerminalWidth()))
    }
    if (await processInput(line)) {
      rl.close()
      process.exit(0)
    }
    processing = false
    if (lineQueue.length > 0) processNextLine()
  }

  rl.on('line', (line) => {
    lineQueue.push(line)
    processNextLine()
  })
}

export async function runInteractive(
  stdin: NodeJS.ReadableStream = process.stdin
): Promise<void> {
  if (stdin.isTTY) {
    await runInteractiveTTY(stdin)
  } else {
    await runInteractivePiped(stdin)
  }
}
