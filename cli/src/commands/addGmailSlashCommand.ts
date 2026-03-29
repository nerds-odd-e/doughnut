import { addGmailAccount, formatAddedGmailAccountMessage } from './gmail.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const addGmailDoc: CommandDoc = {
  name: '/add gmail',
  usage: '/add gmail',
  description: 'Connect a Gmail account (OAuth)',
}

export function createAddGmailCommand(): InteractiveSlashCommand {
  return {
    line: '/add gmail',
    doc: addGmailDoc,
    async run() {
      const email = await addGmailAccount(undefined, undefined)
      return {
        assistantMessage: formatAddedGmailAccountMessage(email),
      }
    },
  }
}
