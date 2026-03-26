import { useLayoutEffect, useRef } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import {
  buildBoxLinesWithCaret,
  buildCurrentPromptSeparator,
  buildCurrentPromptSeparatorForStageBand,
  formatBorderlessCommandInputPaintLines,
  formatCurrentStageIndicatorLine,
  inputBoxBorderLinesWithContextChrome,
  stripAnsi,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'
export const COMMAND_LINE_INK_FOCUS_ID = 'command-line'

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
  const { isFocused, focus } = useFocus({
    id: COMMAND_LINE_INK_FOCUS_ID,
    autoFocus: true,
  })
  const isFocusedRef = useRef(isFocused)
  isFocusedRef.current = isFocused
  const inkFocusEverEstablishedRef = useRef(false)
  if (isFocused) inkFocusEverEstablishedRef.current = true

  useLayoutEffect(() => {
    if (isFocused) return
    focus(COMMAND_LINE_INK_FOCUS_ID)
  }, [isFocused, focus])

  useInput(
    (input, key) => {
      if (key.ctrl && input === 'c') {
        onInterrupt()
        return
      }
      if (!isFocusedRef.current && inkFocusEverEstablishedRef.current) return
      Promise.resolve(onCommandKey(input, key)).catch(() => undefined)
    },
    { isActive: true }
  )

  const boxPaint = { placeholderContext } as const
  const hasStageIndicator = currentStageIndicatorLines.length > 0
  const innerLines = buildBoxLinesWithCaret(
    buffer,
    width,
    caretOffset,
    boxPaint
  )
  const boxLines = inputBoxBorderLinesWithContextChrome(
    formatBorderlessCommandInputPaintLines(innerLines, width),
    boxPaint
  )

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
