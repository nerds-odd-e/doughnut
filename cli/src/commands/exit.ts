import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'

export function createExitCommand(
  exitApp: () => void
): InteractiveSlashCommand {
  return {
    line: '/exit',
    run() {
      setTimeout(() => {
        exitApp()
      }, 0)
      return { assistantMessage: 'Bye.' }
    },
  }
}
