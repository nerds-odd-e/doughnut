import { useEffect, useMemo } from 'react'
import type { InteractiveSlashCommandStageProps } from '../interactiveSlashCommand.js'
import {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  removeAccessTokenLocal,
} from './accessToken.js'
import { AccessTokenLabelPickerStage } from './AccessTokenLabelPickerStage.js'

const CURRENT_PROMPT =
  'Select and enter to remove the token from local config only'

function initialHighlightIndexPreferDefault(labels: readonly string[]): number {
  if (labels.length === 0) return 0
  const d = getDefaultTokenLabel()
  const idx = d ? labels.indexOf(d) : -1
  return Math.max(0, idx)
}

export function RemoveAccessTokenPickerStage({
  argument,
  onSettled,
}: InteractiveSlashCommandStageProps) {
  const labels = useMemo(() => getStoredAccessTokenLabels(), [])

  useEffect(() => {
    if (argument) {
      removeAccessTokenLocal(argument)
      onSettled(`Token "${argument}" removed.`)
    }
  }, [argument, onSettled])

  if (argument) return null

  return (
    <AccessTokenLabelPickerStage
      onSettled={onSettled}
      labels={labels}
      currentPrompt={CURRENT_PROMPT}
      initialHighlightIndex={initialHighlightIndexPreferDefault}
      onPick={(label) => {
        removeAccessTokenLocal(label)
        onSettled(`Token "${label}" removed.`)
      }}
    />
  )
}
