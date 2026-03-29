import { createElement, useCallback, useEffect, useState } from 'react'
import type { ComponentType } from 'react'
import { Box, Text, useApp } from 'ink'
import { MainInteractivePrompt } from './MainInteractivePrompt.js'
import { interactiveSlashCommandByLine } from './commands/interactiveSlashCommands.js'
import type {
  InteractiveSlashCommandStageProps,
  TranscriptMessage,
} from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'

const EXIT_LINE = '/exit'

export function InteractiveCliApp() {
  const { exit } = useApp()
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const [activeStageComponent, setActiveStageComponent] =
    useState<ComponentType<InteractiveSlashCommandStageProps> | null>(null)
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exitAfterCommit, exit])

  const handleAsyncSlashSettled = useCallback((assistantText: string) => {
    setMessages((prev) => [...prev, { role: 'assistant', text: assistantText }])
    setActiveStageComponent(null)
  }, [])

  const onCommittedLine = useCallback((line: string) => {
    const command = interactiveSlashCommandByLine.get(line)
    if (command) {
      setMessages((prev) => [...prev, { role: 'user', text: line }])
      const Stage = command.stageComponent
      if (Stage) {
        // setState(fn) treats fn as updater; bare `Stage` would be called with prior state as props.
        setActiveStageComponent(() => Stage)
        return
      }
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
      {activeStageComponent ? (
        createElement(activeStageComponent, {
          onSettled: handleAsyncSlashSettled,
        })
      ) : (
        <MainInteractivePrompt onCommittedLine={onCommittedLine} />
      )}
    </Box>
  )
}
