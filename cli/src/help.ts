import { accessTokenCommandDocs } from './accessToken.js'
import { gmailCommandDocs } from './gmail.js'
import { recallCommandDocs } from './recall.js'
import { formatHighlightedList } from './listDisplay.js'
import { updateDoc } from './update.js'
import { versionDoc } from './version.js'

export interface CommandDoc {
  name: string
  usage: string
  description: string
  category: 'subcommand' | 'interactive'
}

const exitDoc = {
  name: 'exit',
  usage: 'exit',
  description: 'Quit the CLI',
  category: 'subcommand' as const,
}
const subcommandDocs = [versionDoc, updateDoc, exitDoc]
const helpDocEntry = {
  name: 'help',
  usage: 'help',
  description: 'List available commands',
  category: 'subcommand' as const,
}
export const interactiveDocs = [
  {
    name: '/help',
    usage: '/help',
    description: 'List available commands',
    category: 'interactive' as const,
  },
  {
    name: '/exit',
    usage: '/exit',
    description: 'Quit the CLI',
    category: 'interactive' as const,
  },
  ...accessTokenCommandDocs,
  ...gmailCommandDocs,
  ...recallCommandDocs,
]

export function filterCommandsByPrefix(
  commands: readonly CommandDoc[],
  prefix: string
): CommandDoc[] {
  return commands.filter((d) => d.usage.startsWith(prefix))
}

export function formatCommandSuggestions(
  commands: readonly CommandDoc[],
  maxVisible = 8
): string[] {
  const lines = commands.map((d) => `  ${d.usage.padEnd(20)} ${d.description}`)
  if (lines.length <= maxVisible) return lines
  return [...lines.slice(0, maxVisible), '  ↓ more below']
}

export function formatCommandSuggestionsWithHighlight(
  commands: readonly CommandDoc[],
  maxVisible = 8,
  highlightIndex = 0
): string[] {
  const lines = commands.map((d) => `  ${d.usage.padEnd(20)} ${d.description}`)
  return formatHighlightedList(lines, maxVisible, highlightIndex)
}

export const helpDoc = helpDocEntry

const allSubcommandDocs = [...subcommandDocs, helpDocEntry]

function formatSection(title: string, docs: readonly CommandDoc[]): string {
  const lines = docs.map((d) => {
    const padded = d.usage.padEnd(28)
    return `  ${padded}${d.description}`
  })
  return `${title}:\n${lines.join('\n')}`
}

export function formatHelp(): string {
  return [
    formatSection('Subcommands', allSubcommandDocs),
    '',
    formatSection('Interactive commands (in prompt)', interactiveDocs),
  ].join('\n')
}
