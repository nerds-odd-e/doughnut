import { GREY, RESET } from './ansi.js'
import {
  filterCommandsByPrefix,
  formatCommandCompletionLines,
  interactiveDocs,
} from './help.js'
import { formatTokenLines, type AccessTokenEntry } from './accessToken.js'
import { formatHighlightedList } from './listDisplay.js'
import { renderMarkdownToTerminal } from './markdown.js'
import type { ChatHistory } from './types.js'
import { formatVersionOutput } from './version.js'

export { GREY, RESET }
export const GREEN = '\x1b[32m'
export const GREY_BG = '\x1b[48;5;236m'

export function buildCurrentPromptSeparator(width: number): string {
  return `${GREEN}${'─'.repeat(width)}${RESET}`
}
export const COMMAND_HIGHLIGHT = '\x1b[1;36m' // bold + cyan

export const PLACEHOLDER = '`exit` to quit.'
export const PROMPT = '→ '

export const CLEAR_SCREEN = '\x1b[H\x1b[2J'
/** Shown in Current guidance when user has not typed a slash command prefix. */
export const COMMANDS_HINT = `${GREY}  / commands${RESET}`
export const RECALLING_INDICATOR = `${GREY}Recalling${RESET}`

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g
// biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI escape or single char (for truncation)
const ANSI_OR_CHAR_PATTERN = /\x1b\[[0-9;]*m|./gs

/** Terminal column count; used for truncation and line width. */
export type TerminalWidth = number

export function visibleLength(str: string): number {
  return str.replace(ANSI_PATTERN, '').length
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
        return `${result}...`
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

/** Returns lines for the Current guidance area (command completion or / commands hint). */
export function buildSuggestionLines(
  buffer: string,
  highlightIndex: number,
  width: number,
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
  const lines = formatHighlightedList(
    formatCommandCompletionLines(filtered),
    8,
    highlightIndex
  )
  return lines.map((line) => truncateToWidth(line, width))
}

/** Returns lines for the Current guidance area (access token list). Truncates long labels to width. */
export function buildTokenListLines(
  tokens: AccessTokenEntry[],
  defaultLabel: string | undefined,
  width: number,
  highlightIndex: number
): string[] {
  const lines = formatHighlightedList(
    formatTokenLines(tokens, defaultLabel),
    8,
    highlightIndex
  )
  return lines.map((line) => truncateToWidth(line, width))
}

/** Renders the full display. suggestionLines and recallingIndicator are Current guidance (below input box). */
export function renderFullDisplay(
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

export function writeFullRedraw(
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
