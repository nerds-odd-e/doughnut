import { useCallback, useRef } from 'react'
import { Spinner } from '@inkjs/ui'
import { Box, Text, useFocus, useInput, type Key } from 'ink'
import type { InteractiveFetchWaitLine } from '../interactiveFetchWait.js'
import { PLACEHOLDER_BY_CONTEXT } from '../renderer.js'
import { eachLogicalInkStdinChunk } from './inkStdinLogicalKeys.js'

const FETCH_WAIT_INK_FOCUS_ID = 'fetch-wait'

type Props = {
  waitLine: InteractiveFetchWaitLine
  onCancelWait: () => void
}

export function FetchWaitDisplay({ waitLine, onCancelWait }: Props) {
  const onCancelWaitRef = useRef(onCancelWait)
  onCancelWaitRef.current = onCancelWait

  const { isFocused } = useFocus({
    id: FETCH_WAIT_INK_FOCUS_ID,
    autoFocus: true,
  })
  const isFocusedRef = useRef(isFocused)
  isFocusedRef.current = isFocused
  const inkFocusEverEstablishedRef = useRef(false)
  if (isFocused) inkFocusEverEstablishedRef.current = true

  const handleKey = useCallback((input: string, key: Key) => {
    eachLogicalInkStdinChunk(input, key, (inp, ky) => {
      if (!isFocusedRef.current && inkFocusEverEstablishedRef.current) return
      const isEscape =
        ky.escape || ('name' in ky && ky.name === 'escape') || inp === '\u001b'
      if (isEscape) {
        onCancelWaitRef.current()
      }
    })
  }, [])

  useInput(handleKey, { isActive: true })

  return (
    <Box flexDirection="column">
      <Spinner label={waitLine} type="dots" />
      <Text dimColor>{PLACEHOLDER_BY_CONTEXT.interactiveFetchWait}</Text>
    </Box>
  )
}
