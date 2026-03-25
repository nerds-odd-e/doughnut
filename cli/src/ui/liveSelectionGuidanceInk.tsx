import { useCallback, useRef } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import type { RecallMcqChoiceTexts } from '../types.js'
import {
  formatMcqChoiceLinesWithIndices,
  PROMPT,
  stripAnsi,
  type TerminalWidth,
} from '../renderer.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

/** Ink `useFocus` id while **Current guidance** list selection (recall MCQ or access-token picker) owns stdin. */
export const LIVE_SELECTION_GUIDANCE_INK_FOCUS_ID = 'live-selection-guidance'

function useLiveSelectionGuidanceStdin(
  onLogicalKey: (input: string, key: Key) => void | Promise<void>,
  onInterrupt: () => void
): void {
  useFocus({
    id: LIVE_SELECTION_GUIDANCE_INK_FOCUS_ID,
    autoFocus: true,
  })
  const onKeyRef = useRef(onLogicalKey)
  onKeyRef.current = onLogicalKey
  const handleKey = useCallback(
    (input: string, key: Key) => {
      if (key.ctrl && input === 'c') {
        onInterrupt()
        return
      }
      eachLogicalInkStdinChunk(input, key, (inp, ky) => {
        Promise.resolve(onKeyRef.current(inp, ky)).catch(() => undefined)
      })
    },
    [onInterrupt]
  )
  useInput(handleKey, { isActive: true })
}

export type RecallMcqChoicesLivePanelProps = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  choices: RecallMcqChoiceTexts
  highlightIndex: number
  lineDraft: string
  width: TerminalWidth
  onInterrupt: () => void
  onGuidanceListKey: (input: string, key: Key) => void | Promise<void>
}

/** Recall MCQ: numbered choices in Current guidance, optional answer draft after `PROMPT`. */
export function RecallMcqChoicesLivePanel({
  stageIndicatorLine,
  currentPromptLines,
  choices,
  highlightIndex,
  lineDraft,
  width,
  onInterrupt,
  onGuidanceListKey,
}: RecallMcqChoicesLivePanelProps) {
  useLiveSelectionGuidanceStdin(onGuidanceListKey, onInterrupt)

  const { lines, itemIndexPerLine } = formatMcqChoiceLinesWithIndices(
    choices,
    width
  )

  const promptPlain =
    currentPromptLines.length > 0
      ? currentPromptLines.map((l) => stripAnsi(l)).join('\n')
      : null

  return (
    <Box flexDirection="column" width={width}>
      <Text>{stageIndicatorLine}</Text>
      {promptPlain ? (
        <Text color="grey" wrap="wrap">
          {promptPlain}
        </Text>
      ) : null}
      {lines.map((line, i) => (
        <Text key={i} inverse={itemIndexPerLine[i] === highlightIndex}>
          {stripAnsi(line)}
        </Text>
      ))}
      <Text dimColor>↑↓ Enter or number to select; Esc to cancel</Text>
      <Text>
        {PROMPT}
        {lineDraft}
      </Text>
    </Box>
  )
}

export type AccessTokenPickerLivePanelProps = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  items: AccessTokenEntry[]
  defaultLabel: AccessTokenLabel | undefined
  highlightIndex: number
  onInterrupt: () => void
  onGuidanceListKey: (input: string, key: Key) => void | Promise<void>
}

/** `/list-access-token`, `/remove-access-token`, etc.: pick a row from Current guidance. */
export function AccessTokenPickerLivePanel({
  stageIndicatorLine,
  currentPromptLines,
  items,
  defaultLabel,
  highlightIndex,
  onInterrupt,
  onGuidanceListKey,
}: AccessTokenPickerLivePanelProps) {
  useLiveSelectionGuidanceStdin(onGuidanceListKey, onInterrupt)

  return (
    <Box flexDirection="column">
      <Text>{stageIndicatorLine}</Text>
      {currentPromptLines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
      {items.map((item, i) => (
        <Text key={item.label} inverse={i === highlightIndex}>
          {item.label === defaultLabel ? '★ ' : '  '}
          {item.label}
        </Text>
      ))}
      <Text dimColor>↑↓ Enter to select; other keys cancel</Text>
    </Box>
  )
}
