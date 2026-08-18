import { authoredLinkOccurrences } from "@/utils/authoredLinkMarkup"

export function hasNewWikiLinkTexts(previous: string, next: string): boolean {
  const prev = new Set(authoredLinkOccurrences(previous).map((o) => o.token))
  for (const occ of authoredLinkOccurrences(next)) {
    if (!prev.has(occ.token)) return true
  }
  return false
}
