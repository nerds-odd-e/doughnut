import { addAccessTokenSlashCommand } from './accessToken/addAccessTokenSlashCommand.js'
import { addGmailSlashCommand } from './gmail/addGmail/addGmailSlashCommand.js'
import { exitSlashCommand } from './exit.js'
import { listAccessTokenSlashCommand } from './accessToken/listAccessToken/listAccessTokenSlashCommand.js'
import {
  removeAccessTokenCompletelySlashCommand,
  removeAccessTokenSlashCommand,
} from './accessToken/removeAccessTokenSlashCommands.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'
import { lastEmailSlashCommand } from './gmail/lastEmail/lastEmailSlashCommand.js'
import { recallStatusSlashCommand } from './recall.js'
import { updateDoc } from './update.js'
import { versionDoc } from './version.js'

const helpDoc: CommandDoc = {
  name: '/help',
  usage: '/help',
  description: 'List available commands',
}

function formatDocRows(
  docs: readonly { readonly usage: string; readonly description: string }[]
): string[] {
  return docs.map((d) => {
    const padded = d.usage.padEnd(28)
    return `  ${padded}${d.description}`
  })
}

function formatSection(title: string, rows: string[]): string {
  return `${title}:\n${rows.join('\n')}`
}

function formatInteractiveHelp(interactiveDocs: readonly CommandDoc[]): string {
  const subcommandRows = formatDocRows([versionDoc, updateDoc])
  const slashRows = formatDocRows(interactiveDocs)
  return [
    formatSection('Subcommands', subcommandRows),
    '',
    formatSection('Interactive commands (in prompt)', slashRows),
  ].join('\n')
}

export const helpSlashCommand: InteractiveSlashCommand = {
  line: '/help',
  doc: helpDoc,
  run() {
    return {
      assistantMessage: formatInteractiveHelp([
        helpDoc,
        addAccessTokenSlashCommand.doc,
        listAccessTokenSlashCommand.doc,
        removeAccessTokenSlashCommand.doc,
        removeAccessTokenCompletelySlashCommand.doc,
        addGmailSlashCommand.doc,
        lastEmailSlashCommand.doc,
        recallStatusSlashCommand.doc,
        exitSlashCommand.doc,
      ]),
    }
  },
}
