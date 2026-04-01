/** Diagnostic-only: safe visible text and truncation for PTY assertion failures. */
import { stripAnsiCliPty } from './stripAnsi'

export const TERMINAL_ERROR_PREVIEW_LEN = 500
export const TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS = 12_000

export function sanitizeVisibleTextForError(s: string): string {
  let out = s.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  out = out.split(String.fromCharCode(0x1b)).join('<ESC>')
  return [...out]
    .map((ch) => {
      const n = ch.charCodeAt(0)
      if (n === 0x09 || n === 0x0a) return ch
      if (n < 0x20 || n === 0x7f) {
        return `<0x${n.toString(16).padStart(2, '0')}>`
      }
      return ch
    })
    .join('')
}

export function formatRawTerminalSnapshotForError(raw: string): string {
  const stripped = stripAnsiCliPty(raw)
  const visible = sanitizeVisibleTextForError(stripped)
  const max = TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS
  const truncated =
    visible.length > max
      ? `${visible.slice(0, max)}\n\n… truncated (${visible.length} visible chars, showing first ${max})`
      : visible
  return `raw bytes: ${raw.length} | ANSI-stripped: ${stripped.length} chars\n\n${truncated}`
}

export function headPreview(text: string): string {
  return text.length > TERMINAL_ERROR_PREVIEW_LEN
    ? `${text.slice(0, TERMINAL_ERROR_PREVIEW_LEN)}...`
    : text
}

export function tailPreview(text: string): string {
  return text.length > TERMINAL_ERROR_PREVIEW_LEN
    ? text.slice(-TERMINAL_ERROR_PREVIEW_LEN)
    : text
}
