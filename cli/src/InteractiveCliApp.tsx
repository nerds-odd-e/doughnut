import { useCallback, useMemo, useState } from 'react'
import { Box, Text, useApp } from 'ink'
import { AddGmailStage } from './AddGmailStage.js'
import { MainInteractivePrompt } from './MainInteractivePrompt.js'
import { createInteractiveSlashCommands } from './commands/interactiveSlashCommands.js'
import type {
  InteractiveSlashCommand,
  TranscriptMessage,
} from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'

const ADD_GMAIL_LINE = '/add gmail'

type InteractiveStage = 'main' | 'addGmail'

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
  const [stage, setStage] = useState<InteractiveStage>('main')

  const handleAddGmailSettled = useCallback((assistantText: string) => {
    setMessages((prev) => [...prev, { role: 'assistant', text: assistantText }])
    setStage('main')
  }, [])

  const onCommittedLine = useCallback(
    (line: string) => {
      if (line === ADD_GMAIL_LINE) {
        setMessages((prev) => [...prev, { role: 'user', text: line }])
        setStage('addGmail')
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
    },
    [slashByLine]
  )

  return (
    <Box flexDirection="column">
      {messages.map((item, index) =>
        item.role === 'user' ? (
          <PastUserMessageBlock key={index} text={item.text} />
        ) : (
          <Text key={index}>{item.text}</Text>
        )
      )}
      {stage === 'main' && (
        <MainInteractivePrompt onCommittedLine={onCommittedLine} />
      )}
      {stage === 'addGmail' && (
        <AddGmailStage onSettled={handleAddGmailSettled} />
      )}
    </Box>
  )
}
