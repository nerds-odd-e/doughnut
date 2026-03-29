import { addAccessTokenSlashCommand } from './addAccessTokenSlashCommand.js'
import { addGmailSlashCommand } from './addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { helpSlashCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './lastEmailSlashCommand.js'
import { listAccessTokenSlashCommand } from './listAccessTokenSlashCommand.js'

export const interactiveSlashCommands: readonly InteractiveSlashCommand[] = [
  helpSlashCommand,
  addAccessTokenSlashCommand,
  listAccessTokenSlashCommand,
  addGmailSlashCommand,
  lastEmailSlashCommand,
  exitSlashCommand,
]

export const interactiveSlashCommandByLine = new Map(
  interactiveSlashCommands.map((c) => [c.line, c] as const)
)

export type ResolvedInteractiveSlashCommand = {
  command: InteractiveSlashCommand
  argument: string | undefined
}

export function resolveInteractiveSlashCommand(
  line: string
): ResolvedInteractiveSlashCommand | undefined {
  const exact = interactiveSlashCommandByLine.get(line)
  if (exact) return { command: exact, argument: undefined }

  const prefix = [...interactiveSlashCommands]
    .sort((a, b) => b.line.length - a.line.length)
    .find((c) => line.startsWith(`${c.line} `))
  if (!prefix) return undefined

  const rest = line.slice(prefix.line.length + 1).trim()
  return {
    command: prefix,
    argument: rest === '' ? undefined : rest,
  }
}
