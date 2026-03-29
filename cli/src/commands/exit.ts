import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const exitDoc: CommandDoc = {
  name: '/exit',
  usage: '/exit',
  description: 'Quit the CLI',
  category: 'interactive',
}

export function createExitCommand(
  exitApp: () => void
): InteractiveSlashCommand {
  return {
    line: '/exit',
    doc: exitDoc,
    run() {
      setTimeout(() => {
        exitApp()
      }, 0)
      return { assistantMessage: 'Bye.' }
    },
  }
}
