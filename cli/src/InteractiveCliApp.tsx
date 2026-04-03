import { useCallback, useEffect, useRef, useState } from 'react'
import type { ComponentType } from 'react'
import type { Key } from 'ink'
import { useApp, useInput, useStdout } from 'ink'
import { MainInteractivePrompt } from './mainInteractivePrompt/index.js'
import {
  interactiveSlashCommands,
  type ResolvedInteractiveSlashCommand,
} from './commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommandStageProps } from './commands/interactiveSlashCommand.js'
import { applyResolvedInteractiveSlashCommand } from './commands/interactiveSlashCommandDispatch.js'
import { SlashCommandStageMount } from './commands/slashCommandStageMount.js'
import type { StageKeyHandler } from './commonUIComponents/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commonUIComponents/stageKeyForwardContext.js'
import {
  SessionScrollbackSessionProvider,
  useSessionScrollbackAppend,
} from './sessionScrollback/sessionScrollbackAppendContext.js'
import { inkTerminalColumns } from './terminalColumns.js'

export function InteractiveCliApp() {
  return (
    <SessionScrollbackSessionProvider initialItems={[]}>
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
      appendScrollbackUserMessage(resolved.line)
      applyResolvedInteractiveSlashCommand(resolved, {
        appendScrollbackError,
        setStageArgumentRef: (arg) => {
          stageArgumentRef.current = arg
        },
        openStage: ({ component, stageIndicator }) => {
          setActiveSlashStage(() => ({
            component,
            stageIndicator,
          }))
        },
        onRunSuccess: (command, assistantMessage) => {
          appendScrollbackAssistantTextMessage(assistantMessage)
          if (command.literal === '/exit') setExitAfterCommit(true)
        },
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
      {activeSlashStage ? (
        <SlashCommandStageMount
          cols={liveRegionCols}
          stageIndicator={activeStageIndicator}
          Stage={activeSlashStage.component}
          stageProps={{
            argument: stageArgumentRef.current,
            onSettled: handleAsyncSlashSettled,
            onAbortWithError: handleAsyncSlashAbortWithError,
          }}
        />
      ) : null}
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
