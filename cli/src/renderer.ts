/**
 * Terminal layout bridge: grapheme-aware width and wrapping, ANSI strings for the live region
 * (assembled into Ink via `CommandLineLivePanel` and related paths), piped-adapter box rendering, and
 * shared placeholders / tone helpers. Complements the Ink shell; not a second interactive UI engine.
 */
import {
  GREY,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  ITALIC,
  RED,
  RESET,
  REVERSE,
  HIDE_CURSOR,
  SHOW_CURSOR,
} from './ansi.js'
import {
  filterCommandsByPrefix,
  formatCommandCompletionLines,
  interactiveDocs,
} from './help.js'
import {
  formatTokenLines,
  type AccessTokenEntry,
  type AccessTokenLabel,
} from './accessToken.js'
import {
  CURRENT_GUIDANCE_MAX_VISIBLE,
  formatHighlightedList,
} from './listDisplay.js'
import { renderMarkdownToTerminal } from './markdown.js'
import type {
  ChatHistory,
  ChatHistoryOutputTone,
  RecallMcqChoiceTexts,
} from './types.js'
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

/**
 * Background SGR for the **Current stage band**: full-width Current Stage Indicator line and,
 * when that indicator is shown, the matching Current prompt separator strip (same color index as
 * {@link GREY_BG} until palette is tuned).
 */
export const CURRENT_STAGE_BAND_BACKGROUND_SGR = GREY_BG

export function buildCurrentPromptSeparator(width: TerminalWidth): string {
  return `${GREEN}${'─'.repeat(width)}${RESET}`
}

