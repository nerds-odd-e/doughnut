import { useCallback, useEffect, useRef, useState } from 'react'
import type { Key } from 'ink'
import { useApp, useInput } from 'ink'
import { interactiveSlashCommands } from './commands/interactiveSlashCommands.js'
import { commitMainInteractivePlainLine } from './commands/slashCommandShellPlainLineCommit.js'
import { SlashCommandShellLiveColumn } from './commands/slashCommandShellLiveColumn.js'
import { useSlashCommandShellLiveColumnHandlers } from './commands/useSlashCommandShellLiveColumnHandlers.js'
import type { StageKeyHandler } from './commonUIComponents/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commonUIComponents/stageKeyForwardContext.js'
import { SessionScrollbackSessionProvider } from './sessionScrollback/sessionScrollbackAppendContext.js'

export function InteractiveCliApp() {
  return (
    <SessionScrollbackSessionProvider initialItems={[]}>
      <InteractiveCliAppBody />
    </SessionScrollbackSessionProvider>
  )
}

function InteractiveCliAppBody() {
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
  const [exitAfterCommit, setExitAfterCommit] = useState(false)

  const {
    liveRegionCols,
    activeStage,
    stageArgumentRef,
    handleStageSettled,
    handleStageAbortWithError,
    onCommittedCommand,
    appendScrollbackError,
    appendScrollbackUserMessage,
  } = useSlashCommandShellLiveColumnHandlers({
    onRunSuccess: useCallback(
      (command, assistantMessage, { appendScrollbackAssistantTextMessage }) => {
        appendScrollbackAssistantTextMessage(assistantMessage)
        if (command.literal === '/exit') setExitAfterCommit(true)
      },
      []
    ),
  })

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const onCommittedLine = useCallback(
    (line: string) => {
      commitMainInteractivePlainLine(line, {
        appendScrollbackError,
        appendScrollbackUserMessage,
      })
    },
    [appendScrollbackError, appendScrollbackUserMessage]
  )

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <SlashCommandShellLiveColumn
        cols={liveRegionCols}
        activeStage={activeStage}
        stageArgumentRef={stageArgumentRef}
        onStageSettled={handleStageSettled}
        onStageAbortWithError={handleStageAbortWithError}
        showMainPrompt={!exitAfterCommit}
        slashCommands={interactiveSlashCommands}
        placeholder="`exit` to quit."
        onCommittedCommand={onCommittedCommand}
        onCommittedLine={onCommittedLine}
      />
    </SetStageKeyHandlerContext.Provider>
  )
}
