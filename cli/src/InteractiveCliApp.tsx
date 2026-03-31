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
import type { StageKeyHandler } from './commands/accessToken/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commands/accessToken/stageKeyForwardContext.js'

export function InteractiveCliApp() {
  const { exit } = useApp()
  const stageKeyHandlerRef = useRef<StageKeyHandler | null>(null)
  const stageArgumentRef = useRef<string | undefined>(undefined)
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
  }, [exit, exitAfterCommit])

  const handleAsyncSlashSettled = useCallback((assistantText: string) => {
    setMessages((prev) => [...prev, { role: 'assistant', text: assistantText }])
    setActiveStageComponent(null)
    stageArgumentRef.current = undefined
  }, [])

  const onCommittedLine = useCallback((line: string) => {
    const commitUserLineWithAssistant = (assistantText: string) => {
      setMessages((prev) => [
        ...prev,
        { role: 'user', text: line },
        { role: 'assistant', text: assistantText },
      ])
    }

    const lineOfCommand = line.startsWith('/')
      ? line.slice(1)
      : line.trim() === 'exit'
        ? 'exit'
        : undefined

    if (!lineOfCommand) {
      commitUserLineWithAssistant('Not supported')
      return
    }

    const resolved = resolveInteractiveSlashCommand(lineOfCommand)
    if (!resolved) {
      commitUserLineWithAssistant('unsupported command')
      return
    }

    const { command, argument } = resolved
    setMessages((prev) => [...prev, { role: 'user', text: line }])
    const Stage = command.stageComponent
    if (Stage) {
      const argumentMissing = argument === undefined || argument === ''
      const argSpec = command.argument
      if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
        setMessages((prev) => [
          ...prev,
          {
            role: 'assistant',
            text: `Missing ${argSpec.name}. Usage: ${command.doc.usage}`,
          },
        ])
        return
      }
      stageArgumentRef.current = argument
      // setState(fn) treats fn as updater; bare `Stage` would be called with prior state as props.
      setActiveStageComponent(() => Stage)
      return
    }
    const argumentMissing = argument === undefined || argument === ''
    const argSpec = command.argument
    if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: `Missing ${argSpec.name}. Usage: ${command.doc.usage}`,
        },
      ])
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
  }, [])

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <Box flexDirection="column">
        {messages.flatMap((item, index) =>
          item.role === 'user'
            ? [
                <PastUserMessageBlock key={index} text={item.text} />,
                ...(messages[index + 1]?.role === 'assistant'
                  ? [<Box key={`${index}-gap`} height={1} />]
                  : []),
              ]
            : [<Text key={index}>{item.text}</Text>]
        )}
        {activeStageComponent &&
          createElement(activeStageComponent, {
            argument: stageArgumentRef.current,
            onSettled: handleAsyncSlashSettled,
          })}
        {!exitAfterCommit && (
          <MainInteractivePrompt
            onCommittedLine={onCommittedLine}
            isActive={!activeStageComponent}
          />
        )}
      </Box>
    </SetStageKeyHandlerContext.Provider>
  )
}
