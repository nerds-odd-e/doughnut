import { AsyncAssistantFetchStage } from './AsyncAssistantFetchStage.js'
import { runAddGmailInteractiveAssistantMessage } from './commands/addGmailSlashCommand.js'

export const ADD_GMAIL_STAGE_STATUS_LABEL = 'Connecting Gmail…'

export function AddGmailStage({
  onSettled,
}: {
  readonly onSettled: (assistantText: string) => void
}) {
  return (
    <AsyncAssistantFetchStage
      spinnerLabel={ADD_GMAIL_STAGE_STATUS_LABEL}
      runAssistantMessage={runAddGmailInteractiveAssistantMessage}
      onSettled={onSettled}
    />
  )
}
