import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { addAccessToken } from './accessToken.js'

export function AddAccessTokenStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const token = argument!
  return (
    <AsyncAssistantFetchStage
      spinnerLabel="Verifying token…"
      runAssistantMessage={async (signal) => {
        await addAccessToken(token, signal)
        return 'Token added successfully'
      }}
      onSettled={onSettled}
    />
  )
}
