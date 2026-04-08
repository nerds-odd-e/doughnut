/** Diagnostic-only: safe visible text and truncation for PTY assertion failures. */
import { CLI_INTERACTIVE_PTY_COLS } from './geometry'
import { stripAnsiCliPty } from './stripAnsi'

export const TERMINAL_ERROR_PREVIEW_LEN = 500
export const TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS = 12_000

/** Locator failure block (`---` section): row-numbered snapshot is truncated at this size. */
export const TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS = 8000

/** Raw PTY tail length before `tailPreview` / sanitize in `dumpFrames` diagnostics. */
export const TERMINAL_ERROR_RAW_TAIL_BYTES = 800

export type TtyLocatorSearchSurface =
  | 'viewableBuffer'
  | 'fullBuffer'
  | 'strippedTranscript'

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

export function truncateLocatorFailureSnapshot(snapshot: string): string {
  const max = TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS
  return snapshot.length > max
    ? `${snapshot.slice(0, max)}\n… (truncated)`
    : snapshot
}

export type FormatSnapshotWithRowNumbersOptions = {
  /** 1-based index of the first line (default `1`). */
  firstLine?: number
  /** When true, prepend a tens column ruler for `cols` columns (default off). */
  columnRuler?: boolean
  /** Terminal width for the ruler; defaults to `CLI_INTERACTIVE_PTY_COLS`. */
  cols?: number
}

/**
 * Prefixes each line with a fixed-width row number. Split on `\n`; a snapshot with no newlines
 * becomes a single numbered line.
 */
export function formatSnapshotWithRowNumbers(
  snapshot: string,
  options?: FormatSnapshotWithRowNumbersOptions
): string {
  const firstLine = options?.firstLine ?? 1
  const lines = snapshot.split('\n')
  const lastLineNo = firstLine + lines.length - 1
  const numWidth = Math.max(3, String(lastLineNo).length)
  const parts: string[] = []

  if (options?.columnRuler === true) {
    const cols = options.cols ?? CLI_INTERACTIVE_PTY_COLS
    let ruler = ''
    for (let c = 0; c < cols; c++) {
      ruler += String(c % 10)
    }
    parts.push(`${' '.repeat(numWidth)} | ${ruler}`)
  }

  for (let i = 0; i < lines.length; i++) {
    const n = firstLine + i
    parts.push(`${String(n).padStart(numWidth)} | ${lines[i]}`)
  }
  return parts.join('\n')
}

/**
 * Inserts `«` before and `»` after the half-open range `[start, end)` in `text`.
 * Indices are UTF-16 code unit offsets (JavaScript string indices). Non-finite values are treated
 * as `0`; out-of-range values are clamped to `[0, text.length]`; if `start > end` after clamping,
 * the bounds are swapped.
 *
 * For failure messages: **annotate first**, then `formatSnapshotWithRowNumbers`, so offsets refer
 * to the same string the locator snapshot uses (newline-separated rows).
 */
export function annotateSubstringRangeInText(
  text: string,
  start: number,
  end: number
): string {
  let s = Number.isFinite(start) ? Math.trunc(start) : 0
  let e = Number.isFinite(end) ? Math.trunc(end) : 0
  const len = text.length
  s = Math.max(0, Math.min(s, len))
  e = Math.max(0, Math.min(e, len))
  if (s > e) {
    const t = s
    s = e
    e = t
  }
  return `${text.slice(0, s)}«${text.slice(s, e)}»${text.slice(e)}`
}

/**
 * Body for `waitForTextInSurface` / managed session assertion failures (before raw appendix).
 * Snapshots are shown with row numbers (one logical line per `\n`; a single-line transcript is line 1).
 */
export function formatSearchSurfaceFailure(
  surface: TtyLocatorSearchSurface,
  detail: string,
  snapshot: string
): string {
  const snap = truncateLocatorFailureSnapshot(
    formatSnapshotWithRowNumbers(snapshot)
  )
  const note =
    surface === 'strippedTranscript'
      ? 'Search uses the ANSI-stripped cumulative transcript (same text as the snapshot below).'
      : 'Snapshot: newline-separated trimmed rows. Matching uses a flat row-major block with no newlines between rows.'
  return `${detail}\nSearch surface: "${surface}".\n${note}\n---\n${snap}\n---`
}
