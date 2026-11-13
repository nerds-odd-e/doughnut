/// <reference types="Cypress" />
// @ts-check
import type { NoteInjectionContext } from './testabilityNote'

const UNTITLED_NOTE_TITLE = 'Untitled'
const DEFAULT_RELATION_LABEL = 'related to'

/** Same kebab-case rule as relation type options / app compose. */
function relationKebabFromLabel(label: string): string {
  const t = label.trim()
  if (!t) {
    return relationKebabFromLabel(DEFAULT_RELATION_LABEL)
  }
  return t.toLowerCase().replace(/\s+/g, '-')
}

/** Same title shape as relationship notes created in the app (150 char cap). */
function relationshipNoteTitle(
  sourceTitle: string,
  relationLabel: string,
  targetTitle: string
): string {
  const seg = (s: string, fallback: string) => {
    const x = s.trim()
    return x === '' ? fallback : x
  }
  const source = seg(sourceTitle, UNTITLED_NOTE_TITLE)
  const relation = seg(relationLabel, DEFAULT_RELATION_LABEL)
  const target = seg(targetTitle, UNTITLED_NOTE_TITLE)
  const composed = `${source} ${relation} ${target}`.trim()
  const MAX_TITLE_LENGTH = 150
  if (composed.length > MAX_TITLE_LENGTH) {
    return composed.slice(0, MAX_TITLE_LENGTH)
  }
  return composed
}

/** Same markdown shape as new relationship notes from the app (frontmatter plus optional body). */
function relationshipNoteMarkdown(
  relationLabel: string,
  sourceTitle: string,
  targetTitle: string,
  bodySuffix?: string
): string {
  const relationKebab = relationKebabFromLabel(relationLabel)
  const sourceDisplay =
    sourceTitle.trim() === '' ? UNTITLED_NOTE_TITLE : sourceTitle.trim()
  const targetDisplay =
    targetTitle.trim() === '' ? UNTITLED_NOTE_TITLE : targetTitle.trim()
  const sourceLink = `[[${sourceDisplay}]]`
  const targetLink = `[[${targetDisplay}]]`
  const yamlEscape = (s: string) =>
    s.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  const suffix = bodySuffix?.trim() ? `\n${bodySuffix.trim()}\n` : ''
  return `---
type: Relationship
relation: ${relationKebab}
source: "${yamlEscape(sourceLink)}"
target: "${yamlEscape(targetLink)}"
---
${suffix}`
}

export const relationshipTestabilityMethods = {
  injectRelationshipNote(
    this: NoteInjectionContext,
    notebookName: string,
    relationTypeLabel: string,
    fromNoteTopic: string,
    toNoteTopic: string,
    bodySuffix?: string
  ) {
    return cy
      .get<string>('@injectNotesExternalIdentifier')
      .then((externalIdentifier) => {
        const title = relationshipNoteTitle(
          fromNoteTopic,
          relationTypeLabel,
          toNoteTopic
        )
        const content = relationshipNoteMarkdown(
          relationTypeLabel,
          fromNoteTopic,
          toNoteTopic,
          bodySuffix
        )
        return this.injectNotes(
          [{ Title: title, Content: content }],
          externalIdentifier,
          notebookName
        )
      })
  },
}
