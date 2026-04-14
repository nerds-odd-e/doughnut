/** Matches `BookBlockTitleLimits.STRUCTURAL_MAX_CHARS` on the backend. */
export const BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS = 512

export function structuralTitleSourceFromContentBlockRaw(raw: string): {
  fullText: string
  exceedsMax: boolean
} {
  let fullText = ""
  try {
    const parsed: unknown = JSON.parse(raw)
    if (
      parsed !== null &&
      typeof parsed === "object" &&
      "text" in parsed &&
      typeof (parsed as { text: unknown }).text === "string"
    ) {
      fullText = (parsed as { text: string }).text.trim()
    }
  } catch {
    // invalid JSON: no textual title source (aligns with server derive path)
  }
  return {
    fullText,
    exceedsMax: fullText.length > BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS,
  }
}

export function defaultStructuralTitleDraft(fullText: string): string {
  const t = fullText.trim()
  if (t.length <= BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS) {
    return t
  }
  return t.slice(0, BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS)
}
