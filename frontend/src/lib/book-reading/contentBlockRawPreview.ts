import type { BookContentBlockFull } from "@generated/doughnut-backend-api"

export const BOOK_READING_CONTENT_BLOCK_PREVIEW_MAX_CHARS = 180

export function previewTextFromContentBlockRaw(
  block: Pick<BookContentBlockFull, "type" | "raw">,
  maxChars: number = BOOK_READING_CONTENT_BLOCK_PREVIEW_MAX_CHARS
): string {
  let parsed: unknown
  try {
    parsed = JSON.parse(block.raw) as unknown
  } catch {
    return truncateSingleLine(block.raw.trim() || block.type, maxChars)
  }
  if (
    parsed !== null &&
    typeof parsed === "object" &&
    "text" in parsed &&
    typeof (parsed as { text: unknown }).text === "string"
  ) {
    return truncateSingleLine((parsed as { text: string }).text, maxChars)
  }
  return truncateSingleLine(`[${block.type}]`, maxChars)
}

function truncateSingleLine(s: string, maxChars: number): string {
  const oneLine = s.replace(/\s+/g, " ").trim()
  if (oneLine.length <= maxChars) return oneLine
  return `${oneLine.slice(0, maxChars - 1)}…`
}
