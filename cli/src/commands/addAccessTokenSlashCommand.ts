import { addAccessToken } from './accessToken.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const addAccessTokenDoc: CommandDoc = {
  name: '/add-access-token',
  usage: '/add-access-token <token>',
  description: 'Add a Doughnut API access token',
}

const PREFIX = '/add-access-token'

export const addAccessTokenSlashCommand: InteractiveSlashCommand = {
  line: PREFIX,
  doc: addAccessTokenDoc,
  matchesCommittedLine: (line) =>
    line === PREFIX || line.startsWith(`${PREFIX} `),
  async run(committedLine = PREFIX) {
    if (committedLine === PREFIX) {
      return {
        assistantMessage:
          'Missing access token. Usage: /add-access-token <token>',
      }
    }
    const token = committedLine.slice(`${PREFIX} `.length).trim()
    if (token === '') {
      return {
        assistantMessage:
          'Missing access token. Usage: /add-access-token <token>',
      }
    }
    await addAccessToken(token)
    return { assistantMessage: 'Token added successfully' }
  },
}
