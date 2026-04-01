import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { AddAccessTokenStage } from './AddAccessTokenStage.js'

const addAccessTokenDoc: CommandDoc = {
  name: '/add-access-token',
  usage: '/add-access-token <token>',
  description: 'Add a Doughnut API access token',
}

const PREFIX = '/add-access-token'

export const addAccessTokenSlashCommand: InteractiveSlashCommand = {
  literal: PREFIX,
  doc: addAccessTokenDoc,
  argument: { name: 'access token', optional: false },
  stageComponent: AddAccessTokenStage,
}
