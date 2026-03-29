import { useMemo } from 'react'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  removeAccessTokenLocal,
} from './accessToken.js'
import { AccessTokenLabelPickerStage } from './AccessTokenLabelPickerStage.js'

const STAGE_INDICATOR = 'Remove access token'
const CURRENT_PROMPT =
  'Select and enter to remove the token from local config only'

function initialHighlightIndexPreferDefault(labels: readonly string[]): number {
  if (labels.length === 0) return 0
  const d = getDefaultTokenLabel()
  const idx = d ? labels.indexOf(d) : -1
  return Math.max(0, idx)
}

export function RemoveAccessTokenPickerStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const labels = useMemo(() => getStoredAccessTokenLabels(), [])

  return (
    <AccessTokenLabelPickerStage
      onSettled={onSettled}
      labels={labels}
      stageIndicator={STAGE_INDICATOR}
      currentPrompt={CURRENT_PROMPT}
      initialHighlightIndex={initialHighlightIndexPreferDefault}
      onPick={(label) => {
        removeAccessTokenLocal(label)
        onSettled(`Token "${label}" removed.`)
      }}
    />
  )
}
