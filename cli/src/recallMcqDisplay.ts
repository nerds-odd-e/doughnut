import { renderMarkdownToTerminal } from './markdown.js'
import {
  renderCurrentGuidanceForSelectableLines,
  wrapTextToVisibleWidthLines,
  type TerminalWidth,
} from './renderer.js'
import type { RecallMcqChoiceTexts } from './types.js'

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

/** Numbered rows: piped scrollback and the plain rows passed into Current guidance highlighting. */
export function recallMcqNumberedChoiceLines(
  choices: RecallMcqChoiceTexts
): string[] {
  return choices.map((c, i) => `  ${i + 1}. ${choiceBodyOneVisibleLine(c)}`)
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
  return renderCurrentGuidanceForSelectableLines(
    recallMcqNumberedChoiceLines(choices),
    selectedChoiceIndex,
    width
  )
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
