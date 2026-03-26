import { Spinner } from '@inkjs/ui'
import { Box, Text, useInput } from 'ink'
import type { InteractiveFetchWaitLine } from '../interactiveFetchWait.js'
import { PLACEHOLDER_BY_CONTEXT } from '../renderer.js'

type Props = {
  waitLine: InteractiveFetchWaitLine
  onEscapeCancel: () => void
  onInterrupt: () => void
}

export function FetchWaitDisplay({
  waitLine,
  onEscapeCancel,
  onInterrupt,
}: Props) {
  useInput((input, key) => {
    if (key.ctrl && input === 'c') {
      onInterrupt()
      return
    }
    if (key.escape) {
      onEscapeCancel()
    }
  })

  return (
    <Box flexDirection="column">
      <Spinner label={waitLine} type="dots" />
      <Text dimColor>{PLACEHOLDER_BY_CONTEXT.interactiveFetchWait}</Text>
    </Box>
  )
}
