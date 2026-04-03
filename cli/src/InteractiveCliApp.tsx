import {
  createElement,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import type { ComponentType } from 'react'
import type { Key } from 'ink'
import { Box, useApp, useInput, useStdout } from 'ink'
import { MainInteractivePrompt } from './mainInteractivePrompt/index.js'
import {
  interactiveSlashCommands,
  type ResolvedInteractiveSlashCommand,
} from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommandStageProps } from './commands/interactiveSlashCommand.js'
import { formatVersionOutput } from './commands/version.js'
import { userVisibleSlashCommandError } from './userVisibleSlashCommandError.js'
import type { StageKeyHandler } from './commonUIComponents/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commonUIComponents/stageKeyForwardContext.js'
import { scrollbackAssistantTextMessageItem } from './sessionScrollback/interactiveCliTranscript.js'
import {
  SessionScrollbackSessionProvider,
  useSessionScrollbackAppend,
} from './sessionScrollback/sessionScrollbackAppendContext.js'
import { StageLiveHeaderInk } from './commonUIComponents/stageLiveHeaderInk.js'
import { inkTerminalColumns } from './terminalColumns.js'

export function InteractiveCliApp() {
  const initialScrollbackItems = useMemo(
    () => [scrollbackAssistantTextMessageItem(formatVersionOutput())],
    []
  )
  return (
    <SessionScrollbackSessionProvider initialItems={initialScrollbackItems}>
      <InteractiveCliAppBody />
    </SessionScrollbackSessionProvider>
  )
}

function InteractiveCliAppBody() {
  const {
    appendScrollbackAssistantTextMessage,
    appendScrollbackError,
    appendScrollbackUserMessage,
  } = useSessionScrollbackAppend()
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
  const [activeSlashStage, setActiveSlashStage] = useState<{
    component: ComponentType<InteractiveSlashCommandStageProps>
    stageIndicator?: string
  } | null>(null)
  const activeStageIndicator = activeSlashStage?.stageIndicator
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const clearSlashStage = useCallback(() => {
    setActiveSlashStage(null)
    stageArgumentRef.current = undefined
  }, [])

  const handleAsyncSlashSettled = useCallback(
    (assistantText: string) => {
      if (assistantText !== '') {
        appendScrollbackAssistantTextMessage(assistantText)
      }
      clearSlashStage()
    },
    [appendScrollbackAssistantTextMessage, clearSlashStage]
  )

  const handleAsyncSlashAbortWithError = useCallback(
    (message: string) => {
      if (message !== '') {
        appendScrollbackError(message)
      }
      clearSlashStage()
    },
    [appendScrollbackError, clearSlashStage]
  )

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      const { command, argument, line } = resolved
      appendScrollbackUserMessage(line)
      if ('stageComponent' in command) {
        const argumentMissing = argument === undefined || argument === ''
        const argSpec = command.argument
        if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
          appendScrollbackError(
            `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
          )
          return
        }
        stageArgumentRef.current = argument
        const Stage = command.stageComponent
        const indicator = command.stageIndicator
        // setState(fn) treats fn as updater; bare `Stage` would be called with prior state as props.
        setActiveSlashStage(() => ({
          component: Stage,
          stageIndicator:
            indicator !== undefined && indicator !== '' ? indicator : undefined,
        }))
        return
      }
      const argumentMissing = argument === undefined || argument === ''
      const argSpec = command.argument
      if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
        appendScrollbackError(
          `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
        )
        return
      }
      Promise.resolve(command.run(argument))
        .then((r) => {
          appendScrollbackAssistantTextMessage(r.assistantMessage)
          if (command.literal === '/exit') setExitAfterCommit(true)
        })
        .catch((err: unknown) => {
          appendScrollbackError(userVisibleSlashCommandError(err))
        })
    },
    [
      appendScrollbackAssistantTextMessage,
      appendScrollbackError,
      appendScrollbackUserMessage,
    ]
  )

  const onCommittedLine = useCallback(
    (line: string) => {
      const assistantText = line.startsWith('/')
        ? 'unsupported command'
        : 'Not supported'
      appendScrollbackUserMessage(line)
      appendScrollbackError(assistantText)
    },
    [appendScrollbackError, appendScrollbackUserMessage]
  )

  const { stdout } = useStdout()
  const liveRegionCols = inkTerminalColumns(stdout.columns)

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      {activeSlashStage && (
        <Box flexDirection="column">
          {activeStageIndicator !== undefined ? (
            <StageLiveHeaderInk
              title={activeStageIndicator}
              cols={liveRegionCols}
            />
          ) : null}
          {createElement(activeSlashStage.component, {
            argument: stageArgumentRef.current,
            onSettled: handleAsyncSlashSettled,
            onAbortWithError: handleAsyncSlashAbortWithError,
          })}
        </Box>
      )}
      {!exitAfterCommit && (
        <MainInteractivePrompt
          onCommittedCommand={onCommittedCommand}
          onCommittedLine={onCommittedLine}
          isActive={!activeSlashStage}
          slashCommands={interactiveSlashCommands}
          placeholder="`exit` to quit."
        />
      )}
    </SetStageKeyHandlerContext.Provider>
  )
}
