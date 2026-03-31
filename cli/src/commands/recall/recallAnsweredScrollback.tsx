import { Box, Text } from 'ink'
import type { ReactElement, ReactNode } from 'react'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'

export const RECALL_ANSWERED_BREADCRUMB_SEP = ' › '

export function recallAnsweredPlainInk(text: string): ReactElement {
  return (
    <Box>
      <Text>{text}</Text>
    </Box>
  )
}

export function recallAnsweredScrollbackItem(
  element: ReactNode
): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element,
  }
}
