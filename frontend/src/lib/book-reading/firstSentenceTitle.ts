const MAX_TITLE_CHARS = 80

export function firstSentenceTitle(text: string): string {
  const sentence = text.match(/^.*?[.?!](?=\s|$)/s)?.[0] ?? text
  if (sentence.length <= MAX_TITLE_CHARS) return sentence.trim()
  const lastSpace = sentence.lastIndexOf(" ", MAX_TITLE_CHARS)
  const cut = lastSpace > 0 ? lastSpace : MAX_TITLE_CHARS
  return sentence.slice(0, cut).trim()
}
