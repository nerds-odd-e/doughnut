import { useMemo, useState } from 'react'
import { Box, Text, useApp, useInput } from 'ink'
import { helpInteractiveSlashCommand } from './commands/help.js'
import { createExitCommand } from './commands/exit.js'
import type { InteractiveSlashCommand } from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { useInteractiveCliLineBuffer } from './interactiveCliInput.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'

type TranscriptMessage =
  | { readonly role: 'assistant'; readonly text: string }
  | { readonly role: 'user'; readonly text: string }

export function InteractiveCliApp() {
  const { exit } = useApp()
  const slashCommands: InteractiveSlashCommand[] = useMemo(
    () => [helpInteractiveSlashCommand, createExitCommand(exit)],
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
      const command = slashByLine.get(line)
      if (command) {
        const { assistantMessage } = command.run()
        setMessages((prev) => [
          ...prev,
          { role: 'user', text: line },
          { role: 'assistant', text: assistantMessage },
        ])
        return
      }
      if (line === '') {
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
