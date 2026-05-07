const WIKI_LINK = /\[\[([^\]]+)]]/g

export function extractWikiLinkTexts(markdown: string): Set<string> {
  const texts = new Set<string>()
  for (const match of markdown.matchAll(WIKI_LINK)) {
    const inner = match[1]?.trim()
    if (inner) texts.add(inner)
  }
  return texts
}

export function hasNewWikiLinkTexts(previous: string, next: string): boolean {
  const prev = extractWikiLinkTexts(previous)
  for (const t of extractWikiLinkTexts(next)) {
    if (!prev.has(t)) return true
  }
  return false
}
