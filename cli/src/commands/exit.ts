import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'

const exitDoc: CommandDoc = {
  name: '/exit',
  usage: '/exit',
  description: 'Quit the CLI',
}

export function createExitCommand(
  exitApp: () => void
): InteractiveSlashCommand {
  return {
    line: '/exit',
    doc: exitDoc,
    run() {
      setImmediate(() => {
        exitApp()
      })
      return { assistantMessage: 'Bye.' }
    },
  }
}
