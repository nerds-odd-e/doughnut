import { wikiPortablePathTargetsInOccurrenceOrder } from "@/utils/authoredNoteReference"

export function hasNewWikiLinkTexts(previous: string, next: string): boolean {
  const prev = new Set(
    wikiPortablePathTargetsInOccurrenceOrder(previous).map(
      (ref) => ref.authoredLink
    )
  )
  for (const ref of wikiPortablePathTargetsInOccurrenceOrder(next)) {
    if (!prev.has(ref.authoredLink)) return true
  }
  return false
}