/** Green rule on the Current stage band (after the Current Stage Indicator line). */
export function buildCurrentPromptSeparatorForStageBand(
  width: TerminalWidth
): string {
  return `${CURRENT_STAGE_BAND_BACKGROUND_SGR}${GREEN}${'─'.repeat(width)}${RESET}`
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

/** One **Current Stage Indicator** line for interactive fetch-wait (blue label + ellipsis on the band). */
export function interactiveFetchWaitStageIndicatorLine(
  baseLine: InteractiveFetchWaitLine,
  ellipsisTick: number
): string {
  return `${INTERACTIVE_FETCH_WAIT_PROMPT_FG}${formatInteractiveFetchWaitPromptLine(baseLine, ellipsisTick)}${RESET}`
}

/** Full grey outline for the input box when the user is not free-typing a command. */
export function grayDisabledInputBoxLines(lines: string[]): string[] {
  return lines.map((l) => `${GREY}${l.split(RESET).join(GREY)}${RESET}`)
}

export const CLEAR_SCREEN = '\x1b[H\x1b[2J'

/** Shown in Current guidance when user has not typed a slash command prefix. */
export const COMMANDS_HINT = `${GREY}  / commands${RESET}`

/**
 * Grey foreground SGR for one **Current Stage Indicator** label; full-width band padding is applied in
 * {@link buildLiveRegionLines}.
 */
export function greyCurrentStageIndicatorLabel(plainText: string): string {
  return `${GREY}${plainText}${RESET}`
}

/** Default **Current Stage Indicator** line while recall payload is loading (label: “Recalling”). */
export const DEFAULT_RECALL_LOADING_STAGE_INDICATOR =
  greyCurrentStageIndicatorLabel('Recalling')

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g
// biome-ignore lint/suspicious/noControlCharactersInRegex: SGR at start of slice
const SGR_AT_START = /^\x1b\[[0-9;]*m/

const graphemeSegmenter = new Intl.Segmenter('en', { granularity: 'grapheme' })

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

function isRegionalIndicator(cp: number): boolean {
  return cp >= 0x1f1e6 && cp <= 0x1f1ff
}

/**
 * Column width of one Unicode code point (plain text, not a full grapheme).
 * Used only inside {@link terminalColumnsOfPlainGrapheme}.
 */
function codePointTerminalColumnWidth(cp: number): number {
  if (cp === 0x09) return 1
  if (cp < 0x20 || cp === 0x7f) return 0
  if (
    (cp >= 0x0300 && cp <= 0x036f) ||
    (cp >= 0x1ab0 && cp <= 0x1aff) ||
    (cp >= 0x1dc0 && cp <= 0x1dff) ||
    (cp >= 0x20d0 && cp <= 0x20ff) ||
    (cp >= 0xfe00 && cp <= 0xfe0e) ||
    (cp >= 0xfe20 && cp <= 0xfe2f) ||
    (cp >= 0x1f3fb && cp <= 0x1f3ff)
  ) {
    return 0
  }
  if (cp === 0xfe0f) return 0
  if (
    (cp >= 0x2e80 && cp <= 0x303e) ||
    (cp >= 0x3040 && cp <= 0x33ff) ||
    (cp >= 0x3400 && cp <= 0x4dbf) ||
    (cp >= 0x4e00 && cp <= 0xa4cf) ||
    (cp >= 0xa960 && cp <= 0xa97f) ||
    (cp >= 0xac00 && cp <= 0xd7ff) ||
    (cp >= 0xf900 && cp <= 0xfaff) ||
    (cp >= 0xfe10 && cp <= 0xfe6f) ||
    (cp >= 0xff00 && cp <= 0xff60) ||
    (cp >= 0xffe0 && cp <= 0xffe6) ||
    (cp >= 0x1b000 && cp <= 0x1b12f) ||
    (cp >= 0x20000 && cp <= 0x2fffd) ||
    (cp >= 0x30000 && cp <= 0x3fffd)
  ) {
    return 2
  }
  if (
    (cp >= 0x2600 && cp <= 0x26ff) ||
    (cp >= 0x2700 && cp <= 0x27bf) ||
    (cp >= 0x1f000 && cp <= 0x1faff)
  ) {
    return 2
  }
  return 1
}

/**
 * Terminal column width of one plain-text grapheme cluster (no ANSI).
 * Single source of truth for “how wide is this on a typical terminal?” — CJK, emoji,
 * flags (regional indicators), and ZWJ sequences.
 */
export function terminalColumnsOfPlainGrapheme(g: string): number {
  if (g.includes('\u200d')) return 2
  const cps: number[] = []
  for (const ch of g) cps.push(ch.codePointAt(0)!)
  if (cps.length === 2 && cps.every(isRegionalIndicator)) return 2
  if (g.includes('\ufe0f')) return 2
  let sum = 0
  for (const cp of cps) sum += codePointTerminalColumnWidth(cp)
  return sum
}

/** Terminal column count after stripping ANSI; uses grapheme clusters + {@link terminalColumnsOfPlainGrapheme}. */
export function visibleLength(str: string): number {
  let total = 0
  for (const { segment } of graphemeSegmenter.segment(stripAnsi(str))) {
    total += terminalColumnsOfPlainGrapheme(segment)
  }
  return total
}

function* terminalVisualTokens(str: string): Generator<string> {
  let pos = 0
  while (pos < str.length) {
    const slice = str.slice(pos)
    const ansi = slice.match(SGR_AT_START)
    if (ansi) {
      yield ansi[0]
      pos += ansi[0].length
      continue
    }
    const rest = str.slice(pos)
    if (rest.length === 0) break
    for (const { segment } of graphemeSegmenter.segment(rest)) {
      yield segment
      pos += segment.length
      break
    }
  }
}

function terminalTokenVisibleWidth(token: string): number {
  if (token.startsWith('\x1b')) return 0
  return terminalColumnsOfPlainGrapheme(token)
}

/** Wraps plain text to terminal columns (same rules as {@link wrapTextToVisibleWidthLines} without ANSI). */
export function wrapTextToLines(text: string, width: TerminalWidth): string[] {
  return wrapTextToVisibleWidthLines(text, width)
}

/** Wrap one paragraph to `width` visible columns; preserves ANSI sequences. */
export function wrapTextToVisibleWidthLines(
  text: string,
  width: TerminalWidth
): string[] {
  if (width <= 0) return text.length ? [text] : []
  if (!text) return []
  if (visibleLength(text) <= width) return [text]
  const tokens = [...terminalVisualTokens(text)]
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
      const tw = terminalTokenVisibleWidth(t)
      if (vis + tw > width) break
      if (t === ' ' || t === '\t') {
        breakAfter = j
        breakLine = line + t
      }
      line += t
      vis += tw
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

/** Truncate str to at most width visible chars; append "..." when truncating. ANSI-aware. */
export function truncateToWidth(str: string, width: TerminalWidth): string {
  if (visibleLength(str) <= width) return str
  const maxVisible = width - 3
  let visibleCount = 0
  let result = ''
  for (const token of terminalVisualTokens(str)) {
    if (token.startsWith('\x1b')) {
      result += token
    } else {
      const tw = terminalTokenVisibleWidth(token)
      if (visibleCount + tw > maxVisible) {
        return `${result}...${RESET}`
      }
      result += token
      visibleCount += tw
    }
  }
  return result
}

function padEndVisible(str: string, targetLen: number): string {
  const pad = targetLen - visibleLength(str)
  return pad > 0 ? str + ' '.repeat(pad) : str
}

function stripTrailingSgrReset(s: string): string {
  // biome-ignore lint/suspicious/noControlCharactersInRegex: strip trailing SGR reset
  return s.replace(/\x1b\[0m$/, '')
}

/** One full-width screen line: Current stage band + padded label (visible width = `width`). */
export function formatCurrentStageIndicatorLine(
  labelWithLeadingSgr: string,
  width: TerminalWidth
): string {
  const opened = stripTrailingSgrReset(labelWithLeadingSgr)
  return `${CURRENT_STAGE_BAND_BACKGROUND_SGR}${padEndVisible(opened, width)}${RESET}`
}

/**
 * Line count from the top of the live region through the last Current prompt line (separator and
 * wrapped prompt rows), immediately above the input box top border. Matches
 * {@link buildLiveRegionLines} layout rules.
 */
export function countPromptBlockLinesAboveInputBoxTop(
  currentStageIndicatorLines: string[],
  currentPromptWrappedLines: string[]
): number {
  let n = 0
  if (currentStageIndicatorLines.length > 0) {
    n += currentStageIndicatorLines.length + 1
  }
  if (currentPromptWrappedLines.length > 0) {
    if (currentStageIndicatorLines.length === 0) n += 1
    n += currentPromptWrappedLines.length
  }
  return n
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

/** UTF-16 length of longest `interactiveDocs` usage prefix matching `line` when it starts with `/`; else 0. */
function slashCommandHighlightUtf16Length(line: string): number {
  if (!line.startsWith('/')) return 0
  const usages = interactiveDocs.map((d) => d.usage)
  let highlightLen = 0
  for (const cmd of usages) {
    if (line.startsWith(cmd)) {
      highlightLen = Math.max(highlightLen, cmd.length)
    }
  }
  return highlightLen
}

export function highlightRecognizedCommand(line: string): string {
  const highlightLen = slashCommandHighlightUtf16Length(line)
  if (highlightLen === 0) return line
  const prefix = line.slice(0, highlightLen)
  const rest = line.slice(highlightLen)
  return `${COMMAND_HIGHLIGHT}${prefix}${RESET}${rest}`
}

export interface BuildBoxLinesOptions {
  placeholderContext?: PlaceholderContext
}

export type LiveRegionPaintOptions = BuildBoxLinesOptions & {
  /** When true, history (+ version header) only — no Current prompt / box / guidance tail. */
  omitLiveRegion?: boolean
}

function inputBoxLinePrefix(
  lineIndex: number,
  context: PlaceholderContext
): string {
  return isGreyDisabledInputChrome(context)
    ? ''
    : lineIndex === 0
      ? PROMPT
      : '  '
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
    const prefix = inputBoxLinePrefix(i, context)
    if (i === 0 && buffer === '') {
      return `${prefix}${GREY}${placeholder}${RESET}`
    }
    const highlighted = highlightRecognizedCommand(line)
    return prefix + highlighted
  })
}

function caretLineAndOffsetInBuffer(
  buffer: string,
  caretOffset: number
): { lineIndex: number; offsetInLine: number } {
  const co = Math.max(0, Math.min(caretOffset, buffer.length))
  let lineIndex = 0
  let lineStart = 0
  for (let i = lineStart; i < co; i++) {
    if (buffer[i] === '\n') {
      lineIndex++
      lineStart = i + 1
    }
  }
  return { lineIndex, offsetInLine: co - lineStart }
}

function firstGraphemeFromOffset(line: string, utf16Offset: number): string {
  const slice = line.slice(utf16Offset)
  for (const { segment } of graphemeSegmenter.segment(slice)) {
    return segment
  }
  return ' '
}

function plainInsertCaret(line: string, offsetInLine: number): string {
  const o = Math.max(0, Math.min(offsetInLine, line.length))
  const before = line.slice(0, o)
  if (o >= line.length) {
    return `${before}${REVERSE} ${RESET}`
  }
  const g = firstGraphemeFromOffset(line, o)
  const after = line.slice(o + g.length)
  return `${before}${REVERSE}${g}${RESET}${after}`
}

function buildLineWithCaret(line: string, offsetInLine: number): string {
  if (!line.startsWith('/')) {
    return plainInsertCaret(line, offsetInLine)
  }
  const highlightLen = slashCommandHighlightUtf16Length(line)
  if (highlightLen === 0) {
    return plainInsertCaret(line, offsetInLine)
  }
  if (offsetInLine < highlightLen) {
    const before = line.slice(0, offsetInLine)
    const g = firstGraphemeFromOffset(line, offsetInLine)
    const afterInCmd = line.slice(offsetInLine + g.length, highlightLen)
    const tail = line.slice(highlightLen)
    return `${COMMAND_HIGHLIGHT}${before}${REVERSE}${g}${RESET}${COMMAND_HIGHLIGHT}${afterInCmd}${RESET}${tail}`
  }
  const highlightedCmd = `${COMMAND_HIGHLIGHT}${line.slice(0, highlightLen)}${RESET}`
  const rest = line.slice(highlightLen)
  return highlightedCmd + plainInsertCaret(rest, offsetInLine - highlightLen)
}

/**
 * Like {@link buildBoxLines}, but inserts a reverse-video caret at `caretOffset` in the buffer
 * (UTF-16 index, newline-aware), grapheme-aware for the inverted cell.
 */
export function buildBoxLinesWithCaret(
  buffer: string,
  _width: TerminalWidth,
  caretOffset: number,
  options?: BuildBoxLinesOptions
): string[] {
  const bufferLines = buffer.split('\n')
  const { lineIndex: caretLineIndex, offsetInLine } =
    caretLineAndOffsetInBuffer(buffer, caretOffset)
  const context = options?.placeholderContext ?? 'default'
  const placeholder = PLACEHOLDER_BY_CONTEXT[context]
  return bufferLines.map((line, i) => {
    const prefix = inputBoxLinePrefix(i, context)
    let inner: string
    if (i === 0 && buffer === '') {
      inner =
        caretLineIndex === 0 && offsetInLine === 0
          ? `${REVERSE} ${RESET}${GREY}${placeholder}${RESET}`
          : `${GREY}${placeholder}${RESET}`
    } else if (i === caretLineIndex) {
      inner = buildLineWithCaret(line, offsetInLine)
    } else {
      inner = highlightRecognizedCommand(line)
    }
    return prefix + inner
  })
}

export function getTerminalWidth(): number {
  return process.stdout.columns || 80
}

/** Last line of buffer (for slash-command prefix detection). */
export function getLastLine(buffer: string): string {
  const lines = buffer.split('\n')
  return lines[lines.length - 1] ?? ''
}

/** Blank line before the live region when scrollback exists but there is no Current prompt block (no wrapped lines and no stage indicator). */
export function needsGapBeforeBox(
  history: ChatHistory,
  currentPromptWrappedLines: string[],
  currentStageIndicatorLines: string[]
): boolean {
  return (
    history.length > 0 &&
    currentPromptWrappedLines.length === 0 &&
    currentStageIndicatorLines.length === 0
  )
}

function buildLiveRegionLinesAboveInputBox(
  width: TerminalWidth,
  currentPromptWrappedLines: string[],
  currentStageIndicatorLines: string[]
): string[] {
  const lines: string[] = []
  const hasStageIndicator = currentStageIndicatorLines.length > 0
  if (hasStageIndicator) {
    for (const ind of currentStageIndicatorLines) {
      lines.push(formatCurrentStageIndicatorLine(ind, width))
    }
    lines.push(buildCurrentPromptSeparatorForStageBand(width))
  }
  if (currentPromptWrappedLines.length > 0) {
    if (!hasStageIndicator) {
      lines.push(buildCurrentPromptSeparator(width))
    }
    for (const line of currentPromptWrappedLines) {
      lines.push(`${GREY}${line}${RESET}`)
    }
  }
  return lines
}

function maybeGreyDisabledBoxLines(
  rawBoxLines: string[],
  options?: LiveRegionPaintOptions
): string[] {
  return options?.placeholderContext &&
    isGreyDisabledInputChrome(options.placeholderContext)
    ? grayDisabledInputBoxLines(rawBoxLines)
    : rawBoxLines
}

export function buildLiveRegionLines(
  buffer: string,
  width: TerminalWidth,
  currentPromptWrappedLines: string[],
  suggestionLines: string[],
  currentStageIndicatorLines: string[],
  options?: LiveRegionPaintOptions
): string[] {
  const lines = buildLiveRegionLinesAboveInputBox(
    width,
    currentPromptWrappedLines,
    currentStageIndicatorLines
  )
  const rawBoxLines = renderBox(
    buildBoxLines(buffer, width, options),
    width
  ).split('\n')
  lines.push(...maybeGreyDisabledBoxLines(rawBoxLines, options))
  lines.push(...suggestionLines)
  return lines
}

/** Same layout as {@link buildLiveRegionLines}, with a reverse-video caret in the input box. */
export function buildLiveRegionLinesWithCaret(
  buffer: string,
  width: TerminalWidth,
  caretOffset: number,
  currentPromptWrappedLines: string[],
  suggestionLines: string[],
  currentStageIndicatorLines: string[],
  options?: LiveRegionPaintOptions
): string[] {
  const lines = buildLiveRegionLinesAboveInputBox(
    width,
    currentPromptWrappedLines,
    currentStageIndicatorLines
  )
  const rawBoxLines = renderBox(
    buildBoxLinesWithCaret(buffer, width, caretOffset, options),
    width
  ).split('\n')
  lines.push(...maybeGreyDisabledBoxLines(rawBoxLines, options))
  lines.push(...suggestionLines)
  return lines
}

/** Newline-aware terminal wrap for markdown-rendered text (may contain ANSI). */
export function wrapMarkdownTerminalToLines(
  text: string,
  width: TerminalWidth
): string[] {
  return text
    .split('\n')
    .flatMap((p) =>
      p.length === 0 ? [''] : wrapTextToVisibleWidthLines(p, width)
    )
}

export function formatMcqChoiceLinesWithIndices(
  choices: RecallMcqChoiceTexts,
  width: TerminalWidth
): { lines: string[]; itemIndexPerLine: number[] } {
  const lines: string[] = []
  const itemIndexPerLine: number[] = []
  for (let i = 0; i < choices.length; i++) {
    const rendered = renderMarkdownToTerminal(choices[i]!)
      .replace(/\n+/g, ' ')
      .trim()
    const prefix = `  ${i + 1}. `
    const indent = ' '.repeat(visibleLength(prefix))
    const innerWidth = Math.max(1, width - visibleLength(prefix))
    const bodyLines = wrapTextToVisibleWidthLines(rendered, innerWidth)
    if (bodyLines.length === 0) {
      lines.push(prefix)
      itemIndexPerLine.push(i)
      continue
    }
    for (let r = 0; r < bodyLines.length; r++) {
      lines.push((r === 0 ? prefix : indent) + bodyLines[r]!)
      itemIndexPerLine.push(i)
    }
  }
  return { lines, itemIndexPerLine }
}

export function formatMcqChoiceLines(
  choices: RecallMcqChoiceTexts,
  width: TerminalWidth
): string[] {
  return formatMcqChoiceLinesWithIndices(choices, width).lines
}

/**
 * Plain selectable rows (commands, tokens, …) → Current guidance:
 * highlight one row, truncate to width, scroll window when needed.
 */
export function renderCurrentGuidanceForSelectableLines(
  plainLines: readonly string[],
  selectedIndex: number,
  width: TerminalWidth,
  itemIndexPerLine?: readonly number[]
): string[] {
  return formatHighlightedList(
    plainLines,
    CURRENT_GUIDANCE_MAX_VISIBLE,
    selectedIndex,
    itemIndexPerLine
  ).map((line) => truncateToWidth(line, width))
}

export function recallMcqCurrentGuidanceLines(
  choices: RecallMcqChoiceTexts,
  selectedChoiceIndex: number,
  width: TerminalWidth
): string[] {
  const { lines, itemIndexPerLine } = formatMcqChoiceLinesWithIndices(
    choices,
    width
  )
  return renderCurrentGuidanceForSelectableLines(
    lines,
    selectedChoiceIndex,
    width,
    itemIndexPerLine
  )
}

type SuggestionMode =
  | { kind: 'commandsHint' }
  | {
      kind: 'completion'
      filtered: ReturnType<typeof filterCommandsByPrefix>
    }

function suggestionModeFromBuffer(
  buffer: string,
  options?: { forceCommandsHint?: boolean }
): SuggestionMode {
  const lastLine = getLastLine(buffer)
  const showHint =
    options?.forceCommandsHint ||
    !lastLine.startsWith('/') ||
    lastLine.endsWith(' ')
  if (showHint) return { kind: 'commandsHint' }
  return {
    kind: 'completion',
    filtered: filterCommandsByPrefix(interactiveDocs, lastLine),
  }
}

/** Returns lines for Current guidance (command completion or / commands hint). */
export function buildSuggestionLines(
  buffer: string,
  highlightIndex: number,
  width: TerminalWidth,
  options?: { forceCommandsHint?: boolean }
): string[] {
  const mode = suggestionModeFromBuffer(buffer, options)
  if (mode.kind === 'commandsHint') {
    return [truncateToWidth(COMMANDS_HINT, width)]
  }
  return renderCurrentGuidanceForSelectableLines(
    formatCommandCompletionLines(mode.filtered),
    highlightIndex,
    width
  )
}

/**
 * Current guidance for the default TTY live column: same rows as {@link buildSuggestionLines} but
 * without per-line truncation — Ink `Text` `wrap` owns line breaks (gate 4).
 */
export function buildSuggestionLinesForInk(
  buffer: string,
  highlightIndex: number,
  options?: { forceCommandsHint?: boolean }
): string[] {
  const mode = suggestionModeFromBuffer(buffer, options)
  if (mode.kind === 'commandsHint') {
    return [COMMANDS_HINT]
  }
  return formatHighlightedList(
    formatCommandCompletionLines(mode.filtered),
    CURRENT_GUIDANCE_MAX_VISIBLE,
    highlightIndex
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
  defaultLabel: AccessTokenLabel | undefined,
  width: TerminalWidth,
  highlightIndex: number
): string[] {
  return renderCurrentGuidanceForSelectableLines(
    formatTokenLines(tokens, defaultLabel),
    highlightIndex,
    width
  )
}

/**
 * Renders the full display. `currentPromptLines` is wrapped Current prompt text (stem, etc.).
 * `currentStageIndicatorLines` drives the **Current Stage Indicator** line(s) and banded separator when
 * non-empty (first lines of the Current prompt block). `suggestionLines` are Current guidance
 * (below the input box).
 */
export function renderFullDisplay(
  history: ChatHistory,
  buffer: string,
  width: TerminalWidth,
  suggestionLines: string[],
  currentStageIndicatorLines: string[],
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
    needsGapBeforeBox(
      history,
      currentPromptLines ?? [],
      currentStageIndicatorLines
    ) &&
    lines[lines.length - 1] !== ''
  ) {
    lines.push('')
  }
  if (!options?.omitLiveRegion) {
    lines.push(
      ...buildLiveRegionLines(
        buffer,
        width,
        currentPromptLines ?? [],
        suggestionLines,
        currentStageIndicatorLines,
        options
      )
    )
  }
  return lines
}

export function writeFullRedraw(
  history: ChatHistory,
  buffer: string,
  width: TerminalWidth,
  suggestionLines: string[],
  currentStageIndicatorLines: string[],
  liveRegionOptions?: LiveRegionPaintOptions
): void {
  process.stdout.write(CLEAR_SCREEN)
  const lines = renderFullDisplay(
    history,
    buffer,
    width,
    suggestionLines,
    currentStageIndicatorLines,
    undefined,
    liveRegionOptions
  )
  for (const line of lines) {
    process.stdout.write(`${line}\n`)
  }
}
