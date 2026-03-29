import { TOKEN_LIST_COMMANDS } from '../../../shell/tokenListCommands.js'
import type { InteractiveSlashCommandStageProps } from '../../interactiveSlashCommand.js'
import { AccessTokenPickerStage } from './AccessTokenPickerStage.js'

export function ListAccessTokenStage(props: InteractiveSlashCommandStageProps) {
  return (
    <AccessTokenPickerStage
      {...props}
      tokenListConfig={TOKEN_LIST_COMMANDS['/list-access-token']!}
    />
  )
}
