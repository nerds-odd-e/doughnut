import { Box, Text } from 'ink'
import type { AccessTokenEntry, AccessTokenLabel } from '../accessToken.js'

type Props = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  items: AccessTokenEntry[]
  defaultLabel: AccessTokenLabel | undefined
  highlightIndex: number
}

export function TokenListDisplay({
  stageIndicatorLine,
  currentPromptLines,
  items,
  defaultLabel,
  highlightIndex,
}: Props) {
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
