import {
  GREY,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  ITALIC,
  RED,
  RESET,
  HIDE_CURSOR,
  SHOW_CURSOR,
} from './ansi.js'
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
import type { ChatHistory, ChatHistoryOutputTone } from './types.js'
import type { InteractiveFetchWaitLine } from './interactiveFetchWait.js'
import { formatVersionOutput } from './version.js'

export {
  GREY,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  RESET,
  HIDE_CURSOR,
  SHOW_CURSOR,
}

/**
 * Private OSC (not FinalTerm 133) so terminals with shell integration do not treat it
 * as a shell prompt boundary. Invisible on screen. Appended when the input box is ready.
 */
export const INTERACTIVE_INPUT_READY_OSC =
  '\x1b]900;doughnut-interactive-input-ready\x07' as const

/** Byte sequence written to stdout when the interactive CLI announces input readiness. */
export type InteractiveInputReadyOsc = typeof INTERACTIVE_INPUT_READY_OSC

/** Terminal column count; used for truncation and line width. */
export type TerminalWidth = number

export const GREEN = '\x1b[32m'
export const GREY_BG = '\x1b[48;5;236m'

export function buildCurrentPromptSeparator(width: TerminalWidth): string {
  return `${GREEN}${'─'.repeat(width)}${RESET}`
}
export const COMMAND_HIGHLIGHT = '\x1b[1;36m' // bold + cyan

export const PROMPT = '→ '

/**
 * Which placeholder and input chrome apply in the live region.
 * `interactiveFetchWait`: slow backend/network call in flight — same grey, no-→ box as token list pickers.
 * `recallYesNo`: next Enter submits a recall-session y/n answer (load more days or just-review); that line is
 * not stored as a grey history-input row or in on-disk command history (outcome lines only).
 */
export type PlaceholderContext =
  | 'default'
  | 'tokenList'
  | 'interactiveFetchWait'
  | 'recallMcq'
  | 'recallStopConfirmation'
  | 'recallYesNo'
  | 'recallSpelling'

export const PLACEHOLDER_BY_CONTEXT: Record<PlaceholderContext, string> = {
  default: '`exit` to quit.',
  tokenList: '↑↓ Enter to select; other keys cancel',
  interactiveFetchWait: 'loading ...',
  recallMcq: '↑↓ Enter or number to select; Esc to cancel',
  recallStopConfirmation: 'y or n; Esc to go back',
  recallYesNo: 'y or n; /stop to exit recall',
  recallSpelling: 'type your answer; /stop to exit recall',
}

/** Load-more or just-review y/n — the `PlaceholderContext` for the next recall-session y/n Enter. */
export const RECALL_SESSION_YES_NO_PLACEHOLDER =
  'recallYesNo' as const satisfies PlaceholderContext

/** Same-frame paint facts the TTY already has when finishing the live region (cursor + readiness signal). */
export type InteractiveInputReadyPaint = {
  /** Input box draft; empty means the user has not started typing. */
  lineDraft: string
  /** When set, a slow command is in flight and the live region is still animating — not ready for input. */
  interactiveFetchWaitLine: InteractiveFetchWaitLine | null
}

/**
 * Suffix to append after cursor placement when the interactive input box accepts input;
 * empty during interactive fetch wait or while the user has typed a draft.
 */
export function interactiveInputReadyOscSuffix(
  paint: InteractiveInputReadyPaint
): InteractiveInputReadyOsc | '' {
  if (paint.lineDraft !== '' || paint.interactiveFetchWaitLine !== null) {
    return ''
  }
  return INTERACTIVE_INPUT_READY_OSC
}

/** Token list pick or interactive fetch wait: grey bordered box, no →, cursor hidden. */
export function isGreyDisabledInputChrome(ctx: PlaceholderContext): boolean {
  return ctx === 'tokenList' || ctx === 'interactiveFetchWait'
}

const ELLIPSIS_PHASE = ['.', '..', '...'] as const

/** Appends `.` / `..` / `...` to the wait prompt base (TTY cycles `ellipsisTick`). */
export function formatInteractiveFetchWaitPromptLine(
  baseLine: InteractiveFetchWaitLine,
  ellipsisTick: number
): string {
  return `${baseLine}${ELLIPSIS_PHASE[ellipsisTick % 3]!}`
}

