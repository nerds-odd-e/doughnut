import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { RemoveAccessTokenCompletelyPickerStage } from './RemoveAccessTokenCompletelyPickerStage.js'
import { RemoveAccessTokenPickerStage } from './RemoveAccessTokenPickerStage.js'

const removeAccessTokenDoc: CommandDoc = {
  name: '/remove-access-token',
  usage: '/remove-access-token <label>',
  description: 'Remove a stored access token from local config only',
}

const removeAccessTokenCompletelyDoc: CommandDoc = {
  name: '/remove-access-token-completely',
  usage: '/remove-access-token-completely <label>',
  description:
    'Revoke a stored access token on the server and remove it locally',
}

export const removeAccessTokenSlashCommand: InteractiveSlashCommand = {
  literal: '/remove-access-token',
  doc: removeAccessTokenDoc,
  stageComponent: RemoveAccessTokenPickerStage,
}

export const removeAccessTokenCompletelySlashCommand: InteractiveSlashCommand =
  {
    literal: '/remove-access-token-completely',
    doc: removeAccessTokenCompletelyDoc,
    stageComponent: RemoveAccessTokenCompletelyPickerStage,
  }
