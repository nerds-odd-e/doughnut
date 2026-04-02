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
import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'
import { stripAnsiCliPty } from './stripAnsi'

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
}

const DEFAULT_RETRY_MS = 50

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

function indicesOfNonOverlapping(haystack: string, needle: string): number[] {
  const indices: number[] = []
  let from = 0
  for (;;) {
    const index = haystack.indexOf(needle, from)
    if (index < 0) break
    indices.push(index)
    from = index + needle.length
  }
  return indices
}

function regexForMatchAll(needle: RegExp): RegExp {
  if (needle.global) return needle
  const flags = needle.flags.includes('g') ? needle.flags : `${needle.flags}g`
  return new RegExp(needle.source, flags)
}

function matchCount(haystack: string, needle: string | RegExp): number {
  if (typeof needle === 'string') {
    return indicesOfNonOverlapping(haystack, needle).length
  }
  return Array.from(haystack.matchAll(regexForMatchAll(needle))).length
}

async function buildHaystackAndSnapshot(
  surface: TtySearchSurface,
  raw: string,
  cols: number,
  rows: number
): Promise<{ haystack: string; snapshot: string }> {
  if (surface === 'strippedTranscript') {
    const haystack = stripAnsiCliPty(raw)
    return { haystack, snapshot: haystack }
  }
  return withReplayedXtermTerminal(raw, cols, rows, (term) => {
    const buffer = term.buffer.active
    const startY = surface === 'fullBuffer' ? 0 : buffer.baseY
    const endY = buffer.length
    const rowData = xtermBufferRows(term, startY, endY)
    return {
      haystack: rowMajorSearchBlock(rowData),
      snapshot: rowMajorSnapshot(rowData),
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
    opts.rows
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

/**
 * Resolves when `needle` appears in the chosen `surface` of `raw` (replay or strip).
 * Polls until `timeoutMs` when the needle is absent (e.g. `raw` grows between attempts via a getter).
 * **Strict** violations throw on the first attempt that sees multiple matches.
 */
export async function waitForTextInSurface(
  opts: WaitForTextInSurfaceOptions
): Promise<void> {
  const timeoutMs = opts.timeoutMs ?? 0
  const retryMs = opts.retryMs ?? DEFAULT_RETRY_MS
  const strict = opts.strict ?? true
  const cols = opts.cols ?? CLI_INTERACTIVE_PTY_COLS
  const rows = opts.rows ?? CLI_INTERACTIVE_PTY_ROWS

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
      throw new TtyAssertStrictModeViolationError(
        formatFailureMessage(opts.surface, result.message, result.snapshot)
      )
    }

    lastFail = { snapshot: result.snapshot, detail: result.detail }
    if (Date.now() - started >= timeoutMs) {
      throw new Error(
        formatFailureMessage(
          opts.surface,
          `Timeout after ${timeoutMs}ms. ${lastFail.detail}`,
          lastFail.snapshot
        )
      )
    }
    await sleep(retryMs)
  }
}
