import { Box, Text } from 'ink'
import { PastUserMessageBlock } from '../pastUserMessageBlock.js'
import type { RecallAnsweredItem } from '../commands/recall/recallAnsweredScrollback.js'
import { RecallAnsweredRow } from '../commands/recall/recallAnsweredScrollback.js'
import type { TranscriptItem } from './interactiveCliTranscript.js'
import { SessionScrollback } from './SessionScrollback.js'

export type InteractiveScrollbackItem = TranscriptItem | RecallAnsweredItem

function ScrollbackRow({
  entry,
  nextEntry,
}: {
  readonly entry: InteractiveScrollbackItem
  readonly nextEntry: InteractiveScrollbackItem | undefined
}) {
  if (entry.kind === 'user_line') {
    const gapBeforeFollowing =
      nextEntry?.kind === 'assistant_text' ||
      nextEntry?.kind === 'recall_answered'
    return (
      <Box flexDirection="column">
        <PastUserMessageBlock text={entry.text} />
        {gapBeforeFollowing ? <Box height={1} /> : null}
      </Box>
    )
  }
  if (entry.kind === 'assistant_text') {
    return (
      <Box>
        <Text>{entry.text}</Text>
      </Box>
    )
  }
  return <RecallAnsweredRow text={entry.text} />
}

export function InteractiveSessionScrollback(props: {
  readonly items: readonly InteractiveScrollbackItem[]
}) {
  const { items } = props
  return (
    <SessionScrollback items={items}>
      {(item, index) => (
        <ScrollbackRow
          key={item.id}
          entry={item}
          nextEntry={items[index + 1]}
        />
      )}
    </SessionScrollback>
  )
}
