import { createExitCommand } from './exit.js'
import { createHelpCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'

export function createInteractiveSlashCommands(
  exitApp: () => void
): InteractiveSlashCommand[] {
  const commands: InteractiveSlashCommand[] = []
  const helpCmd = createHelpCommand(() => commands.map((c) => c.doc))
  commands.push(helpCmd, createExitCommand(exitApp))
  return commands
}
