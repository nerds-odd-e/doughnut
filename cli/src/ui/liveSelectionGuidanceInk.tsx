import { Box, Text, type Key } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import type { RecallMcqChoiceTexts } from '../types.js'
import {
  buildCurrentPromptSeparatorForStageBand,
  formatMcqChoiceLinesWithIndices,
  stripAnsi,
  type TerminalWidth,
} from '../renderer.js'
import { PrimaryLiveInkPanel } from './PrimaryLiveInkPanel.js'

/** Ink `useFocus` id while **Current guidance** list selection (recall MCQ or access-token picker) owns stdin. */
export const LIVE_SELECTION_GUIDANCE_INK_FOCUS_ID = 'live-selection-guidance'

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
    <PrimaryLiveInkPanel
      focusId={LIVE_SELECTION_GUIDANCE_INK_FOCUS_ID}
      width={width}
      buffer={lineDraft}
      caretOffset={caretOffset}
      placeholderContext="recallMcq"
      onInkKey={onGuidanceListKey}
      onInterrupt={onInterrupt}
      refocusWhenUnfocused={false}
      stdinLogicalChunks
      ignoreKeysWhenNotFocused={false}
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
    <PrimaryLiveInkPanel
      focusId={LIVE_SELECTION_GUIDANCE_INK_FOCUS_ID}
      width={width}
      buffer={lineDraft}
      caretOffset={caretOffset}
      placeholderContext="tokenList"
      onInkKey={onGuidanceListKey}
      onInterrupt={onInterrupt}
      refocusWhenUnfocused={false}
      stdinLogicalChunks
      ignoreKeysWhenNotFocused={false}
      aboveCommandLine={aboveCommandLine}
      guidance={guidance}
    />
  )
}
