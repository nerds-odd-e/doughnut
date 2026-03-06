import { gmailCommandDocs } from './gmail.js'
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
  description: 'Show this help',
  category: 'subcommand' as const,
}
export const interactiveDocs = [
  {
    name: '/help',
    usage: '/help',
    description: 'Show this help',
    category: 'interactive' as const,
  },
  ...gmailCommandDocs,
]

const GREY = '\x1b[90m'
const REVERSE = '\x1b[7m'
const RESET = '\x1b[0m'

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
  maxVisible = 8
): string[] {
  if (commands.length === 0) return []
  const lines = commands.map((d) => `  ${d.usage.padEnd(20)} ${d.description}`)
  const visible =
    lines.length <= maxVisible
      ? lines
      : [...lines.slice(0, maxVisible), '  ↓ more below']
  return visible.map((line, i) =>
    i === 0 ? `${REVERSE}${line}${RESET}` : `${GREY}${line}${RESET}`
  )
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
