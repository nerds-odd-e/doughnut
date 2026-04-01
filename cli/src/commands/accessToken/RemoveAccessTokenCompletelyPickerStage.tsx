import { useMemo, useState } from 'react'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  removeAccessTokenCompletely,
} from './accessToken.js'
import { AccessTokenLabelPickerStage } from './AccessTokenLabelPickerStage.js'

const CURRENT_PROMPT =
  'Select and enter to revoke on the server and remove locally'

function initialHighlightIndexPreferDefault(labels: readonly string[]): number {
  if (labels.length === 0) return 0
  const d = getDefaultTokenLabel()
  const idx = d ? labels.indexOf(d) : -1
  return Math.max(0, idx)
}

export function RemoveAccessTokenCompletelyPickerStage({
  argument,
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  const labels = useMemo(() => getStoredAccessTokenLabels(), [])
  const [revokeLabel, setRevokeLabel] = useState<string | null>(
    argument ?? null
  )

  if (revokeLabel !== null) {
    return (
      <AsyncAssistantFetchStage
        spinnerLabel="Revoking token…"
        runAssistantMessage={async (signal) => {
          await removeAccessTokenCompletely(revokeLabel, signal)
          return `Token "${revokeLabel}" removed locally and from server.`
        }}
        onSettled={onSettled}
        onAbortWithError={onAbortWithError}
      />
    )
  }

  return (
    <AccessTokenLabelPickerStage
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
      labels={labels}
      currentPrompt={CURRENT_PROMPT}
      initialHighlightIndex={initialHighlightIndexPreferDefault}
      onPick={(label) => {
        setRevokeLabel(label)
      }}
    />
  )
}
