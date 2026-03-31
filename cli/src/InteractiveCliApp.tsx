import {
  createElement,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ComponentProps,
} from 'react'
import type { ComponentType } from 'react'
import type { Key } from 'ink'
import { Box, useApp, useInput } from 'ink'
import { MainInteractivePrompt } from './mainInteractivePrompt/index.js'
import { resolveInteractiveSlashCommand } from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommandStageProps } from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'
import type { StageKeyHandler } from './commands/accessToken/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commands/accessToken/stageKeyForwardContext.js'
import {
  InteractiveCliScrollback,
  transcriptAssistantText,
  transcriptUserLine,
} from './sessionScrollback/interactiveCliTranscript.js'

type InteractiveCliScrollbackItem = ComponentProps<
  typeof InteractiveCliScrollback
>['items'][number]

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
  const [scrollbackItems, setScrollbackItems] = useState<
    InteractiveCliScrollbackItem[]
  >(() => [transcriptAssistantText(formatVersionOutput())])
  const [activeStageComponent, setActiveStageComponent] =
    useState<ComponentType<InteractiveSlashCommandStageProps> | null>(null)
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const handleAsyncSlashSettled = useCallback((assistantText: string) => {
    const assistant = transcriptAssistantText(assistantText)
    setScrollbackItems((prev) => [...prev, assistant])
    setActiveStageComponent(null)
    stageArgumentRef.current = undefined
  }, [])

  const onCommittedLine = useCallback((line: string) => {
    const commitUserLineWithAssistant = (assistantText: string) => {
      const user = transcriptUserLine(line)
      const assistant = transcriptAssistantText(assistantText)
      setScrollbackItems((prev) => [...prev, user, assistant])
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
    const user = transcriptUserLine(line)
    setScrollbackItems((prev) => [...prev, user])
    const Stage = command.stageComponent
    if (Stage) {
      const argumentMissing = argument === undefined || argument === ''
      const argSpec = command.argument
      if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
        const assistant = transcriptAssistantText(
          `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
        )
        setScrollbackItems((prev) => [...prev, assistant])
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
      const assistant = transcriptAssistantText(
        `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
      )
      setScrollbackItems((prev) => [...prev, assistant])
      return
    }
    const run = command.run
    if (!run) {
      const assistant = transcriptAssistantText(
        'Internal error: command has no run handler.'
      )
      setScrollbackItems((prev) => [...prev, assistant])
      return
    }
    Promise.resolve(run(argument))
      .then((r) => {
        const assistant = transcriptAssistantText(r.assistantMessage)
        setScrollbackItems((prev) => [...prev, assistant])
        if (command.line === '/exit') setExitAfterCommit(true)
      })
      .catch((err: unknown) => {
        const assistant = transcriptAssistantText(
          userVisibleSlashCommandError(err)
        )
        setScrollbackItems((prev) => [...prev, assistant])
      })
  }, [])

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <Box flexDirection="column">
        <InteractiveCliScrollback items={scrollbackItems} />
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
