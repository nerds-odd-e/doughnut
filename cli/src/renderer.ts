import { GREY, RESET, HIDE_CURSOR, SHOW_CURSOR } from './ansi.js'
import {
  filterCommandsByPrefix,
  formatCommandCompletionLines,
  interactiveDocs,
} from './help.js'
import { formatTokenLines, type AccessTokenEntry } from './accessToken.js'
import {
  CURRENT_GUIDANCE_MAX_VISIBLE,
  formatHighlightedList,
} from './listDisplay.js'
import { renderMarkdownToTerminal } from './markdown.js'
import type { ChatHistory } from './types.js'
import { formatVersionOutput } from './version.js'

export { GREY, RESET, HIDE_CURSOR, SHOW_CURSOR }

/** Terminal column count; used for truncation and line width. */
export type TerminalWidth = number

export const GREEN = '\x1b[32m'
export const GREY_BG = '\x1b[48;5;236m'

export function buildCurrentPromptSeparator(width: TerminalWidth): string {
  return `${GREEN}${'─'.repeat(width)}${RESET}`
}
export const COMMAND_HIGHLIGHT = '\x1b[1;36m' // bold + cyan

export const PROMPT = '→ '

/** Input box placeholder by interaction context. Single source of truth. */
export type PlaceholderContext =
  | 'default'
  | 'tokenList'
  | 'recallMcq'
  | 'recallStopConfirmation'
  | 'recallYesNo'
  | 'recallSpelling'

export const PLACEHOLDER_BY_CONTEXT: Record<PlaceholderContext, string> = {
  default: '`exit` to quit.',
  tokenList: '↑↓ Enter to select; other keys cancel', // selection mode
  recallMcq: '↑↓ Enter or number to select; Esc to cancel',
  recallStopConfirmation: 'y or n; Esc to go back',
  recallYesNo: 'y or n; /stop to exit recall',
  recallSpelling: 'type your answer; /stop to exit recall',
}

export const CLEAR_SCREEN = '\x1b[H\x1b[2J'

