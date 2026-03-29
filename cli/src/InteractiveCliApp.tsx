import { createElement, useCallback, useEffect, useState } from 'react'
import type { ComponentType } from 'react'
import { Box, Text, useApp } from 'ink'
import { DEFAULT_INTERACTIVE_GUIDANCE } from './interactiveGuidanceDefault.js'
import { MainInteractivePrompt } from './MainInteractivePrompt.js'
import { resolveInteractiveSlashCommand } from './commands/interactiveSlashCommands.js'
import type {
  InteractiveSlashCommandStageProps,
  TranscriptMessage,
} from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'

export function InteractiveCliApp() {
  const { exit } = useApp()
  const [messages, setMessages] = useState<TranscriptMessage[]>([
    { role: 'assistant', text: formatVersionOutput() },
  ])
  const [activeStageComponent, setActiveStageComponent] =
    useState<ComponentType<InteractiveSlashCommandStageProps> | null>(null)
  const [exitAfterCommit, setExitAfterCommit] = useState(false)
  const [currentGuidance, setCurrentGuidance] = useState(
    DEFAULT_INTERACTIVE_GUIDANCE
  )

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exitAfterCommit, exit])

  const handleAsyncSlashSettled = useCallback((assistantText: string) => {
    setMessages((prev) => [...prev, { role: 'assistant', text: assistantText }])
    setActiveStageComponent(null)
  }, [])

  const onCommittedLine = useCallback((line: string) => {
    const resolved = resolveInteractiveSlashCommand(line)
    if (resolved) {
      const { command, argument } = resolved
      if (
        command.argumentName !== undefined &&
        (argument === undefined || argument === '')
      ) {
        setMessages((prev) => [
          ...prev,
          { role: 'user', text: line },
          {
            role: 'assistant',
            text: `Missing ${command.argumentName}. Usage: ${command.doc.usage}`,
          },
        ])
        return
      }
      setMessages((prev) => [...prev, { role: 'user', text: line }])
      const Stage = command.stageComponent
      if (Stage) {
        // setState(fn) treats fn as updater; bare `Stage` would be called with prior state as props.
        setActiveStageComponent(() => Stage)
        return
      }
      Promise.resolve(command.run(argument))
        .then((r) => {
          setMessages((prev) => [
            ...prev,
            { role: 'assistant', text: r.assistantMessage },
          ])
          if (r.currentGuidance !== undefined) {
            setCurrentGuidance(r.currentGuidance)
          }
          if (line === '/exit') setExitAfterCommit(true)
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
      ) : exitAfterCommit ? null : (
        <Box flexDirection="column">
          <MainInteractivePrompt onCommittedLine={onCommittedLine} />
          <Text>{currentGuidance}</Text>
        </Box>
      )}
    </Box>
  )
}
