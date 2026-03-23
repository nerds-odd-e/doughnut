import { Box, Text } from 'ink'
import { PROMPT } from '../renderer.js'

type Props = {
  stageIndicatorLine: string
  currentPromptLines: string[]
  choices: readonly string[]
  highlightIndex: number
}

export function McqDisplay({
  stageIndicatorLine,
  currentPromptLines,
  choices,
  highlightIndex,
}: Props) {
  return (
    <Box flexDirection="column">
      <Text>{stageIndicatorLine}</Text>
      {currentPromptLines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
      {choices.map((choice, i) => (
        <Text key={i} inverse={i === highlightIndex}>
          {'  '}
          {i + 1}. {choice}
        </Text>
      ))}
      <Text dimColor>↑↓ Enter or number to select; Esc to cancel</Text>
      <Text>{PROMPT}</Text>
    </Box>
  )
}
