import type { InteractiveSlashCommandStageProps } from '../../interactiveSlashCommand.js'
import { AccessTokenPickerStage } from './AccessTokenPickerStage.js'

export function ListAccessTokenStage(props: InteractiveSlashCommandStageProps) {
  return <AccessTokenPickerStage {...props} />
}
