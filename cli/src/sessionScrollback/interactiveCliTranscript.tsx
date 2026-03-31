import { Box, Text } from 'ink'
import { PastUserMessageBlock } from '../pastUserMessageBlock.js'
import { SessionScrollback } from './SessionScrollback.js'

type TranscriptItem =
  | { readonly kind: 'user_line'; readonly id: string; readonly text: string }
  | {
      readonly kind: 'assistant_text'
      readonly id: string
      readonly text: string
    }

export function transcriptUserLine(text: string): TranscriptItem {
  return { kind: 'user_line', id: crypto.randomUUID(), text }
}

export function transcriptAssistantText(text: string): TranscriptItem {
  return { kind: 'assistant_text', id: crypto.randomUUID(), text }
}

function TranscriptItemView({
  entry,
  nextEntry,
}: {
  readonly entry: TranscriptItem
  readonly nextEntry: TranscriptItem | undefined
}) {
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

export function InteractiveCliScrollback(props: {
  readonly items: readonly TranscriptItem[]
}) {
  const { items } = props
  return (
    <SessionScrollback items={items}>
      {(entry, index) => (
        <TranscriptItemView
          key={entry.id}
          entry={entry}
          nextEntry={items[index + 1]}
        />
      )}
    </SessionScrollback>
  )
}
