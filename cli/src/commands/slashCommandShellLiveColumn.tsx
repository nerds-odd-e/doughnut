import { Fragment, type MutableRefObject } from 'react'
import { MainInteractivePrompt } from '../mainInteractivePrompt/index.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import type { ResolvedInteractiveSlashCommand } from './interactiveSlashCommands.js'
import { SlashCommandStageMount } from './slashCommandStageMount.js'
import type { ActiveSlashCommandStage } from './useSlashCommandShellState.js'

export function SlashCommandShellLiveColumn({
  cols,
  activeStage,
  stageArgumentRef,
  onStageSettled,
  onStageAbortWithError,
  showMainPrompt,
  slashCommands,
  placeholder,
  onCommittedCommand,
  onCommittedLine,
}: {
  readonly cols: number
  readonly activeStage: ActiveSlashCommandStage | null
  readonly stageArgumentRef: MutableRefObject<string | undefined>
  readonly onStageSettled: (assistantText: string) => void
  readonly onStageAbortWithError: (message: string) => void
  readonly showMainPrompt: boolean
  readonly slashCommands: readonly InteractiveSlashCommand[]
  readonly placeholder: string
  readonly onCommittedCommand: (
    resolved: ResolvedInteractiveSlashCommand
  ) => void
  readonly onCommittedLine: (line: string) => void
}) {
  return (
    <Fragment>
      {activeStage ? (
        <SlashCommandStageMount
          cols={cols}
          stageIndicator={activeStage.stageIndicator}
          Stage={activeStage.component}
          stageProps={{
            argument: stageArgumentRef.current,
            onSettled: onStageSettled,
            onAbortWithError: onStageAbortWithError,
          }}
        />
      ) : null}
      {showMainPrompt ? (
        <MainInteractivePrompt
          onCommittedCommand={onCommittedCommand}
          onCommittedLine={onCommittedLine}
          isActive={activeStage === null}
          slashCommands={slashCommands}
          placeholder={placeholder}
        />
      ) : null}
    </Fragment>
  )
}
