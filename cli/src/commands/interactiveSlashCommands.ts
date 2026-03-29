import { addGmailSlashCommand } from './addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { helpSlashCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './lastEmailSlashCommand.js'

export const interactiveSlashCommands: readonly InteractiveSlashCommand[] = [
  helpSlashCommand,
  addGmailSlashCommand,
  lastEmailSlashCommand,
  exitSlashCommand,
]

export const interactiveSlashCommandByLine = new Map(
  interactiveSlashCommands.map((c) => [c.line, c] as const)
)
