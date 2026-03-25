import { useCallback, useRef } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import {
  formatMcqChoiceLinesWithIndices,
  PROMPT,
  stripAnsi,
  type TerminalWidth,
} from '../renderer.js'

export const LIST_SELECT_INK_FOCUS_ID = 'list-select'

function emptyInkKey(): Key {
  return {
    upArrow: false,
    downArrow: false,
    leftArrow: false,
    rightArrow: false,
    pageDown: false,
    pageUp: false,
    home: false,
    end: false,
    return: false,
    escape: false,
    ctrl: false,
    shift: false,
    tab: false,
    backspace: false,
    delete: false,
    meta: false,
    super: false,
    hyper: false,
    capsLock: false,
    numLock: false,
  }
}

export type NumberedChoiceListLivePanelProps = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  choices: readonly string[]
  highlightIndex: number
  lineDraft: string
  width: TerminalWidth
  onInterrupt: () => void
  onInkKey: (input: string, key: Key) => void | Promise<void>
}

export function NumberedChoiceListLivePanel({
  stageIndicatorLine,
  currentPromptLines,
  choices,
  highlightIndex,
  lineDraft,
  width,
  onInterrupt,
  onInkKey,
}: NumberedChoiceListLivePanelProps) {
  useFocus({
    id: LIST_SELECT_INK_FOCUS_ID,
    autoFocus: true,
  })

  const onInkKeyRef = useRef(onInkKey)
  onInkKeyRef.current = onInkKey

  const handleKey = useCallback(
    (input: string, key: Key) => {
      if (key.ctrl && input === 'c') {
        onInterrupt()
        return
      }

      const processOne = (inp: string, ky: Key) => {
        Promise.resolve(onInkKeyRef.current(inp, ky)).catch(() => undefined)
      }

      const last = input.at(-1)
      if (
        input.length >= 2 &&
        (last === '\r' || last === '\n') &&
        /^[\x20-\x7e]+$/.test(input.slice(0, -1))
      ) {
        for (const c of input.slice(0, -1)) {
          processOne(c, emptyInkKey())
        }
        processOne(last!, { ...emptyInkKey(), return: true })
        return
      }
      processOne(input, key)
    },
    [onInterrupt]
  )

  useInput(handleKey, { isActive: true })

  const { lines, itemIndexPerLine } = formatMcqChoiceLinesWithIndices(
    [...choices],
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
