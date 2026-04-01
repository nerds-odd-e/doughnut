import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const exitDoc: CommandDoc = {
  name: '/exit',
  usage: '/exit, exit',
  description: 'Quit the CLI',
}

export const exitSlashCommand: InteractiveSlashCommand = {
  literal: '/exit',
  doc: exitDoc,
  run() {
    return { assistantMessage: 'Bye.' }
  },
}
