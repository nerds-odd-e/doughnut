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
    name: '/clear',
    usage: '/clear',
    description: 'Clear screen and chat history',
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

export function getTabCompletion(
  buffer: string,
  commands: readonly CommandDoc[]
): { completed: string; count: number } {
  if (!buffer.startsWith('/')) return { completed: buffer, count: 0 }
  const matches = commands.filter((c) => c.usage.startsWith(buffer))
  if (matches.length === 0) return { completed: buffer, count: 0 }
  if (matches.length === 1)
    return { completed: `${matches[0].usage} `, count: 1 }
  const usages = matches.map((m) => m.usage)
  let prefix = usages[0]
  for (let i = 1; i < usages.length; i++) {
    while (!usages[i].startsWith(prefix) && prefix.length > 0) {
      prefix = prefix.slice(0, -1)
    }
  }
  if (prefix.length > buffer.length)
    return { completed: prefix, count: matches.length }
  return { completed: buffer, count: matches.length }
}

export function filterCommandsByPrefix(
  commands: readonly CommandDoc[],
  prefix: string
): CommandDoc[] {
  const searchTerm =
    prefix.startsWith('/') && prefix.length > 1 ? prefix.slice(1) : prefix
  if (!searchTerm) return [...commands]

  return [...commands]
    .filter((d) => d.usage.includes(searchTerm))
    .sort((a, b) => {
      const aBegins =
        a.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && a.usage.startsWith(`/${searchTerm}`))
      const bBegins =
        b.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && b.usage.startsWith(`/${searchTerm}`))
      if (aBegins && !bBegins) return -1
      if (!aBegins && bBegins) return 1
      return 0
    })
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
