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
 *
 * **`requireBold`:** after a string needle matches, the **first** occurrence must be entirely bold
 * (second xterm replay for the cell attribute scan).
 *
 * **Gray block (Ink / chalk):** `\x1b[100m` (bright black background) and `\x1b[90m` (bright black
 * foreground) map to xterm **palette** color **8**. For past-user message blocks, use
 * **`rejectGrayForegroundOnlyWithoutGrayBackground`** and **`requireGrayBackgroundBlock`** on a
 * string needle: both inspect the **last** `indexOf` match in the search haystack (same as the
 * former “last paint in raw buffer” heuristic).
 */

import { formatRawTerminalSnapshotForError } from './errorSnapshotFormatting'
import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'
import { attemptOnce } from './surfaceAttemptOnTerminal'

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
  /**
   * After the needle matches, require every cell of the **first** `indexOf` occurrence in the
   * search region to be bold (xterm `isBold()`). Only for `viewableBuffer` / `fullBuffer` with a
   * **string** needle.
   */
  requireBold?: boolean
  /**
   * After the needle matches, fail if **any** cell of the **last** occurrence has bright-black /
   * gray **foreground** (palette 8) without gray **background** (palette 8) — i.e. chalk `grey`
   * / `90m` without a `100m` block. Only for `viewableBuffer` / `fullBuffer` with a **string**
   * needle.
   */
  rejectGrayForegroundOnlyWithoutGrayBackground?: boolean
  /**
   * After the needle matches, require **every** cell of the **last** occurrence to use gray
   * **background** (palette 8, chalk `bgGray` / `100m`). Only for `viewableBuffer` / `fullBuffer`
   * with a **string** needle.
   */
  requireGrayBackgroundBlock?: boolean
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
  if (opts.requireBold) {
    if (opts.surface === 'strippedTranscript') {
      throw new Error(
        'waitForTextInSurface: requireBold is only supported for viewableBuffer and fullBuffer.'
      )
    }
    if (typeof opts.needle !== 'string') {
      throw new Error(
        'waitForTextInSurface: requireBold requires a string needle.'
      )
    }
  }

  if (
    opts.rejectGrayForegroundOnlyWithoutGrayBackground ||
    opts.requireGrayBackgroundBlock
  ) {
    if (opts.surface === 'strippedTranscript') {
      throw new Error(
        'waitForTextInSurface: gray block options are only supported for viewableBuffer and fullBuffer.'
      )
    }
    if (typeof opts.needle !== 'string') {
      throw new Error(
        'waitForTextInSurface: gray block options require a string needle.'
      )
    }
  }

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
      needle: opts.needle,
      surface: opts.surface,
      raw,
      strict,
      cols,
      rows,
      startAfterAnchor: opts.startAfterAnchor,
      fallbackRowCount: opts.fallbackRowCount,
      requireBold: opts.requireBold,
      rejectGrayForegroundOnlyWithoutGrayBackground:
        opts.rejectGrayForegroundOnlyWithoutGrayBackground,
      requireGrayBackgroundBlock: opts.requireGrayBackgroundBlock,
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
