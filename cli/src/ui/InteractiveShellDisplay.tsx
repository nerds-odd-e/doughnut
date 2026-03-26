import { Box, Newline, Static, Text } from 'ink'
import type { ReactNode } from 'react'
import type { PastMessage } from '../types.js'
import {
  applyCliAssistantMessageTone,
  renderPastUserMessage,
} from '../renderer.js'

function PastMessageBlock({
  entry,
  width,
}: {
  entry: PastMessage
  width: number
}) {
  if (entry.role === 'user') {
    const raw = renderPastUserMessage(entry.content, width)
    const lines = raw.split('\n')
    while (lines.length > 0 && lines[lines.length - 1] === '') {
      lines.pop()
    }
    return (
      <Box flexDirection="column">
        {lines.map((line, i) => (
          <Text key={i}>{line}</Text>
        ))}
      </Box>
    )
  }
  const tone = entry.tone ?? 'plain'
  return (
    <Box flexDirection="column">
      {entry.lines.map((line, i) => (
        <Text key={i}>{applyCliAssistantMessageTone(line, tone)}</Text>
      ))}
    </Box>
  )
}

export type InteractiveShellDisplayProps = {
  pastMessages: PastMessage[]
  terminalWidth: number
  liveLeadingGap: boolean
  livePanel: ReactNode
}

export function InteractiveShellDisplay({
  pastMessages,
  terminalWidth,
  liveLeadingGap,
  livePanel,
}: InteractiveShellDisplayProps) {
  return (
    <Box flexDirection="column">
      <Static items={pastMessages} style={{ position: 'relative' }}>
        {(entry, index) => (
          <Box key={index} flexDirection="column">
            <PastMessageBlock entry={entry} width={terminalWidth} />
          </Box>
        )}
      </Static>
      <Box flexDirection="column">
        {liveLeadingGap ? <Newline /> : null}
        {livePanel}
      </Box>
    </Box>
  )
}
