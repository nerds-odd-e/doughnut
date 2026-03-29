import { useMemo, useState } from 'react'
import { Box, Text, useApp, useInput } from 'ink'
import { createInteractiveSlashCommands } from './commands/interactiveSlashCommands.js'
import type {
  InteractiveSlashCommand,
  TranscriptMessage,
} from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { useInteractiveCliLineBuffer } from './interactiveCliInput.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'

function userVisibleSlashCommandError(err: unknown): string {
  if (
    (typeof DOMException !== 'undefined' &&
      err instanceof DOMException &&
      err.name === 'AbortError') ||
    (err instanceof Error && err.name === 'AbortError')
  ) {
    return 'Cancelled.'
  }
  if (err instanceof Error) return err.message
  return String(err)
}

export function InteractiveCliApp() {
  const { exit } = useApp()
  const slashCommands: InteractiveSlashCommand[] = useMemo(
    () => createInteractiveSlashCommands(exit),
    [exit]
  )
  const slashByLine = useMemo(
    () => new Map(slashCommands.map((c) => [c.line, c])),
    [slashCommands]
  )
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const { buffer, applyInput } = useInteractiveCliLineBuffer()

  useInput((input, key) => {
    applyInput(input, key, (line) => {
      if (line === '') {
        return
      }
      const command = slashByLine.get(line)
      if (command) {
        setMessages((prev) => [...prev, { role: 'user', text: line }])
        Promise.resolve(command.run())
          .then((r) => {
            setMessages((prev) => [
              ...prev,
              { role: 'assistant', text: r.assistantMessage },
            ])
          })
          .catch((err: unknown) => {
            setMessages((prev) => [
              ...prev,
              {
                role: 'assistant',
                text: userVisibleSlashCommandError(err),
              },
            ])
          })
        return
      }
      setMessages((prev) => [
        ...prev,
        { role: 'user', text: line },
        { role: 'assistant', text: 'Not supported' },
      ])
    })
  })

  return (
    <Box flexDirection="column">
      {messages.map((item, index) =>
        item.role === 'user' ? (
          <PastUserMessageBlock key={index} text={item.text} />
        ) : (
          <Text key={index}>{item.text}</Text>
        )
      )}
      <Text>
        {'> '}
        {buffer}
        <Text inverse> </Text>
      </Text>
    </Box>
  )
}
