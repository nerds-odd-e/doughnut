import { updateDoc } from './update.js'
import { versionDoc } from './version.js'

export interface CommandDoc {
  name: string
  usage: string
  description: string
  category: 'subcommand' | 'interactive'
}

const subcommandDocs: CommandDoc[] = [versionDoc, updateDoc]

/** Slash commands and metadata for interactive /help and future TTY hints. */
export const interactiveDocs: CommandDoc[] = [
  {
    name: '/help',
    usage: '/help',
    description: 'List available commands',
    category: 'interactive',
  },
  {
    name: '/exit',
    usage: '/exit',
    description: 'Quit the CLI',
    category: 'interactive',
  },
]

function formatSection(title: string, docs: readonly CommandDoc[]): string {
  const lines = docs.map((d) => {
    const padded = d.usage.padEnd(28)
    return `  ${padded}${d.description}`
  })
  return `${title}:\n${lines.join('\n')}`
}

/** Text shown after interactive `/help` (subcommands from `run.ts` + slash commands). */
export function formatInteractiveHelp(): string {
  return [
    formatSection('Subcommands', subcommandDocs),
    '',
    formatSection('Interactive commands (in prompt)', interactiveDocs),
  ].join('\n')
}
