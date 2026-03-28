/**
 * TTY layout bridge: **command-line paint** strings for Ink (`liveColumnInk.tsx`), stage-band /
 * separator helpers, token guidance strings, and tone helpers. Grapheme width / wrap / truncate
 * live in `terminalLayout.ts`. Not a second interactive UI engine.
 */
import { RESET, HIDE_CURSOR, SHOW_CURSOR } from './ansi.js'
import {
  GREY,
  GREY_BG,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  terminalChalk,
} from './terminalChalk.js'
import { interactiveDocs } from './commands/help.js'
import {
  formatTokenLines,
  type AccessTokenEntry,
  type AccessTokenLabel,
} from './commands/accessToken.js'
import {
  CURRENT_GUIDANCE_MAX_VISIBLE,
  formatHighlightedList,
} from './listDisplay.js'
import type { CliAssistantMessageTone, PastMessages } from './types.js'
import type { InteractiveFetchWaitLine } from './interactiveFetchWait.js'
import {
  padEndVisible,
  stripTrailingSgrReset,
  truncateToWidth,
} from './terminalLayout.js'

export {
  GREY,
  GREY_BG,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  RESET,
  HIDE_CURSOR,
  SHOW_CURSOR,
}

export {
  stripAnsi,
  stripAnsiCsiAndCr,
  visibleLength,
  terminalColumnsOfPlainGrapheme,
  wrapTextToVisibleWidthLines,
  truncateToWidth,
} from './terminalLayout.js'

/** Terminal column count; used for truncation and line width. */
export type TerminalWidth = number

/**
 * Background SGR for the **Current stage band**: full-width Current Stage Indicator line and,
 * when that indicator is shown, the matching Current prompt separator strip (same color index as
 * {@link GREY_BG} until palette is tuned).
 */
export const CURRENT_STAGE_BAND_BACKGROUND_SGR = GREY_BG

export function buildCurrentPromptSeparator(width: TerminalWidth): string {
  return terminalChalk.green('─'.repeat(width))
}

/** Green rule on the Current stage band (after the Current Stage Indicator line). */
export function buildCurrentPromptSeparatorForStageBand(
  width: TerminalWidth
): string {
  return terminalChalk.bgAnsi256(236).green('─'.repeat(width))
}

export const PROMPT = '→ '

/**
 * Which placeholder and command-line chrome apply while the user is in that mode.
 * `interactiveFetchWait` and `tokenList`: typing is disabled — grey paint, no `→` prompt.
 */
export type PlaceholderContext =
  | 'default'
  | 'tokenList'
  | 'interactiveFetchWait'

export const PLACEHOLDER_BY_CONTEXT: Record<PlaceholderContext, string> = {
  default: '`exit` to quit.',
  tokenList: '↑↓ Enter to select; other keys cancel',
  interactiveFetchWait: 'loading ...',
}

function usesGreyNoArrowCommandLinePaint(ctx: PlaceholderContext): boolean {
  return ctx === 'tokenList' || ctx === 'interactiveFetchWait'
}

/** One **Current Stage Indicator** line for interactive fetch-wait layout (blue label; TTY animates via Ink `Spinner`). */
export function interactiveFetchWaitStageIndicatorLine(
  baseLine: InteractiveFetchWaitLine
): string {
  return terminalChalk.blueBright(baseLine)
}

/** Shown in Current guidance when user has not typed a slash command prefix. */
export const COMMANDS_HINT = terminalChalk.gray('  / commands')

/**
 * Grey foreground SGR for one **Current Stage Indicator** label; full-width band padding is applied in
 * {@link formatCurrentStageIndicatorLine}.
 */
export function greyCurrentStageIndicatorLabel(plainText: string): string {
  return terminalChalk.gray(plainText)
}

const graphemeSegmenter = new Intl.Segmenter('en', { granularity: 'grapheme' })

/** One full-width screen line: Current stage band + padded label (visible width = `width`). */
export function formatCurrentStageIndicatorLine(
  labelWithLeadingSgr: string,
  width: TerminalWidth
): string {
  const opened = stripTrailingSgrReset(labelWithLeadingSgr)
  return `${CURRENT_STAGE_BAND_BACKGROUND_SGR}${padEndVisible(opened, width)}${RESET}`
}

/** Submitted buffer counts as a past user message (and past-user paint) only when non-blank after trim. */
export function isCommittedInteractiveInput(submitted: string): boolean {
  return submitted.trim().length > 0
}

