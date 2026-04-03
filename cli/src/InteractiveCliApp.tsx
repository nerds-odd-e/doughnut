import { useCallback, useEffect, useRef, useState } from 'react'
import type { Key } from 'ink'
import { useApp, useInput, useStdout } from 'ink'
import {
  interactiveSlashCommands,
  type ResolvedInteractiveSlashCommand,
} from './commands/interactiveSlashCommands.js'
import { applyResolvedInteractiveSlashCommand } from './commands/interactiveSlashCommandDispatch.js'
import { SlashCommandShellLiveColumn } from './commands/slashCommandShellLiveColumn.js'
import { useSlashCommandShellState } from './commands/useSlashCommandShellState.js'
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
  const setStageKeyHandler = useCallback((handler: StageKeyHandler | null) => {
    stageKeyHandlerRef.current = handler
  }, [])

  useInput(
    useCallback((input: string, key: Key) => {
      stageKeyHandlerRef.current?.(input, key)
    }, [])
  )
  const {
    activeStage,
    stageArgumentRef,
    handleStageSettled,
    handleStageAbortWithError,
    openStage,
    setStageArgumentRef,
  } = useSlashCommandShellState(
    appendScrollbackAssistantTextMessage,
    appendScrollbackError
  )
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      appendScrollbackUserMessage(resolved.line)
      applyResolvedInteractiveSlashCommand(resolved, {
        appendScrollbackError,
        setStageArgumentRef,
        openStage,
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
      openStage,
      setStageArgumentRef,
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
      <SlashCommandShellLiveColumn
        cols={liveRegionCols}
        activeStage={activeStage}
        stageArgumentRef={stageArgumentRef}
        onStageSettled={handleStageSettled}
        onStageAbortWithError={handleStageAbortWithError}
        showMainPrompt={!exitAfterCommit}
        mainPromptIsActive={!activeStage}
        slashCommands={interactiveSlashCommands}
        placeholder="`exit` to quit."
        onCommittedCommand={onCommittedCommand}
        onCommittedLine={onCommittedLine}
      />
    </SetStageKeyHandlerContext.Provider>
  )
}
