import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'
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

export function createHelpCommand(
  getInteractiveDocs: () => readonly CommandDoc[]
): InteractiveSlashCommand {
  return {
    line: '/help',
    doc: helpDoc,
    run() {
      return {
        assistantMessage: formatInteractiveHelp(getInteractiveDocs()),
      }
    },
  }
}
