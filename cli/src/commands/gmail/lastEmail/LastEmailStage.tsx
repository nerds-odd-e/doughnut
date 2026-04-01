import { AsyncAssistantFetchStage } from '../AsyncAssistantFetchStage.js'
import { runLastEmailInteractiveAssistantMessage } from '../gmail.js'
import type { InteractiveSlashCommandStageProps } from '../../interactiveSlashCommand.js'

export const LAST_EMAIL_STAGE_STATUS_LABEL = 'Loading last email…'

export function LastEmailStage({
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  return (
    <AsyncAssistantFetchStage
      spinnerLabel={LAST_EMAIL_STAGE_STATUS_LABEL}
      runAssistantMessage={runLastEmailInteractiveAssistantMessage}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}
