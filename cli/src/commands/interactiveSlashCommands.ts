import { createAddGmailCommand } from './addGmailSlashCommand.js'
import { createExitCommand } from './exit.js'
import { createHelpCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { createLastEmailCommand } from './lastEmailSlashCommand.js'

export function createInteractiveSlashCommands(): InteractiveSlashCommand[] {
  const commands: InteractiveSlashCommand[] = []
  const helpCmd = createHelpCommand(() => commands.map((c) => c.doc))
  commands.push(
    helpCmd,
    createAddGmailCommand(),
    createLastEmailCommand(),
    createExitCommand()
  )
  return commands
}
