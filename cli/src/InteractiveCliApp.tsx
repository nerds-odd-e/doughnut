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
import {
  transcriptAssistantError,
  transcriptAssistantText,
  transcriptUserLine,
} from './sessionScrollback/interactiveCliTranscript.js'
import {
  SessionScrollbackSessionProvider,
  useSessionScrollbackAppend,
} from './sessionScrollback/sessionScrollbackAppendContext.js'
import { StageLiveHeaderInk } from './commonUIComponents/stageLiveHeaderInk.js'
import { inkTerminalColumns } from './terminalColumns.js'

export function InteractiveCliApp() {
  const initialScrollbackItems = useMemo(
    () => [transcriptAssistantText(formatVersionOutput())],
    []
  )
  return (
    <SessionScrollbackSessionProvider initialItems={initialScrollbackItems}>
      <InteractiveCliAppBody />
    </SessionScrollbackSessionProvider>
  )
}

function InteractiveCliAppBody() {
  const { appendScrollbackItem, appendScrollbackItems } =
    useSessionScrollbackAppend()
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
        appendScrollbackItem(transcriptAssistantText(assistantText))
      }
      clearSlashStage()
    },
    [appendScrollbackItem, clearSlashStage]
  )

  const handleAsyncSlashAbortWithError = useCallback(
    (message: string) => {
      if (message !== '') {
        appendScrollbackItem(transcriptAssistantError(message))
      }
      clearSlashStage()
    },
    [appendScrollbackItem, clearSlashStage]
  )

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      const { command, argument, line } = resolved
      const user = transcriptUserLine(line)
      if ('stageComponent' in command) {
        const argumentMissing = argument === undefined || argument === ''
        const argSpec = command.argument
        if (argSpec !== undefined && argumentMissing && !argSpec.optional) {
          const assistant = transcriptAssistantError(
            `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
          )
          appendScrollbackItems([user, assistant])
          return
        }
        appendScrollbackItem(user)
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
        const assistant = transcriptAssistantError(
          `Missing ${argSpec.name}. Usage: ${command.doc.usage}`
        )
        appendScrollbackItems([user, assistant])
        return
      }
      appendScrollbackItem(user)
      Promise.resolve(command.run(argument))
        .then((r) => {
          const assistant = transcriptAssistantText(r.assistantMessage)
          appendScrollbackItem(assistant)
          if (command.literal === '/exit') setExitAfterCommit(true)
        })
        .catch((err: unknown) => {
          const assistant = transcriptAssistantError(
            userVisibleSlashCommandError(err)
          )
          appendScrollbackItem(assistant)
        })
    },
    [appendScrollbackItem, appendScrollbackItems]
  )

  const commitUserLineWithAssistant = useCallback(
    (userLine: string, assistantText: string, isError = false) => {
      const user = transcriptUserLine(userLine)
      const assistant = isError
        ? transcriptAssistantError(assistantText)
        : transcriptAssistantText(assistantText)
      appendScrollbackItems([user, assistant])
    },
    [appendScrollbackItems]
  )

  const onCommittedLine = useCallback(
    (line: string) => {
      if (line.startsWith('/')) {
        commitUserLineWithAssistant(line, 'unsupported command', true)
        return
      }
      commitUserLineWithAssistant(line, 'Not supported', true)
    },
    [commitUserLineWithAssistant]
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
