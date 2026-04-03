import { useCallback } from 'react'
import { useStdout } from 'ink'
import { useSessionScrollbackAppend } from '../sessionScrollback/sessionScrollbackAppendContext.js'
import { inkTerminalColumns } from '../terminalColumns.js'
import {
  applyResolvedInteractiveSlashCommand,
  type InteractiveRunSlashCommand,
} from './interactiveSlashCommandDispatch.js'
import type { ResolvedInteractiveSlashCommand } from './interactiveSlashCommands.js'
import { useSlashCommandShellState } from './useSlashCommandShellState.js'

export type SlashCommandShellRunSuccessContext = {
  readonly appendScrollbackAssistantTextMessage: (message: string) => void
}

export function useSlashCommandShellLiveColumnHandlers({
  onRunSuccess,
}: {
  readonly onRunSuccess: (
    command: InteractiveRunSlashCommand,
    assistantMessage: string,
    ctx: SlashCommandShellRunSuccessContext
  ) => void
}) {
  const {
    appendScrollbackAssistantTextMessage,
    appendScrollbackError,
    appendScrollbackUserMessage,
  } = useSessionScrollbackAppend()
  const { stdout } = useStdout()
  const liveRegionCols = inkTerminalColumns(stdout.columns)

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

  const dispatchRunSuccess = useCallback(
    (command: InteractiveRunSlashCommand, assistantMessage: string) => {
      onRunSuccess(command, assistantMessage, {
        appendScrollbackAssistantTextMessage,
      })
    },
    [appendScrollbackAssistantTextMessage, onRunSuccess]
  )

  const onCommittedCommand = useCallback(
    (resolved: ResolvedInteractiveSlashCommand) => {
      appendScrollbackUserMessage(resolved.line)
      applyResolvedInteractiveSlashCommand(resolved, {
        appendScrollbackError,
        setStageArgumentRef,
        openStage,
        onRunSuccess: dispatchRunSuccess,
      })
    },
    [
      appendScrollbackError,
      appendScrollbackUserMessage,
      dispatchRunSuccess,
      openStage,
      setStageArgumentRef,
    ]
  )

  return {
    liveRegionCols,
    activeStage,
    stageArgumentRef,
    handleStageSettled,
    handleStageAbortWithError,
    onCommittedCommand,
    appendScrollbackAssistantTextMessage,
    appendScrollbackError,
    appendScrollbackUserMessage,
  }
}
