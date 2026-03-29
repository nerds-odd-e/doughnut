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

/** OAuth + profile; returns one assistant transcript line. Throws on failure. */
export async function runAddGmailInteractiveAssistantMessage(): Promise<string> {
  const email = await addGmailAccount(undefined, undefined)
  return formatAddedGmailAccountMessage(email)
}

export function createAddGmailCommand(): InteractiveSlashCommand {
  return {
    line: '/add gmail',
    doc: addGmailDoc,
    async run() {
      return {
        assistantMessage: await runAddGmailInteractiveAssistantMessage(),
      }
    },
  }
}
