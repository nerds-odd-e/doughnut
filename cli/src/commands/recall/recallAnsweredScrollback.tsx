import { Box, Text } from 'ink'
import type { SessionScrollbackItem } from '../../sessionScrollback/SessionScrollback.js'
import type { RecallAnsweredRowPayload } from './recallAnsweredRowPayload.js'

export function RecallAnsweredRow(props: {
  readonly payload: RecallAnsweredRowPayload
}) {
  const { payload } = props
  switch (payload.kind) {
    case 'plain':
      return (
        <Box>
          <Text>{payload.text}</Text>
        </Box>
      )
  }
}

export function recallAnsweredScrollbackItem(
  payload: RecallAnsweredRowPayload
): SessionScrollbackItem {
  return {
    id: crypto.randomUUID(),
    element: <RecallAnsweredRow payload={payload} />,
  }
}
