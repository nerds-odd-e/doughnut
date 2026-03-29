import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const exitDoc: CommandDoc = {
  name: '/exit',
  usage: '/exit',
  description: 'Quit the CLI',
}

export function createExitCommand(): InteractiveSlashCommand {
  return {
    line: '/exit',
    doc: exitDoc,
    run() {
      return { assistantMessage: 'Bye.' }
    },
  }
}
