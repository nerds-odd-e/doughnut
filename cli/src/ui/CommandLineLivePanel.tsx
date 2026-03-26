import { Box, Text, type Key } from 'ink'
import {
  buildCurrentPromptSeparator,
  buildCurrentPromptSeparatorForStageBand,
  formatCurrentStageIndicatorLine,
  stripAnsi,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'
import { PrimaryLiveInkPanel } from './PrimaryLiveInkPanel.js'

export const COMMAND_LINE_INK_FOCUS_ID = 'command-line'

export type CommandLineLivePanelProps = {
  buffer: string
  caretOffset: number
  width: TerminalWidth
  currentPromptWrappedLines: string[]
  /** Current guidance (slash hint + completion list); Ink `Text` wrap — see `buildSuggestionLinesForInk`. */
  currentGuidanceLines: string[]
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
  currentGuidanceLines,
  currentStageIndicatorLines,
  placeholderContext,
  onCommandKey,
  onInterrupt,
}: CommandLineLivePanelProps) {
  const hasStageIndicator = currentStageIndicatorLines.length > 0
  const promptPlainForInk =
    currentPromptWrappedLines.length > 0
      ? currentPromptWrappedLines.map((l) => stripAnsi(l)).join('\n')
      : null

  const aboveCommandLine = (
    <>
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
    </>
  )

  const guidance = currentGuidanceLines.map((line, i) => (
    <Box key={`sug-${i}`} width={width}>
      <Text wrap="wrap">{line}</Text>
    </Box>
  ))

  return (
    <PrimaryLiveInkPanel
      focusId={COMMAND_LINE_INK_FOCUS_ID}
      width={width}
      buffer={buffer}
      caretOffset={caretOffset}
      placeholderContext={placeholderContext}
      onInkKey={onCommandKey}
      onInterrupt={onInterrupt}
      refocusWhenUnfocused
      stdinLogicalChunks={false}
      ignoreKeysWhenNotFocused
      aboveCommandLine={aboveCommandLine}
      guidance={guidance}
    />
  )
}
