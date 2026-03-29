import { useCallback, useEffect, useState } from 'react'
import { Box, Text, useApp } from 'ink'
import { AddGmailStage } from './AddGmailStage.js'
import { LastEmailStage } from './LastEmailStage.js'
import { MainInteractivePrompt } from './MainInteractivePrompt.js'
import { interactiveSlashCommandByLine } from './commands/interactiveSlashCommands.js'
import type { TranscriptMessage } from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'

const ADD_GMAIL_LINE = '/add gmail'
const LAST_EMAIL_LINE = '/last email'
const EXIT_LINE = '/exit'

type InteractiveStage = 'main' | 'addGmail' | 'lastEmail'

export function InteractiveCliApp() {
  const { exit } = useApp()
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const [stage, setStage] = useState<InteractiveStage>('main')
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exitAfterCommit, exit])

  const handleAsyncSlashSettled = useCallback((assistantText: string) => {
    setMessages((prev) => [...prev, { role: 'assistant', text: assistantText }])
    setStage('main')
  }, [])

  const onCommittedLine = useCallback((line: string) => {
    if (line === ADD_GMAIL_LINE) {
      setMessages((prev) => [...prev, { role: 'user', text: line }])
      setStage('addGmail')
      return
    }
    if (line === LAST_EMAIL_LINE) {
      setMessages((prev) => [...prev, { role: 'user', text: line }])
      setStage('lastEmail')
      return
    }
    const command = interactiveSlashCommandByLine.get(line)
    if (command) {
      setMessages((prev) => [...prev, { role: 'user', text: line }])
      Promise.resolve(command.run())
        .then((r) => {
          setMessages((prev) => [
            ...prev,
            { role: 'assistant', text: r.assistantMessage },
          ])
          if (line === EXIT_LINE) setExitAfterCommit(true)
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
  }, [])

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
        <AddGmailStage onSettled={handleAsyncSlashSettled} />
      )}
      {stage === 'lastEmail' && (
        <LastEmailStage onSettled={handleAsyncSlashSettled} />
      )}
    </Box>
  )
}
