import { addAccessToken } from './accessToken.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'

const addAccessTokenDoc: CommandDoc = {
  name: '/add-access-token',
  usage: '/add-access-token <token>',
  description: 'Add a Doughnut API access token',
}

const PREFIX = '/add-access-token'

export const addAccessTokenSlashCommand: InteractiveSlashCommand = {
  line: PREFIX,
  doc: addAccessTokenDoc,
  argument: { name: 'access token', optional: false },
  async run(argument) {
    await addAccessToken(argument!)
    return { assistantMessage: 'Token added successfully' }
  },
}
