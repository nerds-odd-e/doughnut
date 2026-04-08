/** Diagnostic-only: safe visible text and truncation for PTY assertion failures (internal to tty-assert). */
import { stripAnsiCliPty } from './stripAnsi'

const TERMINAL_ERROR_PREVIEW_LEN = 500
const TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS = 12_000
const TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS = 8000

/** Raw PTY tail length before `tailPreview` / sanitize in `dumpFrames` diagnostics. */
export const TERMINAL_ERROR_RAW_TAIL_BYTES = 800

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

function truncateLocatorFailureSnapshot(snapshot: string): string {
  const max = TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS
  return snapshot.length > max
    ? `${snapshot.slice(0, max)}\n… (truncated)`
    : snapshot
}

function snapshotWithRowNumbers(snapshot: string): string {
  const lines = snapshot.split('\n')
  const lastLineNo = lines.length
  const numWidth = Math.max(3, String(lastLineNo).length)
  return lines
    .map((line, i) => `${String(i + 1).padStart(numWidth)} | ${line}`)
    .join('\n')
}

function numberedSnapshotForError(snapshot: string): string {
  return truncateLocatorFailureSnapshot(snapshotWithRowNumbers(snapshot))
}

/** Managed session assertion failures: xterm viewport as plain rows (before raw transcript appendix). */
export const TERMINAL_ERROR_FINAL_VIEWPORT_HEADING =
  '--- Final visible screen (viewport) ---'

export function formatFinalViewportPlaintextForError(
  viewportPlain: string
): string {
  const snap = numberedSnapshotForError(viewportPlain)
  return `${TERMINAL_ERROR_FINAL_VIEWPORT_HEADING}\n${snap}\n---`
}

/**
 * Body for `waitForTextInSurface` / managed session assertion failures (before raw appendix).
 */
export function formatSearchSurfaceFailure(
  surface: 'viewableBuffer' | 'fullBuffer' | 'strippedTranscript',
  detail: string,
  snapshot: string
): string {
  const snap = numberedSnapshotForError(snapshot)
  const note =
    surface === 'strippedTranscript'
      ? 'Search uses the ANSI-stripped cumulative transcript (same text as the snapshot below).'
      : 'Snapshot: newline-separated trimmed rows. Matching uses a flat row-major block with no newlines between rows.'
  return `${detail}\nSearch surface: "${surface}".\n${note}\n---\n${snap}\n---`
}
