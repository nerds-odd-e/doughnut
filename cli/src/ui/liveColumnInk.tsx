import { useLayoutEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import type { InteractiveCommandInput } from '../interactiveCommandInput.js'
import type { RecallMcqChoiceTexts } from '../types.js'
import {
  PLACEHOLDER_BY_CONTEXT,
  PROMPT,
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
import { PatchedTextInput } from './PatchedTextInput.js'

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
  commandInput?: InteractiveCommandInput
  onCommandLineTyping?: (next: InteractiveCommandInput) => void
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

type LiveColumnPromptBlockProps = {
  width: TerminalWidth
  stageIndicatorLines: string[]
  currentPromptLines: string[]
  includeSeparatorWithoutStage: boolean
  tone: 'plain' | 'grey'
  stripPromptAnsi: boolean
}

function LiveColumnPromptBlock({
  width,
  stageIndicatorLines,
  currentPromptLines,
  includeSeparatorWithoutStage,
  tone,
  stripPromptAnsi,
}: LiveColumnPromptBlockProps): ReactNode {
  const promptLines = stripPromptAnsi
    ? currentPromptLines.map((line) => stripAnsi(line))
    : currentPromptLines
  const hasStageIndicator = stageIndicatorLines.length > 0
  const hasPromptLines = promptLines.length > 0
  if (!(hasStageIndicator || hasPromptLines)) return null
  const renderPrompt =
    tone === 'grey' ? (
      <Box width={width}>
        <Text color="grey" wrap="wrap">
          {promptLines.join('\n')}
        </Text>
      </Box>
    ) : (
      <>
        {promptLines.map((line, i) => (
          <Text key={`prompt-${i}`}>{line}</Text>
        ))}
      </>
    )
  return (
    <>
      {stageIndicatorLines.map((line, i) => (
        <Text key={`stage-${i}`}>{line}</Text>
      ))}
      {hasPromptLines ? (
        hasStageIndicator ? (
          <Text>{buildCurrentPromptSeparatorForStageBand(width)}</Text>
        ) : includeSeparatorWithoutStage ? (
          <Text>{buildCurrentPromptSeparator(width)}</Text>
        ) : null
      ) : null}
      {hasPromptLines ? renderPrompt : null}
    </>
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
  commandInput,
  onCommandLineTyping,
  aboveCommandLine,
  guidance,
}: LiveColumnInkPanelProps) {
  const refocusWhenUnfocused = stdinPolicy === 'commandLine'
  const stdinLogicalChunks = stdinPolicy === 'listSelection'
  const ignoreKeysWhenNotFocused = stdinPolicy === 'commandLine'
  const useCommandLineTextInput =
    stdinPolicy === 'commandLine' &&
    commandInput !== undefined &&
    onCommandLineTyping !== undefined

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
  const onTypingRef = useRef(onCommandLineTyping)
  onTypingRef.current = onCommandLineTyping
  const commandInputRef = useRef(commandInput)
  commandInputRef.current = commandInput

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
    { isActive: !useCommandLineTextInput }
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
      {useCommandLineTextInput ? (
        <Box>
          <Text>{PROMPT}</Text>
          <PatchedTextInput
            value={commandInputRef.current!.lineDraft}
            caretOffset={commandInputRef.current!.caretOffset}
            placeholderContext={placeholderContext}
            placeholder={PLACEHOLDER_BY_CONTEXT[placeholderContext]}
            maxWidth={width}
            isActive={
              !ignoreKeysWhenNotFocused ||
              isFocusedRef.current ||
              !inkFocusEverEstablishedRef.current
            }
            onChange={(nextValue, nextCaretOffset) => {
              onTypingRef.current!({
                ...commandInputRef.current!,
                lineDraft: nextValue,
                caretOffset: nextCaretOffset,
              })
            }}
            onSubmit={(submitted) => {
              Promise.resolve(
                onKeyRef.current(submitted, { return: true } as Key)
              ).catch(() => undefined)
            }}
            onUnhandledKey={(inp, ky) => {
              if (ky.ctrl && inp === 'c') {
                onInterrupt()
                return
              }
              Promise.resolve(onKeyRef.current(inp, ky)).catch(() => undefined)
            }}
          />
        </Box>
      ) : (
        commandPaintLines.map((line, i) => <Text key={`cmd-${i}`}>{line}</Text>)
      )}
      {guidance}
    </Box>
  )
}

type CommandLineLivePanelProps = {
  commandInput: InteractiveCommandInput
  width: TerminalWidth
  currentPromptWrappedLines: string[]
  currentGuidanceLines: string[]
  currentStageIndicatorLines: string[]
  placeholderContext: PlaceholderContext
  onCommandKey: (input: string, key: Key) => void | Promise<void>
  onCommandLineTyping: (next: InteractiveCommandInput) => void
  onInterrupt: () => void
}

export function CommandLineLivePanel({
  commandInput,
  width,
  currentPromptWrappedLines,
  currentGuidanceLines,
  currentStageIndicatorLines,
  placeholderContext,
  onCommandKey,
  onCommandLineTyping,
  onInterrupt,
}: CommandLineLivePanelProps) {
  const buffer = commandInput.lineDraft
  const caretOffset = commandInput.caretOffset
  const stageIndicatorLines = currentStageIndicatorLines.map((line) =>
    formatCurrentStageIndicatorLine(line, width)
  )
  const aboveCommandLine = (
    <LiveColumnPromptBlock
      width={width}
      stageIndicatorLines={stageIndicatorLines}
      currentPromptLines={currentPromptWrappedLines}
      includeSeparatorWithoutStage={true}
      tone="grey"
      stripPromptAnsi={true}
    />
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
      commandInput={commandInput}
      onCommandLineTyping={onCommandLineTyping}
      aboveCommandLine={aboveCommandLine}
      guidance={guidance}
    />
  )
}

type RecallMcqChoicesLivePanelProps = {
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
  const stageIndicatorLines = stageIndicatorLine ? [stageIndicatorLine] : []
  const aboveCommandLine = (
    <LiveColumnPromptBlock
      width={width}
      stageIndicatorLines={stageIndicatorLines}
      currentPromptLines={currentPromptLines}
      includeSeparatorWithoutStage={false}
      tone="grey"
      stripPromptAnsi={true}
    />
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

type AccessTokenPickerLivePanelProps = {
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
  const stageIndicatorLines = stageIndicatorLine ? [stageIndicatorLine] : []
  const aboveCommandLine = (
    <LiveColumnPromptBlock
      width={width}
      stageIndicatorLines={stageIndicatorLines}
      currentPromptLines={currentPromptLines}
      includeSeparatorWithoutStage={false}
      tone="plain"
      stripPromptAnsi={false}
    />
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
