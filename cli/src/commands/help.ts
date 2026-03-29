import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'
import { updateDoc } from './update.js'
import { versionDoc } from './version.js'

const subcommandDocs: CommandDoc[] = [versionDoc, updateDoc]

const helpDoc: CommandDoc = {
  name: '/help',
  usage: '/help',
  description: 'List available commands',
  category: 'interactive',
}

function formatSection(title: string, docs: readonly CommandDoc[]): string {
  const lines = docs.map((d) => {
    const padded = d.usage.padEnd(28)
    return `  ${padded}${d.description}`
  })
  return `${title}:\n${lines.join('\n')}`
}

function formatInteractiveHelp(interactiveDocs: readonly CommandDoc[]): string {
  return [
    formatSection('Subcommands', subcommandDocs),
    '',
    formatSection('Interactive commands (in prompt)', interactiveDocs),
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
