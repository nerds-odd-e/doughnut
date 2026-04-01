import { Box, Text } from 'ink'
import { PastUserMessageBlock } from '../commonUIComponents/pastUserMessageBlock.js'
import type { SessionScrollbackItem } from './SessionScrollback.js'

export function transcriptUserLine(text: string): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element: <PastUserMessageBlock text={text} />,
    endsWithUserLine: true,
  }
}

export function transcriptAssistantText(text: string): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element: (
      <Box>
        <Text>{text}</Text>
      </Box>
    ),
  }
}
