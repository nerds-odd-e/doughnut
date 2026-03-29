import { AsyncAssistantFetchStage } from './AsyncAssistantFetchStage.js'
import { runLastEmailInteractiveAssistantMessage } from './commands/gmail.js'

export const LAST_EMAIL_STAGE_STATUS_LABEL = 'Loading last email…'

export function LastEmailStage({
  onSettled,
}: {
  readonly onSettled: (assistantText: string) => void
}) {
  return (
    <AsyncAssistantFetchStage
      spinnerLabel={LAST_EMAIL_STAGE_STATUS_LABEL}
      runAssistantMessage={runLastEmailInteractiveAssistantMessage}
      onSettled={onSettled}
    />
  )
}
