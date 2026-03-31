import { Box, Text } from 'ink'
import type { ScrollbackEntry } from './scrollbackEntry.js'
import { PastUserMessageBlock } from '../pastUserMessageBlock.js'

type ScrollbackLineProps = {
  readonly entry: ScrollbackEntry
  readonly nextEntry: ScrollbackEntry | undefined
}

export function ScrollbackLine({ entry, nextEntry }: ScrollbackLineProps) {
  if (entry.kind === 'user_line') {
    const gapBeforeAssistant = nextEntry?.kind === 'assistant_text'
    return (
      <Box flexDirection="column">
        <PastUserMessageBlock text={entry.text} />
        {gapBeforeAssistant ? <Box height={1} /> : null}
      </Box>
    )
  }
  return (
    <Box>
      <Text>{entry.text}</Text>
    </Box>
  )
}
