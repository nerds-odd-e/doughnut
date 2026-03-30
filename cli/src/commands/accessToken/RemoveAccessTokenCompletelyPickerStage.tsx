import { useMemo, useState } from 'react'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  removeAccessTokenCompletely,
} from './accessToken.js'
import { AccessTokenLabelPickerStage } from './AccessTokenLabelPickerStage.js'

const STAGE_INDICATOR = 'Remove access token completely'
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
      />
    )
  }

  return (
    <AccessTokenLabelPickerStage
      onSettled={onSettled}
      labels={labels}
      stageIndicator={STAGE_INDICATOR}
      currentPrompt={CURRENT_PROMPT}
      initialHighlightIndex={initialHighlightIndexPreferDefault}
      onPick={(label) => {
        setRevokeLabel(label)
      }}
    />
  )
}
