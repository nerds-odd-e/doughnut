import { Box, Text } from 'ink'
import {
  buildLiveRegionLinesWithCaret,
  type LiveRegionPaintOptions,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'

export type CommandLineLivePanelProps = {
  buffer: string
  caretOffset: number
  width: TerminalWidth
  currentPromptWrappedLines: string[]
  suggestionLines: string[]
  currentStageIndicatorLines: string[]
  placeholderContext: PlaceholderContext
}

export function CommandLineLivePanel({
  buffer,
  caretOffset,
  width,
  currentPromptWrappedLines,
  suggestionLines,
  currentStageIndicatorLines,
  placeholderContext,
}: CommandLineLivePanelProps) {
  const paint: LiveRegionPaintOptions = { placeholderContext }
  const lines = buildLiveRegionLinesWithCaret(
    buffer,
    width,
    caretOffset,
    currentPromptWrappedLines,
    suggestionLines,
    currentStageIndicatorLines,
    paint
  )
  return (
    <Box flexDirection="column">
      {lines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
    </Box>
  )
}
