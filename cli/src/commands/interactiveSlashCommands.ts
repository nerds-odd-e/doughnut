import { addAccessTokenSlashCommand } from './accessToken/addAccessTokenSlashCommand.js'
import { addGmailSlashCommand } from './gmail/addGmail/addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { helpSlashCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './gmail/lastEmail/lastEmailSlashCommand.js'
import { listAccessTokenSlashCommand } from './accessToken/listAccessToken/listAccessTokenSlashCommand.js'
import { recallStatusSlashCommand } from './recall.js'
import {
  removeAccessTokenCompletelySlashCommand,
  removeAccessTokenSlashCommand,
} from './accessToken/removeAccessTokenSlashCommands.js'

export const interactiveSlashCommands: readonly InteractiveSlashCommand[] = [
  helpSlashCommand,
  addAccessTokenSlashCommand,
  listAccessTokenSlashCommand,
  removeAccessTokenSlashCommand,
  removeAccessTokenCompletelySlashCommand,
  addGmailSlashCommand,
  lastEmailSlashCommand,
  recallStatusSlashCommand,
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
