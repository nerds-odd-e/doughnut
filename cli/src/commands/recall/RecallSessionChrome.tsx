import type { ReactNode } from 'react'
import { Box, Text } from 'ink'

const RECALL_NOTEBOOK_LINE_EMOJI = '📓'

export function RecallSessionChrome({
  notebookName,
  children,
}: {
  readonly notebookName?: string
  readonly children: ReactNode
}) {
  const trimmed = notebookName?.trim()
  const showNotebook = trimmed !== undefined && trimmed.length > 0
  return (
    <Box flexDirection="column">
      {showNotebook ? (
        <Text>
          {RECALL_NOTEBOOK_LINE_EMOJI} {trimmed}
        </Text>
      ) : null}
      {children}
    </Box>
  )
}
