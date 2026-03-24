import { Box } from 'ink'
import {
  buildBoxLinesWithCaret,
  buildLiveRegionHeaderLines,
  grayDisabledInputBoxLines,
  isGreyDisabledInputChrome,
  renderBox,
  type PlaceholderContext,
} from '../renderer.js'
import { LiveRegionLines } from './LiveRegionLines.js'

export type CommandLineLivePanelProps = {
  terminalWidth: number
  currentPromptWrappedLines: readonly string[]
  currentStageIndicatorLines: readonly string[]
  suggestionLines: readonly string[]
  buffer: string
  caretOffset: number
  placeholderContext: PlaceholderContext
}

export function CommandLineLivePanel({
  terminalWidth,
  currentPromptWrappedLines,
  currentStageIndicatorLines,
  suggestionLines,
  buffer,
  caretOffset,
  placeholderContext,
}: CommandLineLivePanelProps) {
  const headerLines = buildLiveRegionHeaderLines(
    terminalWidth,
    [...currentPromptWrappedLines],
    [...currentStageIndicatorLines]
  )
  const inner = buildBoxLinesWithCaret(
    buffer,
    terminalWidth,
    {
      placeholderContext,
    },
    caretOffset
  )
  let boxLines = renderBox(inner, terminalWidth).split('\n')
  if (isGreyDisabledInputChrome(placeholderContext)) {
    boxLines = grayDisabledInputBoxLines(boxLines)
  }
  return (
    <Box flexDirection="column">
      <LiveRegionLines lines={headerLines} />
      <LiveRegionLines lines={boxLines} />
      <LiveRegionLines lines={suggestionLines} />
    </Box>
  )
}
