/**
 * Locator-style text search over explicit **surfaces** (tui-test–inspired, runner-agnostic).
 *
 * **Search haystack (substring / RegExp):** xterm-backed surfaces use a **row-major** block:
 * each row is `cols` cells as strings joined (empty cells → space), then **all rows concatenated
 * with no `\n` between rows** — same shape as
 * [microsoft/tui-test `Locator`](https://github.com/microsoft/tui-test/blob/main/src/terminal/locator.ts).
 *
 * **Failure snapshots:** newline-separated rows, each row **trimEnd**’d (like tui-test’s timeout
 * snapshot), so debug output is readable and differs from the flat search block.
 *
 * **`ptyTranscriptToViewportPlaintext`:** still uses **`\n`‑joined** viewport lines for guidance
 * heuristics; locators here intentionally use the flat block for matching.
 */

import { Terminal } from '@xterm/headless'
import { formatRawTerminalSnapshotForError } from './errorSnapshotFormatting'
import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'
import { stripAnsiCliPty } from './stripAnsi'

export { stripAnsiCliPty }

export type TtySearchSurface =
  | 'viewableBuffer'
  | 'fullBuffer'
  | 'strippedTranscript'

export type WaitForTextInSurfaceOptions = {
  /** Cumulative PTY bytes, or a supplier invoked each poll attempt. */
  raw: string | (() => string)
  needle: string | RegExp
  surface: TtySearchSurface
  /**
   * Wall-clock budget from the first attempt. `0` means a single attempt (typical in Vitest).
   */
  timeoutMs?: number
  retryMs?: number
  /**
   * When more than one non-overlapping match exists, throw (tui-test default).
   * Set `false` when duplicate Ink lines are expected.
   */
  strict?: boolean
  cols?: number
  rows?: number
  /**
   * Restrict xterm-backed search to rows **below** the last row matching any
   * anchor (tried in priority order, scanning bottom-to-top per anchor).
   * Ignored for `strippedTranscript`.
   */
  startAfterAnchor?: RegExp[]
  /** When no anchor matches, search the bottom N rows (default: all rows). */
  fallbackRowCount?: number
  /** Prepended to the failure body (one line is typical), before surface/snapshot sections. */
  messagePrefix?: string
}

/** Default delay between poll attempts when `timeoutMs` is positive; Cypress adapter should use the same value for `cy.wait` between buffer re-reads. */
export const TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS = 50

