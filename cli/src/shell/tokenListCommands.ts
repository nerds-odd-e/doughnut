import type { AccessTokenPickerCommandConfig } from '../types.js'

/** Slash commands that enter the access-token picker TTY stage (stage indicator + optional current prompt). */
export const TOKEN_LIST_COMMANDS: Record<
  string,
  AccessTokenPickerCommandConfig
> = {
  '/list-access-token': {
    action: 'set-default',
    stageIndicator: 'Access tokens',
    currentPrompt: 'Select and enter to change the default access token',
  },
  '/remove-access-token': {
    action: 'remove',
    stageIndicator: 'Remove access token',
  },
  '/remove-access-token-completely': {
    action: 'remove-completely',
    stageIndicator: 'Remove access token completely',
  },
}
