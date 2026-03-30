/**
 * Pure slash-command completion: filtering, guidance display, tab completion,
 * and arrow highlight cycling. List window UI lives in guidanceListWindowInk.tsx.
 * Used by MainInteractivePrompt in this folder.
 */

import stringWidth from 'string-width'
import { interactiveSlashCommands } from '../commands/interactiveSlashCommands.js'
import type { InteractiveSlashCommand } from '../commands/interactiveSlashCommand.js'

export const DEFAULT_INTERACTIVE_GUIDANCE = '/ commands'

export type SlashCompletionListRow = {
  readonly usage: string
  readonly description: string
  /** Buffer text after Tab/Enter pick (no `<argument>` placeholder). */
  readonly completionLine: string
}

export function slashListMaxUsageWidth(
  rows: readonly { readonly usage: string }[]
): number {
  let m = 0
  for (const r of rows) {
    const w = stringWidth(r.usage)
    if (w > m) m = w
  }
  return m
}

export function padSlashListUsageColumn(
  usage: string,
  minCols: number
): string {
  const w = stringWidth(usage)
  if (w >= minCols) return usage
  return `${usage}${' '.repeat(minCols - w)}`
}

function normalizedDraft(draft: string): string {
  return draft.replace(/\n/g, ' ')
}

export function getSlashTabCompletion(buffer: string): {
  completed: string
  count: number
} {
  if (!buffer.startsWith('/')) return { completed: buffer, count: 0 }
  const matches = interactiveSlashCommands.filter((c) =>
    c.doc.usage.startsWith(buffer)
  )
  if (matches.length === 0) return { completed: buffer, count: 0 }
  if (matches.length === 1)
    return { completed: `${matches[0]!.line} `, count: 1 }
  const lines = matches.map((m) => m.line)
  let prefix = lines[0]!
  for (let i = 1; i < lines.length; i++) {
    while (!lines[i]!.startsWith(prefix) && prefix.length > 0) {
      prefix = prefix.slice(0, -1)
    }
  }
  if (prefix.length > buffer.length)
    return { completed: prefix, count: matches.length }
  return { completed: buffer, count: matches.length }
}

function filterSlashCommandsByPrefix(
  commands: readonly InteractiveSlashCommand[],
  prefix: string
): InteractiveSlashCommand[] {
  const searchTerm =
    prefix.startsWith('/') && prefix.length > 1 ? prefix.slice(1) : prefix
  if (!searchTerm) return [...commands]

  return [...commands]
    .filter((c) => c.doc.usage.includes(searchTerm))
    .sort((a, b) => {
      const aBegins =
        a.doc.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && a.doc.usage.startsWith(`/${searchTerm}`))
      const bBegins =
        b.doc.usage.startsWith(prefix) ||
        (prefix.startsWith('/') && b.doc.usage.startsWith(`/${searchTerm}`))
      if (aBegins && !bBegins) return -1
      if (!aBegins && bBegins) return 1
      return 0
    })
}

export type SlashGuidanceForInk =
  | { show: 'hint' }
  | { show: 'empty' }
  | {
      show: 'list'
      readonly rows: readonly SlashCompletionListRow[]
    }

export function slashGuidanceForInk(draft: string): SlashGuidanceForInk {
  const p = normalizedDraft(draft)
  if (!p.startsWith('/') || p.endsWith(' ')) return { show: 'hint' }
  const matches = filterSlashCommandsByPrefix(interactiveSlashCommands, p)
  if (matches.length === 0) return { show: 'empty' }
  const rows = matches.map((c) => ({
    usage: c.doc.usage,
    description: c.doc.description,
    completionLine: c.line,
  }))
  return { show: 'list', rows }
}

export function effectiveSlashGuidance(
  draft: string,
  suggestionsDismissed: boolean
): SlashGuidanceForInk {
  const g = slashGuidanceForInk(draft)
  if (suggestionsDismissed && g.show === 'list') return { show: 'hint' }
  return g
}

export function isSlashListArrowKey(
  key: 'up' | 'down',
  caretOffset: number,
  lineDraft: string,
  slashCompletionListVisible: boolean
): boolean {
  if (!slashCompletionListVisible) return false
  if (key === 'up') return caretOffset === 0
  return caretOffset === lineDraft.length
}

export function isBareDraftSlash(draft: string): boolean {
  return normalizedDraft(draft) === '/'
}

export function normalizeInputForSlash(input: string): string {
  return normalizedDraft(input)
}
