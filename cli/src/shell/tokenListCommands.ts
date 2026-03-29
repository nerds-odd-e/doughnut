export type AccessTokenPickerAction =
  | 'set-default'
  | 'remove'
  | 'remove-completely'

export type TokenListCommandConfig = {
  readonly action: AccessTokenPickerAction
  readonly stageIndicator: string
  readonly currentPrompt?: string
}

export const TOKEN_LIST_COMMANDS: Record<string, TokenListCommandConfig> = {
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
