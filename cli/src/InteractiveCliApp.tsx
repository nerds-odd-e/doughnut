import { useCallback, useEffect, useRef, useState } from 'react'
import type { Key } from 'ink'
import { useApp, useInput } from 'ink'
import type { InteractiveRunSlashCommand } from './commands/interactiveSlashCommandDispatch.js'
import { interactiveSlashCommands } from './commands/interactiveSlashCommands.js'
import { commitMainInteractivePlainLine } from './commands/slashCommandShellPlainLineCommit.js'
import { SlashCommandShellHost } from './commands/slashCommandShellHost.js'
import type { SlashCommandShellRunSuccessContext } from './commands/useSlashCommandShellLiveColumnHandlers.js'
import type { StageKeyHandler } from './commonUIComponents/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from './commonUIComponents/stageKeyForwardContext.js'
import { InputHistoryProvider } from './inputHistory/index.js'
import { SessionScrollbackSessionProvider } from './sessionScrollback/sessionScrollbackAppendContext.js'

export function InteractiveCliApp() {
  return (
    <SessionScrollbackSessionProvider initialItems={[]}>
      <InputHistoryProvider>
        <InteractiveCliAppBody />
      </InputHistoryProvider>
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

  useEffect(() => {
    if (!exitAfterCommit) return
    exit()
  }, [exit, exitAfterCommit])

  const onRunSuccess = useCallback(
    (
      command: InteractiveRunSlashCommand,
      assistantMessage: string,
      {
        appendScrollbackAssistantTextMessage,
      }: SlashCommandShellRunSuccessContext
    ) => {
      appendScrollbackAssistantTextMessage(assistantMessage)
      if (command.literal === '/exit') setExitAfterCommit(true)
    },
    []
  )

  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <SlashCommandShellHost
        onRunSuccess={onRunSuccess}
        slashCommands={interactiveSlashCommands}
        placeholder="`exit` to quit."
        showMainPrompt={!exitAfterCommit}
        commitPlainLine={commitMainInteractivePlainLine}
      />
    </SetStageKeyHandlerContext.Provider>
  )
}
