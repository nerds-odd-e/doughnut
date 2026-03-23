import { Box, Text } from 'ink'

type Props = {
  guidanceLines: string[]
  placeholderText: string
  hint: string
  draft: string
}

export function ConfirmDisplay({
  guidanceLines,
  placeholderText,
  hint,
  draft,
}: Props) {
  return (
    <Box flexDirection="column">
      {guidanceLines.map((line, i) => (
        <Text key={i}>{line}</Text>
      ))}
      {hint ? <Text color="red">{hint}</Text> : null}
      <Text dimColor={draft === ''}>
        {draft === '' ? placeholderText : draft}
      </Text>
    </Box>
  )
}
