import { useLayoutEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import {
  formatInteractiveCommandLineInkRows,
  type PlaceholderContext,
  type TerminalWidth,
} from '../renderer.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

export type PrimaryLiveInkPanelProps = {
  focusId: string
  width: TerminalWidth
  buffer: string
  caretOffset: number
  placeholderContext: PlaceholderContext
  onInkKey: (input: string, key: Key) => void | Promise<void>
  onInterrupt: () => void
  /** True: after Ink clears focus (e.g. slash-picker Esc), take focus back — default command line only. */
  refocusWhenUnfocused: boolean
  /** True: split merged stdin into logical keys — list selection handlers. */
  stdinLogicalChunks: boolean
  /** True: drop keys once focus left the region — default command line only. */
  ignoreKeysWhenNotFocused: boolean
  aboveCommandLine: ReactNode
  guidance: ReactNode
}

export function PrimaryLiveInkPanel({
  focusId,
  width,
  buffer,
  caretOffset,
  placeholderContext,
  onInkKey,
  onInterrupt,
  refocusWhenUnfocused,
  stdinLogicalChunks,
  ignoreKeysWhenNotFocused,
  aboveCommandLine,
  guidance,
}: PrimaryLiveInkPanelProps) {
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
