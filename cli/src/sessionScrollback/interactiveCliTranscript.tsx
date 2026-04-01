import { Box, Text } from 'ink'
import { PastAssistantErrorBlock } from '../commonUIComponents/pastAssistantErrorBlock.js'
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

export function transcriptAssistantError(text: string): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element: <PastAssistantErrorBlock text={text} />,
  }
}