/** Shown in Current guidance when user has not typed a slash command prefix. */
export const COMMANDS_HINT = `${GREY}  / commands${RESET}`
export const RECALLING_INDICATOR = `${GREY}Recalling${RESET}`

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g
// biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI escape or single char (for truncation)
const ANSI_OR_CHAR_PATTERN = /\x1b\[[0-9;]*m|./gs

/** Strip ANSI escape sequences; returns plain text. */
export function stripAnsi(str: string): string {
  return str.replace(ANSI_PATTERN, '')
}

export function visibleLength(str: string): number {
  return stripAnsi(str).length
}

/** Wraps plain text to width; returns lines. Breaks at word boundaries when possible. */
export function wrapTextToLines(text: string, width: TerminalWidth): string[] {
  if (width <= 0) return text.length ? [text] : []
  if (text.length <= width) return text.length ? [text] : []
  const result: string[] = []
  let remaining = text
  while (remaining.length > 0) {
    if (remaining.length <= width) {
      result.push(remaining)
      break
    }
    const chunk = remaining.slice(0, width + 1)
    const lastSpace = chunk.lastIndexOf(' ')
    const breakAt = lastSpace > 0 && lastSpace <= width ? lastSpace : width
    const line = remaining.slice(0, breakAt).trimEnd()
    if (line.length > 0) result.push(line)
    remaining = remaining.slice(breakAt).trimStart()
  }
  return result
}

/** Truncate str to at most width visible chars; append "..." when truncating. ANSI-aware. */
export function truncateToWidth(str: string, width: TerminalWidth): string {
  if (visibleLength(str) <= width) return str
  const maxVisible = width - 3
  let visibleCount = 0
  let result = ''
  const re = new RegExp(ANSI_OR_CHAR_PATTERN.source, 'gs')
  for (let m = re.exec(str); m !== null; m = re.exec(str)) {
    const token = m[0]
    if (token.startsWith('\x1b')) {
      result += token
    } else {
      if (visibleCount + 1 > maxVisible) {
        return `${result}...${RESET}`
      }
      result += token
      visibleCount++
    }
  }
  return result
}

function padEndVisible(str: string, targetLen: number): string {
  const pad = targetLen - visibleLength(str)
  return pad > 0 ? str + ' '.repeat(pad) : str
}

export function renderBox(lines: string[], width: TerminalWidth): string {
  const innerWidth = width - 4
  const top = `┌${'─'.repeat(width - 2)}┐`
  const bottom = `└${'─'.repeat(width - 2)}┘`
  const rows = lines.map(
    (line) =>
      `│ ${padEndVisible(truncateToWidth(line, innerWidth), innerWidth)} │`
  )
  return [top, ...rows, bottom].join('\n')
}

export function renderPastInput(input: string, width: TerminalWidth): string {
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

export interface BuildBoxLinesOptions {
  placeholderContext?: PlaceholderContext
}

export function buildBoxLines(
  buffer: string,
  width: TerminalWidth,
  options?: BuildBoxLinesOptions
): string[] {
  const bufferLines = buffer.split('\n')
  const context = options?.placeholderContext ?? 'default'
  const placeholder = PLACEHOLDER_BY_CONTEXT[context]
  const isSelectionMode = context === 'tokenList'
  return bufferLines.map((line, i) => {
    const prefix = isSelectionMode ? '' : i === 0 ? PROMPT : '  '
    if (i === 0 && buffer === '') {
      return `${prefix}${GREY}${placeholder}${RESET}`
    }
    const highlighted = highlightRecognizedCommand(line)
    return prefix + highlighted
  })
}

export function getTerminalWidth(): number {
  return process.stdout.columns || 80
}

export function formatMcqChoiceLines(choices: string[]): string[] {
  return choices.map((c, i) => `  ${i + 1}. ${renderMarkdownToTerminal(c)}`)
}

/** Last line of buffer (for slash-command prefix detection). */
export function getLastLine(buffer: string): string {
  const lines = buffer.split('\n')
  return lines[lines.length - 1] ?? ''
}

/** Format plain option lines to Current guidance: highlight selected, truncate to terminal width. */
function formatCurrentGuidanceLines(
  plainLines: string[],
  highlightIndex: number,
  width: TerminalWidth
): string[] {
  return formatHighlightedList(
    plainLines,
    CURRENT_GUIDANCE_MAX_VISIBLE,
    highlightIndex
  ).map((line) => truncateToWidth(line, width))
}

/** Returns lines for Current guidance (command completion or / commands hint). */
export function buildSuggestionLines(
  buffer: string,
  highlightIndex: number,
  width: TerminalWidth,
  options?: { forceCommandsHint?: boolean }
): string[] {
  const lastLine = getLastLine(buffer)
  const showHint =
    options?.forceCommandsHint ||
    !lastLine.startsWith('/') ||
    lastLine.endsWith(' ')
  if (showHint) {
    return [truncateToWidth(COMMANDS_HINT, width)]
  }
  const filtered = filterCommandsByPrefix(interactiveDocs, lastLine)
  return formatCurrentGuidanceLines(
    formatCommandCompletionLines(filtered),
    highlightIndex,
    width
  )
}

/** Returns lines for Current guidance (access token list). */
export function buildTokenListLines(
  tokens: AccessTokenEntry[],
  defaultLabel: string | undefined,
  width: TerminalWidth,
  highlightIndex: number
): string[] {
  return formatCurrentGuidanceLines(
    formatTokenLines(tokens, defaultLabel),
    highlightIndex,
    width
  )
}

/** Renders the full display. currentPromptLines is Current prompt (above input box), pre-wrapped. suggestionLines and recallingIndicator are Current guidance (below input box). */
export function renderFullDisplay(
  history: ChatHistory,
  buffer: string,
  width: TerminalWidth,
  suggestionLines: string[],
  recallingIndicator: string[],
  currentPromptLines?: string[],
  options?: BuildBoxLinesOptions
): string[] {
  const lines: string[] = [formatVersionOutput(), '']
  for (const entry of history) {
    if (entry.type === 'input') {
      lines.push(...renderPastInput(entry.content, width).split('\n'))
    } else {
      lines.push(...entry.lines)
    }
  }
  if (currentPromptLines && currentPromptLines.length > 0) {
    lines.push(buildCurrentPromptSeparator(width))
    for (const line of currentPromptLines) {
      lines.push(`${GREY}${line}${RESET}`)
    }
  }
  const rawBoxLines = renderBox(
    buildBoxLines(buffer, width, options),
    width
  ).split('\n')
  const boxLines =
    options?.placeholderContext === 'tokenList' // selection mode
      ? rawBoxLines.map((l) => `${GREY}${l}${RESET}`)
      : rawBoxLines
  lines.push(...boxLines)
  lines.push(...recallingIndicator)
  lines.push(...suggestionLines)
  return lines
}

export function writeFullRedraw(
  history: ChatHistory,
  buffer: string,
  width: TerminalWidth,
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
