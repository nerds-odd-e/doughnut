import { Box, Text, useInput, type Key } from 'ink'
import {
  buildBoxLinesWithCaret,
  buildCurrentPromptSeparator,
  buildCurrentPromptSeparatorForStageBand,
  formatCurrentStageIndicatorLine,
  grayDisabledInputBoxLines,
  isGreyDisabledInputChrome,
  renderBox,
  stripAnsi,
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
  /** Ink-owned stdin for the main command line (phase 2); not used for alternate live panels. */
  onCommandKey: (input: string, key: Key) => void | Promise<void>
  onInterrupt: () => void
}

export function CommandLineLivePanel({
  buffer,
  caretOffset,
  width,
  currentPromptWrappedLines,
  suggestionLines,
  currentStageIndicatorLines,
  placeholderContext,
  onCommandKey,
  onInterrupt,
}: CommandLineLivePanelProps) {
  useInput(
    (input, key) => {
      if (key.ctrl && input === 'c') {
        onInterrupt()
        return
      }
      Promise.resolve(onCommandKey(input, key)).catch(() => undefined)
    },
    { isActive: true }
  )

  const paint: LiveRegionPaintOptions = { placeholderContext }
  const hasStageIndicator = currentStageIndicatorLines.length > 0
  const rawBoxLines = renderBox(
    buildBoxLinesWithCaret(buffer, width, caretOffset, paint),
    width
  ).split('\n')
  const boxLines =
    placeholderContext && isGreyDisabledInputChrome(placeholderContext)
      ? grayDisabledInputBoxLines(rawBoxLines)
      : rawBoxLines

  const promptPlainForInk =
    currentPromptWrappedLines.length > 0
      ? currentPromptWrappedLines.map((l) => stripAnsi(l)).join('\n')
      : null

  return (
    <Box flexDirection="column">
      {hasStageIndicator ? (
        <>
          {currentStageIndicatorLines.map((ind, i) => (
            <Text key={`stage-${i}`}>
              {formatCurrentStageIndicatorLine(ind, width)}
            </Text>
          ))}
          <Text>{buildCurrentPromptSeparatorForStageBand(width)}</Text>
        </>
      ) : null}
      {promptPlainForInk ? (
        <>
          {!hasStageIndicator ? (
            <Text>{buildCurrentPromptSeparator(width)}</Text>
          ) : null}
          <Box width={width}>
            <Text color="grey" wrap="wrap">
              {promptPlainForInk}
            </Text>
          </Box>
        </>
      ) : null}
      {boxLines.map((line, i) => (
        <Text key={`box-${i}`}>{line}</Text>
      ))}
      {suggestionLines.map((line, i) => (
        <Box key={`sug-${i}`} width={width}>
          <Text wrap="wrap">{line}</Text>
        </Box>
      ))}
    </Box>
  )
}
