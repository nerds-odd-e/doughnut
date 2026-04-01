import { useMemo } from 'react'
import type { InteractiveSlashCommandStageProps } from '../../interactiveSlashCommand.js'
import {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  setDefaultTokenLabel,
} from '../accessToken.js'
import { AccessTokenLabelPickerStage } from '../AccessTokenLabelPickerStage.js'

const CURRENT_PROMPT = 'Select and enter to change the default access token'

function initialHighlightIndexPreferDefault(labels: readonly string[]): number {
  if (labels.length === 0) return 0
  const d = getDefaultTokenLabel()
  const idx = d ? labels.indexOf(d) : -1
  return Math.max(0, idx)
}

export function ListAccessTokenStage({
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const labels = useMemo(() => getStoredAccessTokenLabels(), [])

  return (
    <AccessTokenLabelPickerStage
      onSettled={onSettled}
      labels={labels}
      currentPrompt={CURRENT_PROMPT}
      initialHighlightIndex={initialHighlightIndexPreferDefault}
      onPick={(label) => {
        setDefaultTokenLabel(label)
        onSettled(`Default token set to: ${label}`)
      }}
    />
  )
}
