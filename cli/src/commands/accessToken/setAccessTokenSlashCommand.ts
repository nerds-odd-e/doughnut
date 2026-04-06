import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { SetAccessTokenStage } from './SetAccessTokenStage.js'

const setAccessTokenDoc: CommandDoc = {
  name: '/set-access-token',
  usage: '/set-access-token <token>',
  description: 'Set the Doughnut API access token',
}

const PREFIX = '/set-access-token'

export const setAccessTokenSlashCommand: InteractiveSlashCommand = {
  literal: PREFIX,
  doc: setAccessTokenDoc,
  argument: { name: 'access token', optional: false },
  stageComponent: SetAccessTokenStage,
}
