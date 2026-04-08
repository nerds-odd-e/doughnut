/**
 * xterm Terminal–scoped haystack building and assertion attempts.
 * Used by `waitForTextInSurface` (disposable full replay) and `ManagedTtySession` (incremental replay).
 */

import { Terminal } from '@xterm/headless'
import type { CellExpectationBlock } from './cellExpectations'
import { stripAnsiCliPty } from './stripAnsi'

/** ANSI bright black: `\x1b[90m` (fg) / `\x1b[100m` (bg); xterm palette index 8. */
const BRIGHT_BLACK_PALETTE_INDEX = 8

export type SurfaceAttemptResult =
  | { ok: true }
  | { ok: false; snapshot: string; detail: string }
  | { ok: 'strict'; message: string; snapshot: string }

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

function xtermSearchContext(
  term: Terminal,
  surface: 'viewableBuffer' | 'fullBuffer',
  startAfterAnchor: RegExp[] | undefined,
  fallbackRowCount: number | undefined
): {
  haystack: string
  snapshot: string
  startY: number
  rowOffset: number
} {
  const buffer = term.buffer.active
  const startY = surface === 'fullBuffer' ? 0 : buffer.baseY
  const endY = buffer.length
  const allRows = xtermBufferRows(term, startY, endY)
  const { rows: searchRows, rowOffset } = sliceByAnchor(
    allRows,
    startAfterAnchor,
    fallbackRowCount
  )
  return {
    haystack: rowMajorSearchBlock(searchRows),
    snapshot: rowMajorSnapshot(searchRows),
    startY,
    rowOffset,
  }
}

