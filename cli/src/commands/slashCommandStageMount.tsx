import { createElement, type ComponentType } from 'react'
import { Box } from 'ink'
import type { InteractiveSlashCommandStageProps } from './interactiveSlashCommand.js'
import { StageLiveHeaderInk } from '../commonUIComponents/stageLiveHeaderInk.js'

export function SlashCommandStageMount({
  cols,
  stageIndicator,
  Stage,
  stageProps,
}: {
  readonly cols: number
  readonly stageIndicator?: string
  readonly Stage: ComponentType<InteractiveSlashCommandStageProps>
  readonly stageProps: InteractiveSlashCommandStageProps
}) {
  return (
    <Box flexDirection="column">
      {stageIndicator !== undefined ? (
        <StageLiveHeaderInk title={stageIndicator} cols={cols} />
      ) : null}
      {createElement(Stage, stageProps)}
    </Box>
  )
}
