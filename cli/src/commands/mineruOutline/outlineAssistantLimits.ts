export const BOOK_OUTLINE_ASSISTANT_MAX_CHARS = 12_000

export function truncateForBookOutlineAssistant(text: string): string {
  if (text.length <= BOOK_OUTLINE_ASSISTANT_MAX_CHARS) return text
  return `${text.slice(0, BOOK_OUTLINE_ASSISTANT_MAX_CHARS)}…`
}
