import { Box, Text } from 'ink'
import type { InteractiveFetchWaitLine } from '../interactiveFetchWait.js'
import {
  formatInteractiveFetchWaitPromptLine,
  PLACEHOLDER_BY_CONTEXT,
} from '../renderer.js'

type Props = {
  waitLine: InteractiveFetchWaitLine
  ellipsisTick: number
}

export function FetchWaitDisplay({ waitLine, ellipsisTick }: Props) {
  const label = formatInteractiveFetchWaitPromptLine(waitLine, ellipsisTick)
  return (
    <Box flexDirection="column">
      <Text color="blue">{label}</Text>
      <Text dimColor>{PLACEHOLDER_BY_CONTEXT.interactiveFetchWait}</Text>
    </Box>
  )
}
