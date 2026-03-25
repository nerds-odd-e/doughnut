import { useCallback, useRef } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'
import { LIST_SELECT_INK_FOCUS_ID } from './NumberedChoiceListLivePanel.js'

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

export type TokenListLivePanelProps = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  items: AccessTokenEntry[]
  defaultLabel: AccessTokenLabel | undefined
  highlightIndex: number
  onInterrupt: () => void
  onInkKey: (input: string, key: Key) => void | Promise<void>
}

export function TokenListLivePanel({
  stageIndicatorLine,
  currentPromptLines,
  items,
  defaultLabel,
  highlightIndex,
  onInterrupt,
  onInkKey,
}: TokenListLivePanelProps) {
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
