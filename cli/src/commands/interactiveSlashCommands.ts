import { addAccessTokenSlashCommand } from './accessToken/addAccessTokenSlashCommand.js'
import { addGmailSlashCommand } from './gmail/addGmail/addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { createHelpSlashCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './gmail/lastEmail/lastEmailSlashCommand.js'
import { listAccessTokenSlashCommand } from './accessToken/listAccessToken/listAccessTokenSlashCommand.js'
import { readSlashCommand } from './read/readSlashCommand.js'
import { recallSlashCommand } from './recall/recall.js'
import { recallStatusSlashCommand } from './recallStatus.js'
import {
  removeAccessTokenCompletelySlashCommand,
  removeAccessTokenSlashCommand,
} from './accessToken/removeAccessTokenSlashCommands.js'

export const interactiveSlashCommands: readonly InteractiveSlashCommand[] = [
  createHelpSlashCommand(() => interactiveSlashCommands.map((c) => c.doc)),
  addAccessTokenSlashCommand,
  listAccessTokenSlashCommand,
  removeAccessTokenSlashCommand,
  removeAccessTokenCompletelySlashCommand,
  addGmailSlashCommand,
  lastEmailSlashCommand,
  recallSlashCommand,
  recallStatusSlashCommand,
  readSlashCommand,
  exitSlashCommand,
]

const interactiveSlashCommandByLiteral = new Map<
  string,
  InteractiveSlashCommand
>(interactiveSlashCommands.map((c) => [c.literal.slice(1), c] as const))

type ResolvedInteractiveSlashCommand = {
  command: InteractiveSlashCommand
  argument: string | undefined
}

/**
 * @param body - Same string the resolver uses for lookup: after `/` when the user committed a
 *   slash command, or the full committed line when they did not (e.g. `exit`).
 */
export function resolveInteractiveSlashCommand(
  body: string
): ResolvedInteractiveSlashCommand | undefined {
  const exact = interactiveSlashCommandByLiteral.get(body)
  if (exact) return { command: exact, argument: undefined }

  const prefix = [...interactiveSlashCommands]
    .sort((a, b) => b.literal.length - a.literal.length)
    .find((c) => body.startsWith(`${c.literal.slice(1)} `))
  if (!prefix) return undefined

  const cmdBody = prefix.literal.slice(1)
  const rest = body.slice(cmdBody.length + 1).trim()
  return {
    command: prefix,
    argument: rest === '' ? undefined : rest,
  }
}
