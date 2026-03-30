/** Normalizes the spelling command buffer before submit (trim + newline → space). */
export function normalizeSpellingLineForSubmit(raw: string): string {
  return raw.replace(/\r\n/g, ' ').replace(/\n/g, ' ').trim()
}