export function renderPastUserMessage(
  input: string,
  width: TerminalWidth
): string {
  const innerWidth = width - 2
  const lines = input.split('\n')
  const bg = terminalChalk.bgAnsi256(236)
  const emptyRow = bg(' '.repeat(innerWidth))
  const contentRows = lines.map((line) =>
    bg(` ${padEndVisible(line, innerWidth - 1)}`)
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
  return `${terminalChalk.cyan.bold(prefix)}${rest}`
}

/** Options for building the interactive command-line draft (logical rows before terminal width fit). */
type CommandInputDraftOptions = {
  placeholderContext?: PlaceholderContext
  /** UTF-16 index in `buffer`; when set, inserts inverse caret (Ink-ui TextInput–style). */
  caretOffset?: number
}

function commandInputDraftLinePrefix(
  lineIndex: number,
  context: PlaceholderContext
): string {
  return usesGreyNoArrowCommandLinePaint(context)
    ? ''
    : lineIndex === 0
      ? PROMPT
      : '  '
}

/**
 * Logical draft row: single-line TTY command buffer (prompt or grey placeholder, optional caret,
 * slash-command highlight). Pass `caretOffset` for the live TTY editor; omit for static strings.
 */
export function buildCommandInputDraftLines(
  buffer: string,
  _width: TerminalWidth,
  options?: CommandInputDraftOptions
): string[] {
  const context = options?.placeholderContext ?? 'default'
  const placeholder = PLACEHOLDER_BY_CONTEXT[context]
  const line = buffer
  const caretOffset = options?.caretOffset
  const prefix = commandInputDraftLinePrefix(0, context)

  if (caretOffset === undefined) {
    if (line === '') {
      return [`${prefix}${terminalChalk.gray(placeholder)}`]
    }
    return [prefix + highlightRecognizedCommand(line)]
  }

  const co = Math.max(0, Math.min(caretOffset, line.length))
  let inner: string
  if (line === '') {
    inner =
      co === 0
        ? `${terminalChalk.inverse(' ')}${terminalChalk.gray(placeholder)}`
        : `${terminalChalk.gray(placeholder)}`
  } else {
    inner = buildLineWithCaret(line, co)
  }
  return [prefix + inner]
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
    return `${before}${terminalChalk.inverse(' ')}`
  }
  const g = firstGraphemeFromOffset(line, o)
  const after = line.slice(o + g.length)
  return `${before}${terminalChalk.inverse(g)}${after}`
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
    return `${terminalChalk.cyan.bold(before)}${terminalChalk.inverse(g)}${terminalChalk.cyan.bold(afterInCmd)}${tail}`
  }
  const highlightedCmd = terminalChalk.cyan.bold(line.slice(0, highlightLen))
  const rest = line.slice(highlightLen)
  return highlightedCmd + plainInsertCaret(rest, offsetInLine - highlightLen)
}

export function getTerminalWidth(): number {
  return process.stdout.columns || 80
}

/** Blank line before the live Ink column when past messages exist but there is no Current prompt block. */
export function needsGapBeforeLiveRegion(
  pastMessages: PastMessages,
  currentPromptWrappedLines: string[],
  currentStageIndicatorLines: string[]
): boolean {
  return (
    pastMessages.length > 0 &&
    currentPromptWrappedLines.length === 0 &&
    currentStageIndicatorLines.length === 0
  )
}

function greyOutFullPaintRow(line: string): string {
  return `${GREY}${line.split(RESET).join(GREY)}${RESET}`
}

/**
 * Terminal rows for the Ink command line: draft with caret, grapheme-aware width fit, then
 * grey-out when typing is disabled (token list / fetch wait).
 */
export function formatInteractiveCommandLineInkRows(
  buffer: string,
  width: TerminalWidth,
  caretOffset: number,
  options?: CommandInputDraftOptions
): string[] {
  const draft = buildCommandInputDraftLines(buffer, width, {
    ...options,
    caretOffset,
  })
  const fitted = draft.map((line) =>
    padEndVisible(truncateToWidth(line, width), width)
  )
  const ctx = options?.placeholderContext ?? 'default'
  if (usesGreyNoArrowCommandLinePaint(ctx)) {
    return fitted.map(greyOutFullPaintRow)
  }
  return fitted
}

/**
 * Scroll-window **Current guidance** for plain rows (token list, tests, non-Ink paths): grey /
 * inverse highlight, optional per-item wrapped lines, then **truncate** each row to `width`.
 * Default command-line guidance uses {@link buildSuggestionLinesForInk} + Ink wrap instead.
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

/**
 * Current guidance for the default TTY live column: static `/ commands` hint (no completion list).
 * No per-line grapheme truncation — Ink `Text` `wrap` inside `Box width={terminalWidth}` (gate 4).
 */
export function buildSuggestionLinesForInk(
  _buffer: string,
  _highlightIndex: number
): string[] {
  return [COMMANDS_HINT]
}

/** SGR wrapper for one line of a past CLI assistant message. */
export function applyCliAssistantMessageTone(
  line: string,
  tone: CliAssistantMessageTone
): string {
  if (tone === 'error') return terminalChalk.red(line)
  if (tone === 'userNotice') return terminalChalk.gray.italic(line)
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
