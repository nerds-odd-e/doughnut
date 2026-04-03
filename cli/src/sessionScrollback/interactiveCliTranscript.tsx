import { Box, Text } from 'ink'
import { PastAssistantErrorBlock } from '../commonUIComponents/pastAssistantErrorBlock.js'
import { PastUserMessageBlock } from '../commonUIComponents/pastUserMessageBlock.js'
import type { SessionScrollbackItem } from './SessionScrollback.js'

export const scrollbackUserMessageItem = (
  text: string
): SessionScrollbackItem => ({
  id: crypto.randomUUID(),
  element: <PastUserMessageBlock text={text} />,
  endsWithUserLine: true,
})

export const scrollbackAssistantTextMessageItem = (
  text: string
): SessionScrollbackItem => ({
  id: crypto.randomUUID(),
  element: (
    <Box>
      <Text>{text}</Text>
    </Box>
  ),
})

export const scrollbackErrorItem = (text: string): SessionScrollbackItem => ({
  id: crypto.randomUUID(),
  element: <PastAssistantErrorBlock text={text} />,
})
