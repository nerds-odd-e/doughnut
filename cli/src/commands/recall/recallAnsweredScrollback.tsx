import { Box, Text } from 'ink'

export type RecallAnsweredItem = {
  readonly kind: 'recall_answered'
  readonly id: string
  readonly text: string
}

export function recallAnsweredLine(text: string): RecallAnsweredItem {
  return { kind: 'recall_answered', id: crypto.randomUUID(), text }
}

export function RecallAnsweredRow(props: { readonly text: string }) {
  const { text } = props
  return (
    <Box>
      <Text>{text}</Text>
    </Box>
  )
}
