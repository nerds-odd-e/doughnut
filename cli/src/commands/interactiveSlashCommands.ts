import { addAccessTokenSlashCommand } from './accessToken/addAccessTokenSlashCommand.js'
import { addGmailSlashCommand } from './gmail/addGmail/addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { createHelpSlashCommand } from './help.js'
import type { InteractiveSlashCommand } from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './gmail/lastEmail/lastEmailSlashCommand.js'
import { recallSlashCommand } from './recall/recall.js'
import { useNotebookSlashCommand } from './notebook/useNotebookSlashCommand.js'
import { recallStatusSlashCommand } from './recallStatus.js'
export const interactiveSlashCommands: readonly InteractiveSlashCommand[] = [
  createHelpSlashCommand(() => interactiveSlashCommands.map((c) => c.doc)),
  addAccessTokenSlashCommand,
  addGmailSlashCommand,
  lastEmailSlashCommand,
  recallSlashCommand,
  recallStatusSlashCommand,
  useNotebookSlashCommand,
  exitSlashCommand,
]

export type ResolvedInteractiveSlashCommand = {
  command: InteractiveSlashCommand
  argument: string | undefined
  /** Full committed input line (e.g. `/help`, `exit`) for transcript display. */
  line: string
}

/**
 * @param slashCommands - Same registry the prompt uses for Tab/`/` guidance (e.g. main vs stage).
 * @param line - Full committed input line; echoed on {@link ResolvedInteractiveSlashCommand.line}.
 */
export function resolveInteractiveSlashCommand(
  line: string,
  slashCommands: readonly InteractiveSlashCommand[]
): ResolvedInteractiveSlashCommand | undefined {
  const byLiteral = new Map<string, InteractiveSlashCommand>(
    slashCommands.map((c) => [c.literal.slice(1), c])
  )
  const body = line.startsWith('/')
    ? line.slice(1)
    : line.trim() === 'exit'
      ? 'exit'
      : undefined

  if (body === undefined) return undefined

  const exact = byLiteral.get(body)
  if (exact) return { command: exact, argument: undefined, line }

  const prefix = [...slashCommands]
    .sort((a, b) => b.literal.length - a.literal.length)
    .find((c) => body.startsWith(`${c.literal.slice(1)} `))
  if (!prefix) return undefined

  const cmdBody = prefix.literal.slice(1)
  const rest = body.slice(cmdBody.length + 1).trim()
  return {
    command: prefix,
    argument: rest === '' ? undefined : rest,
    line,
  }
}
