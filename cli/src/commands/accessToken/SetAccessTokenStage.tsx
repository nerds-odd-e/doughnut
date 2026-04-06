import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { setAccessToken } from './accessToken.js'

export function SetAccessTokenStage({
  argument,
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  const token = argument!
  return (
    <AsyncAssistantFetchStage
      spinnerLabel="Verifying token…"
      runAssistantMessage={async (signal) => {
        await setAccessToken(token, signal)
        return 'Access token saved'
      }}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}
