import { getLastEmailSubject } from './gmail.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const lastEmailDoc: CommandDoc = {
  name: '/last email',
  usage: '/last email',
  description: 'Show subject of last email',
}

/** Gmail list + metadata; returns one assistant transcript line. Throws on failure. */
export async function runLastEmailInteractiveAssistantMessage(): Promise<string> {
  return getLastEmailSubject(undefined, undefined)
}

export function createLastEmailCommand(): InteractiveSlashCommand {
  return {
    line: '/last email',
    doc: lastEmailDoc,
    async run() {
      return {
        assistantMessage: await runLastEmailInteractiveAssistantMessage(),
      }
    },
  }
}