export class TtyAssertStrictModeViolationError extends Error {
  readonly name = 'TtyAssertStrictModeViolationError'
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function resolveRaw(raw: string | (() => string)): string {
  return typeof raw === 'function' ? raw() : raw
}

function xtermBufferRows(
  term: Terminal,
  startY: number,
  endY: number
): string[][] {
  const buffer = term.buffer.active
  const cols = term.cols
  const lines: string[][] = []
  for (let y = startY; y < endY; y++) {
    const termLine = buffer.getLine(y)
    const line: string[] = []
    let cell = termLine?.getCell(0)
    for (let x = 0; x < cols; x++) {
      cell = termLine?.getCell(x, cell)
      const rawChars = cell?.getChars() ?? ''
      line.push(rawChars === '' ? ' ' : rawChars)
    }
    lines.push(line)
  }
  return lines
}

function rowMajorSearchBlock(rows: string[][]): string {
  return rows.map((r) => r.join('')).join('')
}

function rowMajorSnapshot(rows: string[][]): string {
  return rows.map((r) => r.join('').trimEnd()).join('\n')
}

function findAnchorRowIndex(rows: string[][], anchors: RegExp[]): number {
  for (const anchor of anchors) {
    for (let i = rows.length - 1; i >= 0; i--) {
      const row = rows[i]
      if (row && anchor.test(row.join('').trimEnd())) return i
    }
  }
  return -1
}

function sliceByAnchor(
  rows: string[][],
  anchors: RegExp[] | undefined,
  fallbackRowCount: number | undefined
): { rows: string[][]; rowOffset: number } {
  if (!anchors || anchors.length === 0) return { rows, rowOffset: 0 }
  const idx = findAnchorRowIndex(rows, anchors)
  if (idx >= 0) return { rows: rows.slice(idx + 1), rowOffset: idx + 1 }
  if (fallbackRowCount != null) {
    const start = Math.max(0, rows.length - fallbackRowCount)
    return { rows: rows.slice(start), rowOffset: start }
  }
  return { rows, rowOffset: 0 }
}

function withReplayedXtermTerminal<T>(
  raw: string,
  cols: number,
  rows: number,
  fn: (term: Terminal) => T
): Promise<T> {
  const term = new Terminal({
    cols,
    rows,
    allowProposedApi: true,
  })
  return new Promise((resolve, reject) => {
    try {
      term.write(raw, () => {
        try {
          resolve(fn(term))
        } finally {
          term.dispose()
        }
      })
    } catch (e) {
      term.dispose()
      reject(e)
    }
  })
}

function countNonOverlapping(haystack: string, needle: string): number {
  let count = 0
  let from = 0
  for (;;) {
    const index = haystack.indexOf(needle, from)
    if (index < 0) break
    count++
    from = index + needle.length
  }
  return count
}

function regexForMatchAll(needle: RegExp): RegExp {
  if (needle.global) return needle
  const flags = needle.flags.includes('g') ? needle.flags : `${needle.flags}g`
  return new RegExp(needle.source, flags)
}

function matchCount(haystack: string, needle: string | RegExp): number {
  if (typeof needle === 'string') {
    return countNonOverlapping(haystack, needle)
  }
  return Array.from(haystack.matchAll(regexForMatchAll(needle))).length
}

async function buildHaystackAndSnapshot(
  surface: TtySearchSurface,
  raw: string,
  cols: number,
  rows: number,
  startAfterAnchor?: RegExp[],
  fallbackRowCount?: number
): Promise<{ haystack: string; snapshot: string }> {
  if (surface === 'strippedTranscript') {
    const haystack = stripAnsiCliPty(raw)
    return { haystack, snapshot: haystack }
  }
  return withReplayedXtermTerminal(raw, cols, rows, (term) => {
    const buffer = term.buffer.active
    const startY = surface === 'fullBuffer' ? 0 : buffer.baseY
    const endY = buffer.length
    const allRows = xtermBufferRows(term, startY, endY)
    const { rows: searchRows } = sliceByAnchor(
      allRows,
      startAfterAnchor,
      fallbackRowCount
    )
    return {
      haystack: rowMajorSearchBlock(searchRows),
      snapshot: rowMajorSnapshot(searchRows),
    }
  })
}

type AttemptResult =
  | { ok: true }
  | { ok: false; snapshot: string; detail: string }
  | { ok: 'strict'; message: string; snapshot: string }

async function attemptOnce(
  opts: Omit<WaitForTextInSurfaceOptions, 'timeoutMs' | 'retryMs'> & {
    raw: string
    strict: boolean
    cols: number
    rows: number
  }
): Promise<AttemptResult> {
  if (typeof opts.needle === 'string' && opts.needle.length === 0) {
    return {
      ok: 'strict',
      snapshot: '',
      message:
        'waitForTextInSurface: empty string needle is not supported (ambiguous matches).',
    }
  }

  const { haystack, snapshot } = await buildHaystackAndSnapshot(
    opts.surface,
    opts.raw,
    opts.cols,
    opts.rows,
    opts.startAfterAnchor,
    opts.fallbackRowCount
  )

  const count = matchCount(haystack, opts.needle)
  if (count > 1 && opts.strict) {
    const label =
      typeof opts.needle === 'string'
        ? JSON.stringify(opts.needle)
        : `${opts.needle}`
    return {
      ok: 'strict',
      snapshot,
      message: `Strict mode violation: ${label} matched ${count} non-overlapping occurrences in surface "${opts.surface}".`,
    }
  }
  if (count === 0) {
    const label =
      typeof opts.needle === 'string'
        ? JSON.stringify(opts.needle)
        : `${opts.needle}`
    return {
      ok: false,
      snapshot,
      detail: `Substring/pattern ${label} not found in surface "${opts.surface}".`,
    }
  }
  return { ok: true }
}

function formatFailureMessage(
  surface: TtySearchSurface,
  detail: string,
  snapshot: string
): string {
  const snap =
    snapshot.length > 8000
      ? `${snapshot.slice(0, 8000)}\n… (truncated)`
      : snapshot
  const note =
    surface === 'strippedTranscript'
      ? 'Search uses the ANSI-stripped cumulative transcript (same text as the snapshot below).'
      : 'Snapshot: newline-separated trimmed rows. Matching uses a flat row-major block with no newlines between rows.'
  return `${detail}\nSearch surface: "${surface}".\n${note}\n---\n${snap}\n---`
}

const CLI_TERMINAL_RAW_SNAPSHOT_HEADING =
  '--- CLI terminal snapshot (ANSI-stripped, safe text) ---'

function withOptionalMessagePrefix(
  prefix: string | undefined,
  body: string
): string {
  if (prefix == null || prefix === '') return body
  const p = prefix.replace(/\n+$/, '')
  return `${p}\n${body}`
}

function appendRawTerminalSnapshotForErrorMessage(
  body: string,
  raw: string
): string {
  return `${body}\n\n${CLI_TERMINAL_RAW_SNAPSHOT_HEADING}\n${formatRawTerminalSnapshotForError(raw)}`
}

/**
 * Resolves when `needle` appears in the chosen `surface` of `raw` (replay or strip).
 * Polls until `timeoutMs` when the needle is absent (e.g. `raw` grows between attempts via a getter).
 * **Strict** violations throw on the first attempt that sees multiple matches.
 */
export async function waitForTextInSurface(
  opts: WaitForTextInSurfaceOptions
): Promise<void> {
  const timeoutMs = opts.timeoutMs ?? 0
  const retryMs = opts.retryMs ?? TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
  const strict = opts.strict ?? true
  const cols = opts.cols ?? CLI_INTERACTIVE_PTY_COLS
  const rows = opts.rows ?? CLI_INTERACTIVE_PTY_ROWS
  const messagePrefix = opts.messagePrefix

  const started = Date.now()
  let lastFail: { snapshot: string; detail: string } | undefined

  for (;;) {
    const raw = resolveRaw(opts.raw)
    const result = await attemptOnce({
      ...opts,
      raw,
      strict,
      cols,
      rows,
    })

    if (result.ok === true) return

    if (result.ok === 'strict') {
      const body = withOptionalMessagePrefix(
        messagePrefix,
        formatFailureMessage(opts.surface, result.message, result.snapshot)
      )
      throw new TtyAssertStrictModeViolationError(
        appendRawTerminalSnapshotForErrorMessage(body, raw)
      )
    }

    lastFail = { snapshot: result.snapshot, detail: result.detail }
    if (Date.now() - started >= timeoutMs) {
      const detail =
        timeoutMs === 0
          ? lastFail.detail
          : `Timeout after ${timeoutMs}ms. ${lastFail.detail}`
      const body = withOptionalMessagePrefix(
        messagePrefix,
        formatFailureMessage(opts.surface, detail, lastFail.snapshot)
      )
      throw new Error(appendRawTerminalSnapshotForErrorMessage(body, raw))
    }
    await sleep(retryMs)
  }
}

/**
 * Locator-style cell resolution: replays `raw` through xterm, finds `needle`
 * in the viewport (optionally below an anchor row), and returns the matched
 * xterm cells with position and styling attributes.
 *
 * Inspired by [tui-test `Locator.resolve`](https://github.com/microsoft/tui-test).
 */
export type ResolvedCell = {
  char: string
  x: number
  y: number
  bold: boolean
}

export type LocateTextResult = {
  found: boolean
  cells: ResolvedCell[]
  snapshot: string
}

export async function locateTextCellsInViewport(opts: {
  raw: string
  needle: string
  startAfterAnchor?: RegExp[]
  fallbackRowCount?: number
  cols?: number
  rows?: number
}): Promise<LocateTextResult> {
  const cols = opts.cols ?? CLI_INTERACTIVE_PTY_COLS
  const rows = opts.rows ?? CLI_INTERACTIVE_PTY_ROWS

  return withReplayedXtermTerminal(opts.raw, cols, rows, (term) => {
    const buffer = term.buffer.active
    const baseY = buffer.baseY
    const allRows = xtermBufferRows(term, baseY, buffer.length)
    const { rows: searchRows, rowOffset } = sliceByAnchor(
      allRows,
      opts.startAfterAnchor,
      opts.fallbackRowCount
    )

    const block = rowMajorSearchBlock(searchRows)
    const snapshot = rowMajorSnapshot(searchRows)
    const index = block.indexOf(opts.needle)
    if (index < 0) {
      return { found: false, cells: [], snapshot }
    }

    const cells: ResolvedCell[] = []
    const rowWidth = cols
    for (let i = 0; i < opts.needle.length; i++) {
      const pos = index + i
      const localY = Math.floor(pos / rowWidth)
      const x = pos % rowWidth
      const termLine = buffer.getLine(baseY + rowOffset + localY)
      const cell = termLine?.getCell(x)
      cells.push({
        char: cell?.getChars() ?? '',
        x,
        y: rowOffset + localY,
        bold: (cell?.isBold() ?? 0) !== 0,
      })
    }

    return { found: true, cells, snapshot }
  })
}
