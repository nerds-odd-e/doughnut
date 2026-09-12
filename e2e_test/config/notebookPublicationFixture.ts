/**
 * Pure fixture-reference mapping for the notebook publication profile (safe
 * for the Cypress browser bundle) — the single source of truth for how a
 * measured document's `source`/`related` frontmatter targets other existing
 * notes. Shared by the fixture generator
 * (`start/pageObjects/cli/notebookPublicationProfile.ts`) and the derived-state
 * checker (`notebookPublicationState.ts`) so the encode and decode sides of
 * the mapping cannot drift apart.
 */

export function existingNoteTitle(index: number): string {
  return `Existing-${String(index).padStart(5, '0')}`
}

export function existingNoteIndex(title: string): number {
  return Number(title.slice('Existing-'.length))
}

export function sourceReferenceIndex(index: number, existing: number): number {
  return index % existing
}

export function relatedReferenceIndex(
  index: number,
  existing: number,
  itemIndex: number
): number {
  return (index + itemIndex + 1) % existing
}
