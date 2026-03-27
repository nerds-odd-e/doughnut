/**
 * Slash-command completion for the interactive TTY: how **Current guidance** should render
 * (generic `/` hint, empty list, or completion rows). Uses the same draft normalization as the live editor.
 */
import {
  filterCommandsByPrefix,
  interactiveDocs,
  type CommandDoc,
} from './commands/help.js'
import { singleLineCommandDraft } from './interactiveCommandInput.js'

/** Same normalization as the live command line (paste newlines → spaces). */
export function normalizedInteractiveDraft(draft: string): string {
  return singleLineCommandDraft(draft)
}

/** What to paint under the command line for slash-related drafts. */
export type SlashGuidanceForInk =
  | { show: 'hint' }
  | { show: 'empty' }
  | { show: 'list'; docs: readonly CommandDoc[] }

export function slashGuidanceForInk(draft: string): SlashGuidanceForInk {
  const p = normalizedInteractiveDraft(draft)
  if (!p.startsWith('/') || p.endsWith(' ')) return { show: 'hint' }
  const docs = filterCommandsByPrefix(interactiveDocs, p)
  if (docs.length === 0) return { show: 'empty' }
  return { show: 'list', docs }
}

/** True when the completion list (not hint, not empty) should drive ↑↓ in the guidance strip. */
export function hasInteractiveSlashCompletions(draft: string): boolean {
  return slashGuidanceForInk(draft).show === 'list'
}
