export const AUTHORED_NOTE_TITLE_PLAIN_ALIAS_MESSAGE =
  "Title must not use ／ for alternative spellings; add aliases in note frontmatter instead."

const TITLE_WITH_QUALIFIER = /^(.+?)(\p{Ps}([^\p{Ps}\p{Pe}]+)\p{Pe})?$/su

function normalizeWhitespace(content: string): string {
  return content.replace(/\p{White_Space}/gu, " ")
}

function splitAliasSegments(text: string): string[] {
  const rawSegments: string[] = []
  let current = ""
  for (let i = 0; i < text.length; i++) {
    const character = text[i]
    if (character === "／") {
      if (i + 1 < text.length && text[i + 1] === "／") {
        current += "／"
        i++
      } else {
        rawSegments.push(current)
        current = ""
      }
    } else {
      current += character
    }
  }
  rawSegments.push(current)
  return rawSegments
}

function aliasSectionWithoutQualifier(title: string): string | null {
  const match = TITLE_WITH_QUALIFIER.exec(title)
  if (!match) return null
  return match[1] ?? null
}

function isSuffixFragment(segment: string): boolean {
  const trimmed = normalizeWhitespace(segment).trim()
  return (
    trimmed.startsWith("~") ||
    trimmed.startsWith("〜") ||
    trimmed.startsWith("～")
  )
}

export function hasPlainTitleAliasSegments(title: string): boolean {
  const aliasSection = aliasSectionWithoutQualifier(title)
  if (aliasSection == null) return false
  const segments = splitAliasSegments(aliasSection)
  for (let i = 1; i < segments.length; i++) {
    if (!isSuffixFragment(segments[i] ?? "")) {
      return true
    }
  }
  return false
}

export function authoredNoteTitleValidationError(
  title: string
): string | undefined {
  if (hasPlainTitleAliasSegments(title)) {
    return AUTHORED_NOTE_TITLE_PLAIN_ALIAS_MESSAGE
  }
  return
}
