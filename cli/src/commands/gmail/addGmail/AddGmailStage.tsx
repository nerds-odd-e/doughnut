import { AsyncAssistantFetchStage } from '../AsyncAssistantFetchStage.js'
import { runAddGmailInteractiveAssistantMessage } from '../gmail.js'
import type { InteractiveSlashCommandStageProps } from '../../interactiveSlashCommand.js'

export const ADD_GMAIL_STAGE_STATUS_LABEL = 'Connecting Gmail…'

export function AddGmailStage({
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  return (
    <AsyncAssistantFetchStage
      spinnerLabel={ADD_GMAIL_STAGE_STATUS_LABEL}
      runAssistantMessage={runAddGmailInteractiveAssistantMessage}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}
