import { createElement, useCallback, useEffect, useRef, useState } from 'react'
import type { ComponentType } from 'react'
import type { Key } from 'ink'
import { Box, Text, useApp, useInput } from 'ink'
import { MainInteractivePrompt } from './mainInteractivePrompt/index.js'
import { resolveInteractiveSlashCommand } from './commands/interactiveSlashCommands.js'
import type {
  InteractiveSlashCommandStageProps,
  TranscriptMessage,
} from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { PastUserMessageBlock } from './pastUserMessageBlock.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'
import type { StageKeyHandler } from './stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './stageKeyForwardContext.js'

export function InteractiveCliApp() {
  const { exit } = useApp()
  const stageKeyHandlerRef = useRef<StageKeyHandler | null>(null)
  const setStageKeyHandler = useCallback((handler: StageKeyHandler | null) => {
    stageKeyHandlerRef.current = handler
  }, [])

  useInput(
    useCallback((input: string, key: Key) => {
      stageKeyHandlerRef.current?.(input, key)
    }, [])
  )
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
      const run = command.run
      if (!run) {
        setMessages((prev) => [
          ...prev,
          {
            role: 'assistant',
            text: 'Internal error: command has no run handler.',
          },
        ])
        return
      }
      Promise.resolve(run(argument))
        .then((r) => {
          setMessages((prev) => [
            ...prev,
            { role: 'assistant', text: r.assistantMessage },
          ])
          if (command.line === '/exit') setExitAfterCommit(true)
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
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
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
          <MainInteractivePrompt onCommittedLine={onCommittedLine} />
        )}
      </Box>
    </SetStageKeyHandlerContext.Provider>
  )
}
