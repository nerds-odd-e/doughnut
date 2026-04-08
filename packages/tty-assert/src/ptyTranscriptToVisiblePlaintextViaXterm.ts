import { Terminal } from '@xterm/headless'
import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'

/** xterm headless replay → viewport plaintext. Canonical export: `ptyTranscriptToViewportPlaintext`. */

export function viewportPlaintextFromHeadlessTerminal(term: Terminal): string {
  const buffer = term.buffer.active
  const lines: string[] = []
  for (let i = 0; i < term.rows; i++) {
    const line = buffer.getLine(buffer.viewportY + i)
    lines.push(line?.translateToString(true) ?? '')
  }
  return lines.join('\n').replace(/\n+$/, '')
}

/**
 * Replays a PTY transcript through xterm.js (headless) and returns visible plaintext
 * for the **viewport**: one row per screen line, trailing spaces stripped per row,
 * rows joined with `\n`, no trailing blank lines — for downstream heuristics (e.g. Current guidance).
 *
 * `write` is asynchronous; the promise resolves after the parser has applied the transcript.
 *
 * **Canonical import:** `ptyTranscriptToViewportPlaintext` from `tty-assert/ptyTranscriptToViewportPlaintext`.
 */
export function ptyTranscriptToVisiblePlaintextViaXterm(
  raw: string,
  cols: number = CLI_INTERACTIVE_PTY_COLS,
  rows: number = CLI_INTERACTIVE_PTY_ROWS
): Promise<string> {
  const term = new Terminal({
    cols,
    rows,
    allowProposedApi: true,
  })
  return new Promise((resolve, reject) => {
    try {
      term.write(raw, () => {
        try {
          resolve(viewportPlaintextFromHeadlessTerminal(term))
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

/** @see {@link ptyTranscriptToVisiblePlaintextViaXterm} — canonical name for new imports. */
export const ptyTranscriptToViewportPlaintext =
  ptyTranscriptToVisiblePlaintextViaXterm
