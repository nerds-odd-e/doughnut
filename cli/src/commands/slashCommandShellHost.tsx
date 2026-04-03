import { useCallback } from 'react'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import type { InteractiveRunSlashCommand } from './interactiveSlashCommandDispatch.js'
import { SlashCommandShellLiveColumn } from './slashCommandShellLiveColumn.js'
import type { PlainLineCommitScrollback } from './slashCommandShellPlainLineCommit.js'
import {
  type SlashCommandShellRunSuccessContext,
  useSlashCommandShellLiveColumnHandlers,
} from './useSlashCommandShellLiveColumnHandlers.js'

export function SlashCommandShellHost({
  onRunSuccess,
  slashCommands,
  placeholder,
  showMainPrompt,
  commitPlainLine,
}: {
  readonly onRunSuccess: (
    command: InteractiveRunSlashCommand,
    assistantMessage: string,
    ctx: SlashCommandShellRunSuccessContext
  ) => void
  readonly slashCommands: readonly InteractiveSlashCommand[]
  readonly placeholder: string
  readonly showMainPrompt: boolean
  readonly commitPlainLine: (
    line: string,
    scrollback: PlainLineCommitScrollback
  ) => void
}) {
  const {
    liveRegionCols,
    activeStage,
    stageArgumentRef,
    handleStageSettled,
    handleStageAbortWithError,
    onCommittedCommand,
    appendScrollbackError,
    appendScrollbackUserMessage,
  } = useSlashCommandShellLiveColumnHandlers({ onRunSuccess })

  const onCommittedLine = useCallback(
    (line: string) => {
      commitPlainLine(line, {
        appendScrollbackError,
        appendScrollbackUserMessage,
      })
    },
    [commitPlainLine, appendScrollbackError, appendScrollbackUserMessage]
  )

  return (
    <SlashCommandShellLiveColumn
      cols={liveRegionCols}
      activeStage={activeStage}
      stageArgumentRef={stageArgumentRef}
      onStageSettled={handleStageSettled}
      onStageAbortWithError={handleStageAbortWithError}
      showMainPrompt={showMainPrompt}
      slashCommands={slashCommands}
      placeholder={placeholder}
      onCommittedCommand={onCommittedCommand}
      onCommittedLine={onCommittedLine}
    />
  )
}