/** Full grey outline for the input box when the user is not free-typing a command. */
export function grayDisabledInputBoxLines(lines: string[]): string[] {
  return lines.map((l) => `${GREY}${l.split(RESET).join(GREY)}${RESET}`)
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

/** Strip SGR, other CSI sequences, and CR — for inspecting raw TTY captures in tests. */
export function stripAnsiCsiAndCr(str: string): string {
  const esc = '\x1b'
  return str
    .replace(new RegExp(`${esc}\\[[0-9;]*m`, 'g'), '')
    .replace(new RegExp(`${esc}\\[[0-9;]*[A-Za-z]`, 'g'), '')
    .replace(/\r/g, '')
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

/** Wrap one paragraph to `width` visible columns; preserves ANSI sequences. */
export function wrapTextToVisibleWidthLines(
  text: string,
  width: TerminalWidth
): string[] {
  if (width <= 0) return text.length ? [text] : []
  if (!text) return []
  if (visibleLength(text) <= width) return [text]
  const tokens: string[] = []
  const re = new RegExp(ANSI_OR_CHAR_PATTERN.source, 'gs')
  for (let m = re.exec(text); m !== null; m = re.exec(text)) {
    tokens.push(m[0])
  }
  const lines: string[] = []
  let i = 0
  while (i < tokens.length) {
    let line = ''
    let vis = 0
    let breakAfter = -1
    let breakLine = ''
    let j = i
    while (j < tokens.length) {
      const t = tokens[j]!
      if (t.startsWith('\x1b')) {
        line += t
        j++
        continue
      }
      if (vis + 1 > width) break
      if (t === ' ' || t === '\t') {
        breakAfter = j
        breakLine = line + t
      }
      line += t
      vis += 1
      j++
    }
    if (j === i) {
      const t = tokens[i]!
      if (t.startsWith('\x1b')) {
        line = t
        i++
        while (i < tokens.length && tokens[i]!.startsWith('\x1b')) {
          line += tokens[i]!
          i++
        }
        lines.push(line)
        continue
      }
      lines.push(t)
      i++
      continue
    }
    if (j < tokens.length && breakAfter >= i) {
      lines.push(breakLine.trimEnd())
      i = breakAfter + 1
    } else {
      lines.push(line.trimEnd())
      i = j
    }
    while (i < tokens.length && tokens[i] === ' ') i++
  }
  return lines
}

/** Newline-aware terminal wrap for markdown-rendered recall stems (may contain ANSI). */
export function wrapMarkdownTerminalToLines(
  text: string,
  width: TerminalWidth
): string[] {
  const parts = text.split('\n')
  const out: string[] = []
  for (const p of parts) {
    if (p.length === 0) out.push('')
    else out.push(...wrapTextToVisibleWidthLines(p, width))
  }
  return out
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

/** Submitted buffer counts as history input (and past-input paint) only when non-blank after trim. */
export function isCommittedInteractiveInput(submitted: string): boolean {
  return submitted.trim().length > 0
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

export type LiveRegionPaintOptions = BuildBoxLinesOptions & {
  /** Default grey. Interactive fetch wait uses `INTERACTIVE_FETCH_WAIT_PROMPT_FG`. */
  currentPromptSgr?: string
}

export function buildBoxLines(
  buffer: string,
  width: TerminalWidth,
  options?: BuildBoxLinesOptions
): string[] {
  const bufferLines = buffer.split('\n')
  const context = options?.placeholderContext ?? 'default'
  const placeholder = PLACEHOLDER_BY_CONTEXT[context]
  return bufferLines.map((line, i) => {
    const prefix = isGreyDisabledInputChrome(context)
      ? ''
      : i === 0
        ? PROMPT
        : '  '
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

export function formatMcqChoiceLines(choices: readonly string[]): string[] {
  return choices.map((c, i) => `  ${i + 1}. ${renderMarkdownToTerminal(c)}`)
}

/** Last line of buffer (for slash-command prefix detection). */
export function getLastLine(buffer: string): string {
  const lines = buffer.split('\n')
  return lines[lines.length - 1] ?? ''
}

export function needsGapBeforeBox(
  history: ChatHistory,
  currentPromptWrappedLines: string[]
): boolean {
  return history.length > 0 && currentPromptWrappedLines.length === 0
}

export function buildLiveRegionLines(
  buffer: string,
  width: TerminalWidth,
  currentPromptWrappedLines: string[],
  suggestionLines: string[],
  recallingIndicator: string[],
  options?: LiveRegionPaintOptions
): string[] {
  const lines: string[] = []
  const promptSgr = options?.currentPromptSgr ?? GREY
  if (currentPromptWrappedLines.length > 0) {
    lines.push(buildCurrentPromptSeparator(width))
    for (const line of currentPromptWrappedLines) {
      lines.push(`${promptSgr}${line}${RESET}`)
    }
  }
  const rawBoxLines = renderBox(
    buildBoxLines(buffer, width, options),
    width
  ).split('\n')
  const boxLines =
    options?.placeholderContext &&
    isGreyDisabledInputChrome(options.placeholderContext)
      ? grayDisabledInputBoxLines(rawBoxLines)
      : rawBoxLines
  lines.push(...boxLines)
  lines.push(...recallingIndicator)
  lines.push(...suggestionLines)
  return lines
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

/** SGR wrapper for one line of scrollback from {@link ChatHistoryOutputEntry}. */
export function applyChatHistoryOutputTone(
  line: string,
  tone: ChatHistoryOutputTone
): string {
  if (tone === 'error') return `${RED}${line}${RESET}`
  if (tone === 'userNotice') return `${GREY}${ITALIC}${line}${RESET}`
  return line
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
  options?: LiveRegionPaintOptions
): string[] {
  const lines: string[] = [formatVersionOutput(), '']
  for (const entry of history) {
    if (entry.type === 'input') {
      lines.push(...renderPastInput(entry.content, width).split('\n'))
    } else {
      const tone = entry.tone ?? 'plain'
      lines.push(
        ...entry.lines.map((line) => applyChatHistoryOutputTone(line, tone))
      )
    }
  }
  if (
    needsGapBeforeBox(history, currentPromptLines ?? []) &&
    lines[lines.length - 1] !== ''
  ) {
    lines.push('')
  }
  lines.push(
    ...buildLiveRegionLines(
      buffer,
      width,
      currentPromptLines ?? [],
      suggestionLines,
      recallingIndicator,
      options
    )
  )
  return lines
}

export function writeFullRedraw(
  history: ChatHistory,
  buffer: string,
  width: TerminalWidth,
  suggestionLines: string[],
  recallingIndicator: string[],
  liveRegionOptions?: LiveRegionPaintOptions
): void {
  process.stdout.write(CLEAR_SCREEN)
  const lines = renderFullDisplay(
    history,
    buffer,
    width,
    suggestionLines,
    recallingIndicator,
    undefined,
    liveRegionOptions
  )
  for (const line of lines) {
    process.stdout.write(`${line}\n`)
  }
}
