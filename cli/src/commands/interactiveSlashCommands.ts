import { addAccessTokenSlashCommand } from './addAccessTokenSlashCommand.js'
import { addGmailSlashCommand } from './addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { helpSlashCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './lastEmailSlashCommand.js'

export const interactiveSlashCommands: readonly InteractiveSlashCommand[] = [
  helpSlashCommand,
  addAccessTokenSlashCommand,
  addGmailSlashCommand,
  lastEmailSlashCommand,
  exitSlashCommand,
]

export const interactiveSlashCommandByLine = new Map(
  interactiveSlashCommands.map((c) => [c.line, c] as const)
)

export function resolveInteractiveSlashCommand(
  line: string
): InteractiveSlashCommand | undefined {
  const exact = interactiveSlashCommandByLine.get(line)
  if (exact) return exact
  for (const c of interactiveSlashCommands) {
    if (c.matchesCommittedLine?.(line)) return c
  }
  return undefined
}
