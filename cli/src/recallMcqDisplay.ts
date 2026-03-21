import { renderMarkdownToTerminal } from './markdown.js'
import {
  CURRENT_GUIDANCE_MAX_VISIBLE,
  formatHighlightedListByItem,
} from './listDisplay.js'
import {
  truncateToWidth,
  visibleLength,
  wrapTextToVisibleWidthLines,
  type TerminalWidth,
} from './renderer.js'
import type { RecallMcqChoiceTexts } from './types.js'

const DEFAULT_NUMBERED_CHOICE_WRAP_WIDTH = 4096

function collapseSoftLineBreaksToSingleLine(text: string): string {
  return text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
    .join(' ')
}

function choiceBodyOneVisibleLine(apiChoiceText: string): string {
  const normalized = collapseSoftLineBreaksToSingleLine(apiChoiceText)
  const rendered = renderMarkdownToTerminal(normalized)
  return collapseSoftLineBreaksToSingleLine(rendered)
}

function recallMcqNumberedChoiceLinesAndIndices(
  choices: RecallMcqChoiceTexts,
  width: TerminalWidth
): { lines: string[]; itemIndexPerLine: number[] } {
  const lines: string[] = []
  const itemIndexPerLine: number[] = []
  for (let i = 0; i < choices.length; i++) {
    const body = choiceBodyOneVisibleLine(choices[i]!)
    const prefix = `  ${i + 1}. `
    const indent = ' '.repeat(visibleLength(prefix))
    const innerWidth = Math.max(1, width - visibleLength(prefix))
    const bodyLines = wrapTextToVisibleWidthLines(body, innerWidth)
    if (bodyLines.length === 0) {
      lines.push(prefix)
      itemIndexPerLine.push(i)
      continue
    }
    for (let r = 0; r < bodyLines.length; r++) {
      const rowPrefix = r === 0 ? prefix : indent
      lines.push(rowPrefix + bodyLines[r]!)
      itemIndexPerLine.push(i)
    }
  }
  return { lines, itemIndexPerLine }
}

/**
 * Numbered rows for scrollback (and tests): wraps each choice to `width` (default: very wide → one row per choice when text fits).
 */
export function recallMcqNumberedChoiceLines(
  choices: RecallMcqChoiceTexts,
  width: TerminalWidth = DEFAULT_NUMBERED_CHOICE_WRAP_WIDTH
): string[] {
  return recallMcqNumberedChoiceLinesAndIndices(choices, width).lines
}

/**
 * TTY Current guidance under the input box: grey list, one reversed row for
 * `selectedChoiceIndex` (0-based).
 */
export function recallMcqCurrentGuidanceLines(
  choices: RecallMcqChoiceTexts,
  selectedChoiceIndex: number,
  width: TerminalWidth
): string[] {
  const { lines, itemIndexPerLine } = recallMcqNumberedChoiceLinesAndIndices(
    choices,
    width
  )
  return formatHighlightedListByItem(
    lines,
    itemIndexPerLine,
    CURRENT_GUIDANCE_MAX_VISIBLE,
    selectedChoiceIndex
  ).map((line) => truncateToWidth(line, width))
}

/**
 * MCQ stem already rendered for the terminal; preserve markdown hard breaks as empty
 * paragraphs, then wrap each segment to the live-region width (ANSI preserved).
 */
export function recallMcqStemWrappedLinesForCurrentPrompt(
  stemRenderedForTerminal: string,
  width: TerminalWidth
): string[] {
  return stemRenderedForTerminal
    .split('\n')
    .flatMap((paragraph) =>
      paragraph.length === 0
        ? ['']
        : wrapTextToVisibleWidthLines(paragraph, width)
    )
}
