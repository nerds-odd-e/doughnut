/**
 * Main command line: one `useInput` + session (`handleCommandLineInkInput`). Caret styling matches
 * `@inkjs/ui` TextInput (`chalk.inverse` in `renderer.ts`); stock TextInput is not mounted here
 * (single-line, uncontrolled-only upstream; multiline + ↑↓/Tab domain keys need this hook).
 */
import { useLayoutEffect, useRef } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import {
  buildCurrentPromptSeparator,
  buildCurrentPromptSeparatorForStageBand,
  formatCurrentStageIndicatorLine,
  formatInteractiveCommandLineInkRows,
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

  const paintOptions = { placeholderContext } as const
  const hasStageIndicator = currentStageIndicatorLines.length > 0
  const commandPaintLines = formatInteractiveCommandLineInkRows(
    buffer,
    width,
    caretOffset,
    paintOptions
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
      {commandPaintLines.map((line, i) => (
        <Text key={`cmd-${i}`}>{line}</Text>
      ))}
      {suggestionLines.map((line, i) => (
        <Box key={`sug-${i}`} width={width}>
          <Text wrap="wrap">{line}</Text>
        </Box>
      ))}
    </Box>
  )
}
