import { Box, Text } from 'ink'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'

export function RecallAnsweredRow(props: { readonly text: string }) {
  const { text } = props
  return (
    <Box>
      <Text>{text}</Text>
    </Box>
  )
}

export function recallAnsweredLine(text: string): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element: <RecallAnsweredRow text={text} />,
  }
}
