import { useLayoutEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import type { RecallMcqChoiceTexts } from '../types.js'
import {
  buildCurrentPromptSeparator,
  buildCurrentPromptSeparatorForStageBand,
  formatCurrentStageIndicatorLine,
  formatInteractiveCommandLineInkRows,
  formatMcqChoiceLinesWithIndices,
  stripAnsi,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

/** Default shell: draft, slash completion, Tab cycle — refocus after Esc dismisses slash picker. */
export const COMMAND_LINE_INK_FOCUS_ID = 'command-line'

/**
 * Recall MCQ + access-token list: `dispatchSelectListKey` on stdin; logical byte coalescing
 * (`eachLogicalInkStdinChunk`). String value is the stable Ink `useFocus` id.
 */
export const LIST_SELECTION_INK_FOCUS_ID = 'live-selection-guidance'

/**
 * Mutually exclusive stdin contracts for the live column (see `Live column focus` in cli rules).
 * Not booleans: each mode fixes refocus, coalescing, and focus-loss key gating together.
 */
export type LiveColumnStdinPolicy = 'commandLine' | 'listSelection'

type LiveColumnInkPanelProps = {
  focusId: string
  width: TerminalWidth
  buffer: string
  caretOffset: number
  placeholderContext: PlaceholderContext
  stdinPolicy: LiveColumnStdinPolicy
  onInkKey: (input: string, key: Key) => void | Promise<void>
  onInterrupt: () => void
  /** Current prompt block (stage band, separators, grey stem lines) — above the command-line paint. */
  aboveCommandLine: ReactNode
  /** Current guidance — below the command-line paint (slash hints, MCQ rows, token rows). */
  guidance: ReactNode
}

type ListSelectionLiveColumnProps = Omit<
  LiveColumnInkPanelProps,
  'focusId' | 'stdinPolicy'
> & {
  /** MCQ vs token list: different `PLACEHOLDER_BY_CONTEXT` for the live strip. */
  placeholderContext: 'recallMcq' | 'tokenList'
}

function ListSelectionLiveColumn(props: ListSelectionLiveColumnProps) {
  return (
    <LiveColumnInkPanel
      {...props}
      focusId={LIST_SELECTION_INK_FOCUS_ID}
      stdinPolicy="listSelection"
    />
  )
}

function LiveColumnInkPanel({
  focusId,
  width,
  buffer,
  caretOffset,
  placeholderContext,
  stdinPolicy,
  onInkKey,
  onInterrupt,
  aboveCommandLine,
  guidance,
}: LiveColumnInkPanelProps) {
  const refocusWhenUnfocused = stdinPolicy === 'commandLine'
  const stdinLogicalChunks = stdinPolicy === 'listSelection'
  const ignoreKeysWhenNotFocused = stdinPolicy === 'commandLine'

  const { isFocused, focus } = useFocus({
    id: focusId,
    autoFocus: true,
  })
  const isFocusedRef = useRef(isFocused)
  isFocusedRef.current = isFocused
  const inkFocusEverEstablishedRef = useRef(false)
  if (isFocused) inkFocusEverEstablishedRef.current = true

  const onKeyRef = useRef(onInkKey)
  onKeyRef.current = onInkKey

  useLayoutEffect(() => {
    if (!refocusWhenUnfocused || isFocused) return
    focus(focusId)
  }, [refocusWhenUnfocused, isFocused, focus, focusId])

  useInput(
    (input, key) => {
      if (key.ctrl && input === 'c') {
        onInterrupt()
        return
      }
      if (
        ignoreKeysWhenNotFocused &&
        !isFocusedRef.current &&
        inkFocusEverEstablishedRef.current
      ) {
        return
      }
      const dispatch = (inp: string, ky: Key) => {
        Promise.resolve(onKeyRef.current(inp, ky)).catch(() => undefined)
      }
      if (stdinLogicalChunks) {
        eachLogicalInkStdinChunk(input, key, dispatch)
      } else {
        dispatch(input, key)
      }
    },
    { isActive: true }
  )

  const commandPaintLines = formatInteractiveCommandLineInkRows(
    buffer,
    width,
    caretOffset,
    { placeholderContext }
  )

  return (
    <Box flexDirection="column" width={width}>
      {aboveCommandLine}
      {commandPaintLines.map((line, i) => (
        <Text key={`cmd-${i}`}>{line}</Text>
      ))}
      {guidance}
    </Box>
  )
}

export type CommandLineLivePanelProps = {
  buffer: string
  caretOffset: number
  width: TerminalWidth
  currentPromptWrappedLines: string[]
  currentGuidanceLines: string[]
  currentStageIndicatorLines: string[]
  placeholderContext: PlaceholderContext
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
    <LiveColumnInkPanel
      focusId={COMMAND_LINE_INK_FOCUS_ID}
      width={width}
      buffer={buffer}
      caretOffset={caretOffset}
      placeholderContext={placeholderContext}
      stdinPolicy="commandLine"
      onInkKey={onCommandKey}
      onInterrupt={onInterrupt}
      aboveCommandLine={aboveCommandLine}
      guidance={guidance}
    />
  )
}

export type RecallMcqChoicesLivePanelProps = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  choices: RecallMcqChoiceTexts
  highlightIndex: number
  lineDraft: string
  caretOffset: number
  width: TerminalWidth
  onInterrupt: () => void
  onGuidanceListKey: (input: string, key: Key) => void | Promise<void>
}

