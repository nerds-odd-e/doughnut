import { Box, Newline, Static, Text } from 'ink'
import type { ReactNode } from 'react'
import type { ChatHistory, ChatHistoryEntry } from '../types.js'
import { applyChatHistoryOutputTone, renderPastInput } from '../renderer.js'

function HistoryBlock({
  entry,
  width,
}: {
  entry: ChatHistoryEntry
  width: number
}) {
  if (entry.type === 'input') {
    const raw = renderPastInput(entry.content, width)
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
        <Text key={i}>{applyChatHistoryOutputTone(line, tone)}</Text>
      ))}
    </Box>
  )
}

export type InteractiveShellDisplayProps = {
  history: ChatHistory
  terminalWidth: number
  liveLeadingGap: boolean
  livePanel: ReactNode
}

export function InteractiveShellDisplay({
  history,
  terminalWidth,
  liveLeadingGap,
  livePanel,
}: InteractiveShellDisplayProps) {
  return (
    <Box flexDirection="column">
      <Static items={history} style={{ position: 'relative' }}>
        {(entry, index) => (
          <Box key={index} flexDirection="column">
            <HistoryBlock entry={entry} width={terminalWidth} />
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