export function buildHaystackAndSnapshotFromTerminal(
  term: Terminal,
  surface: 'viewableBuffer' | 'fullBuffer',
  startAfterAnchor: RegExp[] | undefined,
  fallbackRowCount: number | undefined
): { haystack: string; snapshot: string } {
  const ctx = xtermSearchContext(
    term,
    surface,
    startAfterAnchor,
    fallbackRowCount
  )
  return { haystack: ctx.haystack, snapshot: ctx.snapshot }
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

function cellHasPaletteBackground(
  cell: { isBgPalette(): boolean; getBgColor(): number },
  index: number
): boolean {
  return cell.isBgPalette() && cell.getBgColor() === index
}

function blockNotFoundLabel(block: CellExpectationBlock): string {
  const kinds = new Set(block.expectations.map((e) => e.kind))
  if (kinds.has('allBold') && kinds.size === 1) return 'bold check'
  if (kinds.has('allBgPalette')) {
    return 'gray block check'
  }
  return 'cell expectation check'
}

function runCellExpectationBlocks(
  term: Terminal,
  opts: {
    surface: 'viewableBuffer' | 'fullBuffer'
    needle: string
    cols: number
    startAfterAnchor?: RegExp[]
    fallbackRowCount?: number
  },
  blocks: CellExpectationBlock[]
): { ok: true } | { ok: false; snapshot: string; detail: string } {
  const buffer = term.buffer.active
  const { haystack, snapshot, startY, rowOffset } = xtermSearchContext(
    term,
    opts.surface,
    opts.startAfterAnchor,
    opts.fallbackRowCount
  )
  const rowWidth = opts.cols

  for (const block of blocks) {
    const index =
      block.match === 'first'
        ? haystack.indexOf(opts.needle)
        : haystack.lastIndexOf(opts.needle)
    if (index < 0) {
      const label = blockNotFoundLabel(block)
      return {
        ok: false,
        snapshot,
        detail: `Substring ${JSON.stringify(opts.needle)} not found in surface "${opts.surface}" (${label}).`,
      }
    }

    for (const exp of block.expectations) {
      if (exp.kind === 'allBold') {
        const which =
          block.match === 'first' ? 'first occurrence' : 'last occurrence'
        for (let i = 0; i < opts.needle.length; i++) {
          const pos = index + i
          const localY = Math.floor(pos / rowWidth)
          const x = pos % rowWidth
          const termLine = buffer.getLine(startY + rowOffset + localY)
          const cell = termLine?.getCell(x)
          if ((cell?.isBold() ?? 0) === 0) {
            return {
              ok: false,
              snapshot,
              detail: `Matched text ${JSON.stringify(opts.needle)} is present but not all cells at the ${which} are bold.`,
            }
          }
        }
        continue
      }

      if (exp.kind === 'allBgPalette') {
        const grayProduct = exp.index === BRIGHT_BLACK_PALETTE_INDEX
        for (let i = 0; i < opts.needle.length; i++) {
          const pos = index + i
          const localY = Math.floor(pos / rowWidth)
          const x = pos % rowWidth
          const termLine = buffer.getLine(startY + rowOffset + localY)
          const cell = termLine?.getCell(x)
          if (cell == null) {
            return {
              ok: false,
              snapshot,
              detail: `Gray block check: no cell at offset ${i} for last match of ${JSON.stringify(opts.needle)}.`,
            }
          }
          if (!cellHasPaletteBackground(cell, exp.index)) {
            if (grayProduct) {
              return {
                ok: false,
                snapshot,
                detail:
                  `Past user message ${JSON.stringify(opts.needle)} must appear in a gray-background block (palette background 8 / chalk \\x1b[100m). ` +
                  `Last match is missing gray background on at least one cell.`,
              }
            }
            return {
              ok: false,
              snapshot,
              detail: `Matched text ${JSON.stringify(opts.needle)}: expected background palette index ${exp.index} on every cell in the span.`,
            }
          }
        }
      }
    }
  }

  return { ok: true }
}

export type SurfaceAttemptOpts = {
  needle: string | RegExp
  surface: TtySearchSurfaceForAttempt
  raw: string
  strict: boolean
  cols: number
  rows: number
  startAfterAnchor?: RegExp[]
  fallbackRowCount?: number
  /** Resolved style checks; empty means none. Caller must validate surface and needle. */
  cellExpectations?: CellExpectationBlock[]
}

type TtySearchSurfaceForAttempt =
  | 'viewableBuffer'
  | 'fullBuffer'
  | 'strippedTranscript'

/**
 * Single assertion attempt using an existing terminal that already reflects `raw` transcript.
 */
export function attemptOnceOnLiveTerminal(
  term: Terminal,
  opts: SurfaceAttemptOpts & {
    surface: 'viewableBuffer' | 'fullBuffer'
  }
): SurfaceAttemptResult {
  if (typeof opts.needle === 'string' && opts.needle.length === 0) {
    return {
      ok: 'strict',
      snapshot: '',
      message:
        'waitForTextInSurface: empty string needle is not supported (ambiguous matches).',
    }
  }

  const { haystack, snapshot } = buildHaystackAndSnapshotFromTerminal(
    term,
    opts.surface,
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

  const blocks = opts.cellExpectations ?? []
  if (blocks.length > 0) {
    if (typeof opts.needle !== 'string') {
      throw new Error(
        'waitForTextInSurface: cell expectations require a string needle (caller must validate).'
      )
    }
    const cellResult = runCellExpectationBlocks(
      term,
      {
        surface: opts.surface,
        needle: opts.needle,
        cols: opts.cols,
        startAfterAnchor: opts.startAfterAnchor,
        fallbackRowCount: opts.fallbackRowCount,
      },
      blocks
    )
    if (!cellResult.ok) {
      return {
        ok: false,
        snapshot: cellResult.snapshot,
        detail: cellResult.detail,
      }
    }
  }

  return { ok: true }
}

export function attemptOnceStrippedTranscript(
  opts: SurfaceAttemptOpts & { surface: 'strippedTranscript' }
): SurfaceAttemptResult {
  if (typeof opts.needle === 'string' && opts.needle.length === 0) {
    return {
      ok: 'strict',
      snapshot: '',
      message:
        'waitForTextInSurface: empty string needle is not supported (ambiguous matches).',
    }
  }

  const haystack = stripAnsiCliPty(opts.raw)
  const snapshot = haystack

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

export function writeTranscriptToTerminal(
  term: Terminal,
  data: string
): Promise<void> {
  return new Promise((resolve, reject) => {
    try {
      term.write(data, () => resolve())
    } catch (e) {
      reject(e)
    }
  })
}

export function withReplayedXtermTerminal<T>(
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

export async function attemptOnce(
  opts: SurfaceAttemptOpts
): Promise<SurfaceAttemptResult> {
  if (opts.surface === 'strippedTranscript') {
    return attemptOnceStrippedTranscript(
      opts as SurfaceAttemptOpts & { surface: 'strippedTranscript' }
    )
  }
  return withReplayedXtermTerminal(opts.raw, opts.cols, opts.rows, (term) =>
    attemptOnceOnLiveTerminal(
      term,
      opts as SurfaceAttemptOpts & { surface: 'viewableBuffer' | 'fullBuffer' }
    )
  )
}