export function RecallMcqChoicesLivePanel({
  stageIndicatorLine,
  currentPromptLines,
  choices,
  highlightIndex,
  lineDraft,
  caretOffset,
  width,
  onInterrupt,
  onGuidanceListKey,
}: RecallMcqChoicesLivePanelProps) {
  const { lines, itemIndexPerLine } = formatMcqChoiceLinesWithIndices(
    choices,
    width
  )

  const promptPlain =
    currentPromptLines.length > 0
      ? currentPromptLines.map((l) => stripAnsi(l)).join('\n')
      : null

  const aboveCommandLine = (
    <>
      {stageIndicatorLine ? (
        <>
          <Text>{stageIndicatorLine}</Text>
          <Text>{buildCurrentPromptSeparatorForStageBand(width)}</Text>
        </>
      ) : null}
      {promptPlain ? (
        <Box width={width}>
          <Text color="grey" wrap="wrap">
            {promptPlain}
          </Text>
        </Box>
      ) : null}
    </>
  )

  const guidance = lines.map((line, i) => (
    <Text key={i} inverse={itemIndexPerLine[i] === highlightIndex}>
      {stripAnsi(line)}
    </Text>
  ))

  return (
    <ListSelectionLiveColumn
      width={width}
      buffer={lineDraft}
      caretOffset={caretOffset}
      placeholderContext="recallMcq"
      onInkKey={onGuidanceListKey}
      onInterrupt={onInterrupt}
      aboveCommandLine={aboveCommandLine}
      guidance={guidance}
    />
  )
}

export type AccessTokenPickerLivePanelProps = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  lineDraft: string
  caretOffset: number
  width: TerminalWidth
  items: AccessTokenEntry[]
  defaultLabel: AccessTokenLabel | undefined
  highlightIndex: number
  onInterrupt: () => void
  onGuidanceListKey: (input: string, key: Key) => void | Promise<void>
}

export function AccessTokenPickerLivePanel({
  stageIndicatorLine,
  currentPromptLines,
  lineDraft,
  caretOffset,
  width,
  items,
  defaultLabel,
  highlightIndex,
  onInterrupt,
  onGuidanceListKey,
}: AccessTokenPickerLivePanelProps) {
  const aboveCommandLine = (
    <>
      <Text>{stageIndicatorLine}</Text>
      <Text>{buildCurrentPromptSeparatorForStageBand(width)}</Text>
      {currentPromptLines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
    </>
  )

  const guidance = items.map((item, i) => (
    <Text key={item.label} inverse={i === highlightIndex}>
      {item.label === defaultLabel ? '★ ' : '  '}
      {item.label}
    </Text>
  ))

  return (
    <ListSelectionLiveColumn
      width={width}
      buffer={lineDraft}
      caretOffset={caretOffset}
      placeholderContext="tokenList"
      onInkKey={onGuidanceListKey}
      onInterrupt={onInterrupt}
      aboveCommandLine={aboveCommandLine}
      guidance={guidance}
    />
  )
}
